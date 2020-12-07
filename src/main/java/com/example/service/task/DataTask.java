package com.example.service.task;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.GuPiao;
import com.example.mapper.HistoryDayStockMapper;
import com.example.model.GuPiaoDo;
import com.example.model.HistoryDayStockDo;
import com.example.model.HistoryStockDo;
import com.example.model.RealTimeDo;
import com.example.model.RobotAccountDo;
import com.example.model.RobotSetDo;
import com.example.model.StockDo;
import com.example.model.StockPriceVo;
import com.example.model.SubscriptionDo;
import com.example.model.TradingRecordDo;
import com.example.service.GuPiaoService;
import com.example.service.TrendStrategyService;
import com.example.uitls.DingTalkRobotHTTPUtil;
import com.example.uitls.ReadApiUrl;
import com.example.uitls.RedisKeyUtil;
import com.example.uitls.RedisUtil;

@Service
public class DataTask  implements InitializingBean {
	ThreadPoolExecutor  pool = new ThreadPoolExecutor(20, 100, 1,TimeUnit.SECONDS,
														new LinkedBlockingDeque<Runnable>(1000), 
														Executors.defaultThreadFactory(), 
														new ThreadPoolExecutor.CallerRunsPolicy());
	
	private static Logger logger = LoggerFactory.getLogger("task_log");
	
	@Autowired
	private GuPiaoService guPiaoService;
	@Resource
	private RedisUtil redisUtil;
	@Autowired
	private ReadApiUrl apiUrl;
	@Autowired
	private HistoryDayStockMapper historyDayStockMapper;
	@Autowired
	private TrendStrategyService trendStrategyService;
	@Override
	public void afterPropertiesSet() throws Exception {
		String robotbuy = MessageFormat.format("【初始化股票池】 \n 初始化股票池数量："  + init(),new Object[] {});
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
	}
	
	/**
	 * 缓存股票名称
	 * @return
	 */
	private int init() {
		List<StockDo> stockList = guPiaoService.getAllStock();
		stockList.forEach(stock->{
			redisUtil.set(RedisKeyUtil.getStockName(stock.getNumber()), stock.getName());
        });
		return stockList.size();
	}
	
	
	/**
	 * 搜索每天新发行的股票
	 */
	@Scheduled(cron = "0 10 15 * * MON-FRI")
	private void updateAllGuPiao() {
		logger.info("开始更新股票池总数");
		pool.execute(new Runnable() {
			@Override
			public void run() {
				logger.info("==>开始更新股票");
				for(int i=0;i<=99999;i++) {
					String number = String.format("%05d", i);
					if(redisUtil.hasKey(RedisKeyUtil.getStockName("sz0"+number))
							|| redisUtil.hasKey(RedisKeyUtil.getStockName("sz3"+number))
							|| redisUtil.hasKey(RedisKeyUtil.getStockName("sh6"+number))) {
						continue;
					}
					GuPiao date=apiUrl.readUrl(i, "sz0",false);
					if(date !=null) {
						GuPiaoDo model=new GuPiaoDo();
						BeanUtils.copyProperties(date, model);
						guPiaoService.updateStock(model.getNumber(),model.getName(), 2) ;
					}
					date=apiUrl.readUrl(i, "sz3",false);
					if(date !=null) {
						GuPiaoDo model=new GuPiaoDo();
						BeanUtils.copyProperties(date, model);
						guPiaoService.updateStock(model.getNumber(),model.getName(), 3) ;
					}
					date=apiUrl.readUrl(i, "sh6",false);
					if(date !=null) {
						GuPiaoDo model=new GuPiaoDo();
						BeanUtils.copyProperties(date, model);
						guPiaoService.updateStock(model.getNumber(),model.getName(), 1) ;
					}
				}
				logger.info("==>更新完毕");
				init();
			}
		});
		
	}
	
	/**
	 * 更新日线数据
	 */
	@Scheduled(cron = "0 10 15 * * MON-FRI")
	public void updateAllDayGuPiao() {
		List<StockDo> stockList = guPiaoService.getAllStock();
		final SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
		String dateStr=dateformat.format(new Date());
		for(StockDo stock:stockList) {
			RealTimeDo model=new RealTimeDo();
			model.setNumber(stock.getNumber());
			model.setDate(dateStr);
			String key =RedisKeyUtil.getRealTimeListByRealTimeDo(model);
			Map<String,RealTimeDo> map=(Map<String,RealTimeDo>) redisUtil.get(key);
			if(map == null) {
				continue;
			}
			
			List<RealTimeDo>list=new ArrayList<RealTimeDo>();
			Collection<RealTimeDo> valueCollection = map.values();
			List<RealTimeDo>templist=new ArrayList<RealTimeDo>(valueCollection);
			for(RealTimeDo rt:templist) {
				if(rt.getTop() == null||rt.getLow()== null||rt.getKaipanjia()== null||rt.getZuorishoupanjia()== null||rt.getChengjiaogupiao()== null) {
					logger.warn("空值数据："+rt.getName()+" "+rt.getNumber()+" 最高价:"+rt.getTop()+" 最低价："+rt.getLow()+" 开盘价："+rt.getKaipanjia()+" 收盘价："+rt.getZuorishoupanjia()+" 成交量："+rt.getChengjiaogupiao());
					continue;
				}
				if(rt.getTop()<=0.1||rt.getLow()<=0.1||rt.getKaipanjia()<=0.1||rt.getZuorishoupanjia()<=0.1||rt.getChengjiaogupiao()<=0.1) {
					logger.warn("异常数据："+rt.getName()+" "+rt.getNumber()+" 最高价:"+rt.getTop()+" 最低价："+rt.getLow()+" 开盘价："+rt.getKaipanjia()+" 收盘价："+rt.getZuorishoupanjia()+" 成交量："+rt.getChengjiaogupiao());
					continue;
				}
				list.add(rt);
			}
			
			if(list != null && list.size()>0) {
				double avg=0;
				double high=0;
				double low=100000;
				for(RealTimeDo rt:list) {
					avg+=rt.getDangqianjiage();
					if(rt.getTop()>=high) {
						high=rt.getTop();
					}
					if(rt.getLow()<=low) {
						low=rt.getLow();
					}
				}
				avg=avg/list.size();
				RealTimeDo last=list.get(list.size()-1);
				HistoryDayStockDo obj =new HistoryDayStockDo();
				obj.setHistoryDay(dateStr);
				obj.setNumber(stock.getNumber());
				obj.setOpen(new BigDecimal(last.getKaipanjia()));
				obj.setClose(new BigDecimal(last.getDangqianjiage()));
				obj.setAvg(new BigDecimal(avg));
				obj.setHigh(new BigDecimal(high));
				obj.setLow(new BigDecimal(low));
				obj.setVolume(last.getChengjiaogupiao().longValue());
				if(historyDayStockMapper.getByTime(obj) == null) {
					System.out.println(obj.getNumber());
					historyDayStockMapper.insert(obj);
				}
			}
        }
		String robotbuy = MessageFormat.format("更新日线成功" ,new Object[] {});
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
	}
	
	/**
	 * 补风险线
	 */
	@Scheduled(cron = "0 25 9,16 * * MON-FRI")
	private void updateRisk() {
		List<StockDo> stockList = guPiaoService.getAllStock();
		for(StockDo stock:stockList) {
			trendStrategyService.reRisk(stock.getNumber());
		}
	}
	
	
	/**
	 * 关注个股，显示操作
	 */
	@Scheduled(cron = "0 30 9-11,13-14 * * MON-FRI")
	private void showBoduan() {
		List<SubscriptionDo> list=guPiaoService.listMemberAll();
		for(SubscriptionDo realTime:list) {
			if(!StringUtils.equals(realTime.getNumber(), "0")) {
				excuteRunListen(realTime.getNumber(),realTime.getDingtalkId(),realTime.getBegintime());
			}
		}
	}
	public void excuteRunListen(final String number,final String appSecret,final String beginTime) {
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Boolean isNotifyByMock=(Boolean)redisUtil.get(RedisKeyUtil.getBoduanNotify(number, appSecret));
					//通知开关
					if(isNotifyByMock == null || isNotifyByMock) {
						isNotifyByMock=true;
					}
					List<HistoryStockDo> list= guPiaoService.getLastHistoryStock(number,2);
					String msg="GS========超短线策略波段分析(3-5天)=========GS"
							+ "\n 股票编号："+number
							+ "\n 股票名称："+(String)redisUtil.get(RedisKeyUtil.getStockName(number))
							+ "\n";
					for(HistoryStockDo stock:list) {
						msg=msg+stock.getRemark();
					}
					msg = updateMsg(number, msg);
					logger.info(msg);
					
					if(isNotifyByMock) {
						DingTalkRobotHTTPUtil.sendMsg(appSecret, msg, null, false);
						isNotifyByMock=false;
					}
					redisUtil.set(RedisKeyUtil.getBoduanNotify(number, appSecret),isNotifyByMock,86400L);
				} catch (Exception e) {
					logger.error("异常个股波段分析:"+"number:"+number+"-->"+e.getMessage(),e);
				}
			}

			private String updateMsg(final String number, String msg) {
				try {
					guPiaoService.updateHistoryStock(number);
					guPiaoService.timeInterval(number);
					List<StockPriceVo> spList=trendStrategyService.transformByDayLine(historyDayStockMapper.getNumber(number));
					RobotAccountDo account=new RobotAccountDo();
					RobotSetDo config=new RobotSetDo();
					account.setTotal(new BigDecimal(100000));
					List<TradingRecordDo> rtList=trendStrategyService.getStrateByBoll(spList, account, config);
					if(rtList!=null && rtList.size() >1) {
						msg=msg+rtList.get(rtList.size()-1).getRemark();
					}
				} catch (Exception e) {
					logger.error("updateMsg:"+"number:"+number+"-->"+e.getMessage(),e);
				}
				return msg;
			}
		});
	}
	
}

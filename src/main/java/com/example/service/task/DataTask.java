package com.example.service.task;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

import com.example.ai.MockDeal;
import com.example.demo.GuPiao;
import com.example.mapper.HistoryDayStockMapper;
import com.example.model.GuPiaoDo;
import com.example.model.HistoryDayStockDo;
import com.example.model.HistoryStockDo;
import com.example.model.MockLog;
import com.example.model.RealTimeDo;
import com.example.model.RiskStockDo;
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
	private static Logger ai_logger = LoggerFactory.getLogger("ai_log");
	private static Logger logger = LoggerFactory.getLogger("task_log");
	private static final SimpleDateFormat DF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
	private static final SimpleDateFormat DF_YYYY_MM_DD_number = new SimpleDateFormat("yyyyMMdd");// 设置日期格式
	
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
	@Autowired
	private MockDeal mockDeal;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		String robotbuy = MessageFormat.format("【初始化股票池】 \n 初始化股票池数量："  + init(),new Object[] {});
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
        EmaGupiao();
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
				
				HistoryDayStockDo obj =new HistoryDayStockDo();
				RealTimeDo last=(RealTimeDo)map.get(dateStr+"_150000");
				if(last==null) {
					last=new RealTimeDo();
					String cacheKey = RedisKeyUtil.getLastRealTime(stock.getNumber());
					GuPiao date= (GuPiao)redisUtil.get(cacheKey);
					if(date==null) {
						date=apiUrl.readRealTimeUrl(stock.getNumber());
						last.setChengjiaogupiao(date.getChengjiaogupiao());
						last.setKaipanjia(date.getKaipanjia());
						last.setDangqianjiage(date.getDangqianjiage());
					}
				}
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
        updateRisk();
        EmaGupiao();
	}
	
	private void updateRisk() {
		List<StockDo> stockList = guPiaoService.getAllStock();
		String robotbuy = MessageFormat.format("GS----开始更新风险线-------一共："+stockList.size(),new Object[] {});
		long bs=System.currentTimeMillis();
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
		for(StockDo stock:stockList) {
			trendStrategyService.reRisk(stock.getNumber());
		}
		bs=System.currentTimeMillis()-bs;
		robotbuy = MessageFormat.format("GS----更新风险线完毕-------耗时："+bs+" ms",new Object[] {});
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
	}
	
	public void EmaGupiao() {
		String robotbuy = MessageFormat.format("开始策略选股" ,new Object[] {});
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
		Calendar now = Calendar.getInstance();  
		now.add(Calendar.DATE, -1);
		String today=DF_YYYY_MM_DD_number.format(now.getTime());
		List<SubscriptionDo> subscriptionList=new ArrayList<SubscriptionDo>();
		for(SubscriptionDo realTime:guPiaoService.listMemberAll()) {
			if(StringUtils.equals(realTime.getNumber(), "0")){
				subscriptionList.add(realTime);
			}
		}
		String logContext="GS===========EMA策略选股=============";
		for(StockDo stock : guPiaoService.getAllStock()){
			HistoryDayStockDo obj=new HistoryDayStockDo();
			obj.setNumber(stock.getNumber()); 
			obj.setHistoryDay(today);
			obj=historyDayStockMapper.getByTime(obj);
			if(obj == null||obj.getClose()==null) {
				continue;
			}
			if(obj.getClose().intValue() >35 || obj.getClose().intValue() <12 ) {
				continue;
			}
			
			Calendar before = Calendar.getInstance();  
			before.add(Calendar.DATE, -3);
			List<StockPriceVo> spList=trendStrategyService.transformByDayLine(historyDayStockMapper.getNumber(stock.getNumber()));
			RobotAccountDo account=new RobotAccountDo();
			RobotSetDo config=new RobotSetDo();
			account.setTotal(new BigDecimal(100000));
			List<TradingRecordDo> rtList=trendStrategyService.getStrategyByEMA(spList, account, config);
			for(TradingRecordDo rt:rtList) {
				System.out.println(logContext);
				if(rt.getCreateDate().after(before.getTime())) {
					logContext=logContext
							+"\n时间:"+DF_YYYY_MM_DD.format(rt.getCreateDate())
							+"\n股票编号:"+rt.getNumber()
							+"\n股票名称:"+rt.getName()
							+"\n当天均价："+rt.getPrice()
							+"\n"+rt.getRemark()+"\n";
				}
			}
		}
		ai_logger.info(logContext);
		for(SubscriptionDo realTime:subscriptionList) {
			String key=RedisKeyUtil.getEmaStockSellNotify(realTime.getNumber(), realTime.getDingtalkId());
			Boolean isNotifyByMock=(Boolean)redisUtil.get(key);
			//通知开关
			if(isNotifyByMock == null || isNotifyByMock) {
				isNotifyByMock=true;
			}
			if(isNotifyByMock) {
				isNotifyByMock=false;
				DingTalkRobotHTTPUtil.sendMsg(realTime.getDingtalkId(), logContext, null, false);
			}
			redisUtil.set(key,isNotifyByMock,86400L);
		}
	}


	/**
	 * 每天推荐
	 */
	@Scheduled(cron = "0 30 9 * * MON-FRI")
	private void todayList() {
		List<RiskStockDo> list=trendStrategyService.getTodayList();
		if(list!=null) {
			for(RiskStockDo rs:list) {
				redisUtil.del(RedisKeyUtil.getBoduanNotify(rs.getNumber(),DingTalkRobotHTTPUtil.APP_TEST_SECRET));
				excuteRunListen(rs.getNumber(),DingTalkRobotHTTPUtil.APP_TEST_SECRET);
			}
		}
	}
	
	

	/**
	 * 关注个股，显示操作
	 */
	@Scheduled(cron = "0 35 9 * * MON-FRI")
	private void showBoduan() {
		List<SubscriptionDo> list=guPiaoService.listMemberAll();
		for(SubscriptionDo realTime:list) {
			if(!StringUtils.equals(realTime.getNumber(), "0")) {
				excuteRunListen(realTime.getNumber(),realTime.getDingtalkId());
			}
		}
	}
	public void excuteRunListen(final String number,final String appSecret) {
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Boolean isNotifyByMock=(Boolean)redisUtil.get(RedisKeyUtil.getBoduanNotify(number, appSecret));
					//通知开关
					if(isNotifyByMock == null || isNotifyByMock) {
						isNotifyByMock=true;
					}
					
					RiskStockDo rs=trendStrategyService.getRiskStock(number);
					Calendar calendar = Calendar.getInstance();  
					calendar.add(Calendar.DATE, -15);
					SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
					MockLog log=mockDeal.mockDeal(number, dateformat.format(calendar.getTime()),DingTalkRobotHTTPUtil.APP_TEST_SECRET,false);
					log.setLogs(log.getLogs().replace("测试AI操盘", "个股关注(操盘策略)"));
					String msg=log.getLogs();
					if(rs==null) {
						msg=msg+"\n 找不到分析数据，请联系管理员！股票号码："+number;
					}else {
						String tg="技术线一般";
						if(rs.getMa5()>rs.getMa20() && rs.getMa20()>rs.getMa200()) {
							tg="多头排列，值得持有,建议逢低加仓";
						} 
						if(rs.getMa5()<rs.getMa20() && rs.getMa20()<rs.getMa200()) {
							tg="危险的股票，小心创新低,建议逢高减仓";
						}
						if(rs.getClose()<rs.getBollDayMid()) {
							tg+=",目前趋势很弱";
						}
						if(rs.getClose()>rs.getBollDayMid()) {
							tg+=",目前趋势很强";
						}
						if(rs.getOpen()<rs.getEma89() && rs.getClose()>rs.getEma144() && rs.getEma89()<rs.getEma144()) {
							tg+=",需要注意能量配合";
						}
						if(rs.getClose()<rs.getBuyPointEnd()&&rs.getClose()>rs.getBuyPointBegin()) {
							tg+=",目前是买入区间，可以分批建仓";
						}
					msg=msg
					+"\n 开盘价："+rs.getOpen()
					+"\n 收盘价："+rs.getClose()
					+"\n 压力位："+(rs.getClose()>=rs.getBollDayMid()?rs.getBollDayUp():rs.getBollDayMid())
					+"\n 支撑位置："+(rs.getClose()>=rs.getBollDayMid()?rs.getBollDayMid():rs.getBollDayLower())
					+"\n 买入区间："+rs.getBuyPointBegin()+"~"+rs.getBuyPointEnd()
					+"\n 目标价："+rs.getStopProfit()
					+"\n 止损位："+rs.getStopLoss()
					+"\n 平均能量："+rs.getTop5volume()
					+"\n 技术分析："+tg
					+"\n";
					;
					}
					if(isNotifyByMock) {
						DingTalkRobotHTTPUtil.sendMsg(appSecret, msg, null, false);
						isNotifyByMock=false;
					}
					redisUtil.set(RedisKeyUtil.getBoduanNotify(number, appSecret),isNotifyByMock,3600L);
				} catch (Exception e) {
					logger.error("异常个股波段分析:"+"number:"+number+"-->"+e.getMessage(),e);
				}
			}

		});
	}
	
}

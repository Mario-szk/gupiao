package com.example.service.task;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.ai.MockDeal;
import com.example.mapper.HistoryDayStockMapper;
import com.example.model.HistoryDayStockDo;
import com.example.model.MockLog;
import com.example.model.RobotAccountDo;
import com.example.model.RobotSetDo;
import com.example.model.StockDo;
import com.example.model.StockPriceVo;
import com.example.model.SubscriptionDo;
import com.example.model.TradingRecordDo;
import com.example.service.GuPiaoService;
import com.example.service.TrendStrategyService;
import com.example.uitls.DateUtils;
import com.example.uitls.DingTalkRobotHTTPUtil;
import com.example.uitls.RedisKeyUtil;
import com.example.uitls.RedisUtil;

@Service
public class MonitorTask  {
	private static Logger logger = LoggerFactory.getLogger("task_log");
	private static Logger ai_logger = LoggerFactory.getLogger("task_log");
	ThreadPoolExecutor  pool = new ThreadPoolExecutor(20, 100, 1,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(1000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());
	private static final SimpleDateFormat DF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
	private static final SimpleDateFormat DF_YYYY_MM_DD_number = new SimpleDateFormat("yyyyMMdd");// 设置日期格式
	@Autowired
	private GuPiaoService guPiaoService;
	@Autowired
	private MockDeal mockDeal;
	@Resource
	private RedisUtil redisUtil;
	@Autowired
	private TrendStrategyService trendStrategyService;
	@Autowired
	private HistoryDayStockMapper historyDayStockMapper;
	
	
	
	@Scheduled(cron = "0 30 9 * * MON-FRI")
	private void followTask1() {
		followTask();
	}
	
	private void followTask() {
		if(!DateUtils.traceTime(guPiaoService.getHolidayList())) {
			return ;
		}
		
		List<String>list=new ArrayList<String>();
		List<SubscriptionDo> subscriptionList=guPiaoService.listMemberAll();
		for(SubscriptionDo realTime:subscriptionList) {
			if(!StringUtils.equals(realTime.getNumber(), "0")) {
				list.add(realTime.getNumber());
			}
		}
		pool.execute(new Runnable() {
			@Override
			public void run() {
				mockDeal.sendMsgByList(list,"2020-09-24",DingTalkRobotHTTPUtil.APP_TEST_SECRET);
			}
		});
	}
	
	//初始化map
	@Scheduled(cron = "0 25 11 * * MON-FRI")
	public void AiBuyIn() {
		List<SubscriptionDo> subscriptionList=guPiaoService.listMemberAll();
		int max=0;
		int min=0;
		double total=0;
		MockLog maxprice=new MockLog();
		MockLog minprice=new MockLog();
		String winLog="";
		String lossLog="";
		for(StockDo stock : guPiaoService.getAllStock()){
            Calendar calendar = Calendar.getInstance();  
			calendar.add(Calendar.MONTH, -1);
			SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
			MockLog log=mockDeal.mockDeal(stock.getNumber(), dateformat.format(calendar.getTime()),DingTalkRobotHTTPUtil.APP_TEST_SECRET,false);
			if(log == null ) {
				continue;
			}
			//初始化
			if(maxprice.getWin() == null ) {
				maxprice=log;
			}
			if(minprice.getWin() == null ) {
				minprice=log;
			}
			//替换最大值
			if(log.getWin()!=null && log.getWin() >= maxprice.getWin() ) {
				maxprice=log;
			}
			//替换最小值
			if(log.getWin()!=null && log.getWin() <= minprice.getWin()) {
				minprice=log;
				if(log.getWinRate().doubleValue()<=-3) {
					//DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, log.getLogs(), null, false);
				}
			}
			
			if(log.getWinRate() != null && log.getWinRate()>0) {
				winLog+=log.getNumber()+"  "+log.getName()+" "+log.getWinRate()+"\n";
				max++;
			}
			if(log.getWinRate() != null &&  log.getWinRate()<0) { 
				min++;
				lossLog+=log.getNumber()+"  "+log.getName()+" "+log.getWinRate()+"\n";
			}
			//统计金额
			if(log.getWin() != null) {
				total+=log.getWin();
			}
			
			//近5天出现买入点,推荐
			Calendar before = Calendar.getInstance();  
			before.add(Calendar.DATE, -5);
			if(log.getIsBuyin() && log.getLastBuyin()!= null && log.getLastBuyin().after(before.getTime()) ) {
				if(log.getWinRate().doubleValue()>=3 && log.getWinRate().doubleValue()<=40) {
					log.setLogs(log.getLogs().replace("测试AI操盘", "AI个股推荐(箱体策略)"));
					for(SubscriptionDo realTime:subscriptionList) {
						String key=RedisKeyUtil.getStockSellNotify(realTime.getNumber(), realTime.getDingtalkId());
						Boolean isNotifyByMock=(Boolean)redisUtil.get(key);
						//通知开关
						if(isNotifyByMock == null || isNotifyByMock) {
							isNotifyByMock=true;
						}
						
						if(StringUtils.equals(realTime.getNumber(), "0") && isNotifyByMock) {
							isNotifyByMock=false;
							log.setLogs(updateMsg(log.getNumber(),log.getLogs()));
							DingTalkRobotHTTPUtil.sendMsg(realTime.getDingtalkId(), log.getLogs(), null, false);
						}
						redisUtil.set(key,isNotifyByMock,86400L);
					}
				}
			}
		}
		try {
			String  context = "所有机器人总收益:"+total
					+"\n 赚钱的机器人:"+max
					//+"\n 赚钱股票："+winLog
					+"\n 亏钱的机器人:"+min;
					//+"\n 亏钱股票："+lossLog;
					//+"\n 单个机器人最大盈利："
					// + maxprice.getLogs()
					//+"\n 单个机器人最大亏损："
					//+minprice.getLogs();
			DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, context, null, false);
			logger.info(context);
		} catch (Exception e) {
		}
		
	}
	
	@Scheduled(cron = "0 30 10 * * MON-FRI")
	public void EmaGupiao() {
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
				return msg;
			}
		} catch (Exception e) {
			logger.error("updateMsg:"+"number:"+number+"-->"+e.getMessage(),e);
		}
		return msg;
	}
	
	
	
	
	

	
	
	
}

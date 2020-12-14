package com.example.service.task;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.mapper.HistoryDayStockMapper;
import com.example.model.HistoryDayStockDo;
import com.example.model.RiskStockDo;
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
import com.example.uitls.ReadApiUrl;
import com.example.uitls.RedisKeyUtil;
import com.example.uitls.RedisUtil;

@Service
public class RealTimeTask implements InitializingBean {
	private static Logger logger = LoggerFactory.getLogger("real_time");
	
	@Autowired
	private GuPiaoService guPiaoService;
	@Autowired
	private TrendStrategyService trendStrategyService;
	@Autowired
	private ReadApiUrl apiUrl;
	@Resource
	private RedisUtil redisUtil;
	
	
	private static String today=DateUtils.getToday();
	
	ThreadPoolExecutor  pool1 = new ThreadPoolExecutor(20, 300, 5,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(2000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());
	ThreadPoolExecutor  pool2 = new ThreadPoolExecutor(20, 300, 5,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(2000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());
	ThreadPoolExecutor  pool3 = new ThreadPoolExecutor(20, 300, 5,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(2000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());
	ThreadPoolExecutor  pool4 = new ThreadPoolExecutor(20, 300, 5,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(2000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());
	ThreadPoolExecutor  pool5 = new ThreadPoolExecutor(20, 300, 5,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(2000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());
	private static List<StockDo> list= new ArrayList<StockDo>();
	private static List<StockDo> list1= new ArrayList<StockDo>();
	private static List<StockDo> list2= new ArrayList<StockDo>();
	private static List<StockDo> list3= new ArrayList<StockDo>();
	private static List<StockDo> list4= new ArrayList<StockDo>();
	private static List<StockDo> list5= new ArrayList<StockDo>();
	
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			list=guPiaoService.getAllStock();
			int k=list.size()/5;
			list1=list.subList(0, k);
			list2=list.subList(1*k, 2*k);
			list3=list.subList(2*k, 3*k);
			list4=list.subList(3*k, 4*k);
			list5=list.subList(4*k, list.size());
			today=DateUtils.getToday();
			updateRiskStock();
		} catch (Exception e) {
			logger.warn("init Exception:"+e.getMessage(),e);
		}
	}
	
	@Scheduled(cron = "0 3 * * * *")
	public void  todayTask() throws Exception {
		today=DateUtils.getToday();
	}
	
	@Scheduled(cron = "0/30 * 9-15 * * MON-FRI")
	public void  task1() throws Exception {
		updateCache(list1,pool1);
	}
	@Scheduled(cron = "0/30 * 9-15 * * MON-FRI")
	public void  task2() throws Exception {
		updateCache(list2,pool2);
	}
	@Scheduled(cron = "0/30 * 9-15 * * MON-FRI")
	public void  task3() throws Exception {
		updateCache(list3,pool3);
	}
	@Scheduled(cron = "0/30 * 9-15 * * MON-FRI")
	public void  task4() throws Exception {
		updateCache(list4,pool4);
	}
	@Scheduled(cron = "0/30 * 9-15 * * MON-FRI")
	public void  task5() throws Exception {
		updateCache(list5,pool5);
	}
	
	@Scheduled(cron = "0 * 9-15 * * MON-FRI")
	public void updateRiskStock() {
		pool1.execute(new Runnable() {
			@Override
			public void run() {
				for (StockDo stock : list) {
					try {
						String key = RedisKeyUtil.getRiskStock(stock.getNumber());
						if (redisUtil.hasKey(key)) {
							redisUtil.del(key);
						}
						RiskStockDo riskStock = trendStrategyService.getRiskStock(stock.getNumber());
						if (riskStock != null) {
							redisUtil.set(key, riskStock, 36000);
						}
					} catch (Exception e) {
						logger.error(e.getMessage(),e);
					}
				}
			}
		});
	}
	
	
	//实时更新价格到cache
	private void updateCache(List<StockDo> stockList,ThreadPoolExecutor  pool) {
		if(!DateUtils.traceTime(guPiaoService.getHolidayList())) {
			return ;
		}
		for(StockDo stock:stockList) {
			try {
				pool.execute(new UpdateRealTimeTask(guPiaoService,stock.getNumber(),apiUrl,redisUtil));
			}catch (Exception e) {
				logger.warn("pool5 Exception:"+stock.getNumber()+":"+e.getMessage(),e);
			}
		}
	}
	
	
	
	@Scheduled(cron = "0 20 19 * * *")
	public void  updateHistoryTask1() {
		//获取所有股票的历史60分钟数据
		updateHistoryStock(list1,today,pool1);
		updateHistoryStock(list2,today,pool2);
		updateHistoryStock(list3,today,pool3);
		updateHistoryStock(list4,today,pool4);
		updateHistoryStock(list5,today,pool5);
		String robotbuy = MessageFormat.format("更新60分钟数据" ,new Object[] {});
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
	}
	
	//@Scheduled(cron = "0 20 20 * * *")
	public void  updateHistoryTask2() {
		updateIntervalHistoryStock(list1,pool1);
		updateIntervalHistoryStock(list2,pool2);
		updateIntervalHistoryStock(list3,pool3);
		updateIntervalHistoryStock(list4,pool4);
		updateIntervalHistoryStock(list5,pool5);
		String robotbuy = MessageFormat.format("更新波段" ,new Object[] {});
        DingTalkRobotHTTPUtil.sendMsg(DingTalkRobotHTTPUtil.APP_TEST_SECRET, robotbuy, null, false);
	}
	
	private void updateIntervalHistoryStock(final List<StockDo> stockList, final ThreadPoolExecutor pool) {
		for (StockDo stock : stockList) {
			try {
				pool.execute(new Runnable() {
					@Override
					public void run() {
						guPiaoService.timeInterval(stock.getNumber());
					}
				});
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	private void updateHistoryStock(final List<StockDo> stockList, final String today, final ThreadPoolExecutor pool) {
		for (StockDo stock : stockList) {
			try {
				pool.execute(new Runnable() {
					@Override
					public void run() {
						String key = RedisKeyUtil.getRecheckStock(stock.getNumber());
						if (!redisUtil.hasKey(key)) {
							logger.info("更新数据(补60分钟线)--->" + stock.getNumber() + " "
									+ redisUtil.get(RedisKeyUtil.getStockName(stock.getNumber())) + " " + today);
							guPiaoService.updateHistoryStock(stock.getNumber());
							redisUtil.set(key, true, 3500);
						}
					}
				});
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	
}

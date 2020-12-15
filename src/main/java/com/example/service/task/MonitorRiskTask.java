package com.example.service.task;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.example.demo.GuPiao;
import com.example.model.GuPiaoDo;
import com.example.model.RiskStockDo;
import com.example.model.SubscriptionDo;
import com.example.service.GuPiaoService;
import com.example.service.TrendStrategyService;
import com.example.uitls.DateUtils;
import com.example.uitls.DingTalkRobotHTTPUtil;
import com.example.uitls.ReadApiUrl;
import com.example.uitls.RedisKeyUtil;
import com.example.uitls.RedisUtil;

@Service
public class MonitorRiskTask {
	private static Logger logger = LoggerFactory.getLogger("task_log");
	ThreadPoolExecutor  pool = new ThreadPoolExecutor(20, 100, 1,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(1000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());

	@Autowired
	private GuPiaoService guPiaoService;
	
	@Autowired
	private TrendStrategyService trendStrategyService;
	
	@Autowired
	private ReadApiUrl apiUrl;
	@Resource
	private RedisUtil redisUtil;
	
	
	@Scheduled(cron = "0/30 * 9-15 * * MON-FRI")
	private void  monitorAll() throws Exception {
		if(!DateUtils.traceTime(guPiaoService.getHolidayList())) {
			return ;
		}
		
		List<SubscriptionDo> list=guPiaoService.listMemberAll();
		for(SubscriptionDo realTime:list) {
			if(!StringUtils.equals(realTime.getNumber(), "0")) {
				logger.info("实时监控-->"+realTime.getNumber());
				excuteRunListen(realTime.getNumber(),realTime.getDingtalkId(),realTime.getBegintime());
			}
		}
	}


	private void excuteRunListen(final String number,final String appSecret,final String beginTime) {
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					listenRealTime(number,appSecret);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void setRealTimeStatus(String number,Boolean isNotify) {
		redisUtil.set(RedisKeyUtil.getRealTimeStatus(number), isNotify,86500L);
	}
	private Boolean getRealTimeStatus(String number) {
		return (Boolean)redisUtil.get(RedisKeyUtil.getRealTimeStatus(number));
	}
	private void setNotify(String number,String tag,Boolean isNotify) {
		redisUtil.set(RedisKeyUtil.getRealTimeNotify(number,tag), isNotify,1800L);
	}
	private Boolean getNotify(String number,String tag) {
		return (Boolean)redisUtil.get(RedisKeyUtil.getRealTimeNotify(number,tag));
	}

	private void listenRealTime(final String number,final String appSecret) throws Exception {
		DecimalFormat    df   = new DecimalFormat("######0.00");  
		Date now=new Date();
    	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	GuPiao date= apiUrl.readRealTimeUrl(number);
    	if(date == null) {
    		pool.execute(new UpdateRealTimeTask(guPiaoService,number,apiUrl,redisUtil));
    		return;
    	}
		GuPiaoDo nowPrice=new GuPiaoDo();
		BeanUtils.copyProperties(date, nowPrice);
		if(nowPrice.getDangqianjiage()<=0) {
			return;
		}
		
		//风险指标
		RiskStockDo riskPrice=trendStrategyService.getRiskStock(number);
		if(riskPrice == null ) {
			logger.error("找不到风控规则指标："+number);
			return;
		}
		Boolean status= getRealTimeStatus(number);
		
		//弱势 当前价格小于20天线
		if(nowPrice.getDangqianjiage()<riskPrice.getBollDayMid() && status == null) {
				String content = MessageFormat.format("GS【诊断股票走势】"+dateformat.format(now)
		        +"\n------------------------------------ \n股票代码：{0}\n股票名称：{1}\n压力位置:{2}\n变盘位置:{3}\n支撑位置:{4}\n当前价格:{5}\n当前趋势:{6}", 
                 new Object[] {date.getNumber(), 
        		 date.getName(), 
        		 riskPrice.getBollDayUp(),
        		 riskPrice.getBollDayMid(),
        		 riskPrice.getBollDayLower(),
        		 df.format(nowPrice.getDangqianjiage()), 
        		 "目前属于弱趋势"});
				logger.info(content);
				SendMsg(number, appSecret, content,"init");
				setRealTimeStatus(number,true);
				return;
		}
		if(nowPrice.getDangqianjiage()>=riskPrice.getBollDayMid() && status == null){
				String content = MessageFormat.format("GS【诊断股票走势】"+dateformat.format(now)
		        +"\n------------------------------------ \n股票代码：{0}\n股票名称：{1}\n压力位置:{2}\n变盘位置:{3}\n支撑位置:{4}\n当前价格:{5}\n当前趋势:{6}", 
	             new Object[] {date.getNumber(), 
	    		 date.getName(), 
	    		 riskPrice.getBollDayUp(),
	    		 riskPrice.getBollDayMid(),
	    		 riskPrice.getBollDayLower(),
	    		 df.format(nowPrice.getDangqianjiage()), 
	    		 "目前属于强趋势"});
				logger.info(content);
				SendMsg(number, appSecret, content,"init");
				setRealTimeStatus(number,true);
				return;
		}
		
		//止损 当前价格低于历史支撑位
		if(nowPrice.getDangqianjiage()<riskPrice.getBollDayLower()) {
			String content = MessageFormat.format("GS【止损通知】"+dateformat.format(now)
	        +"\n------------------------------------ \n股票代码：{0}\n股票名称：{1}\n压力位置:{2}\n变盘位置:{3}\n支撑位置:{4}\n当前价格:{5}\n当前趋势:{6}", 
	        		                 new Object[] {date.getNumber(), 
	        		        		 date.getName(), 
	        		        		 riskPrice.getBollDayUp(),
	        		        		 riskPrice.getBollDayMid(),
	        		        		 riskPrice.getBollDayLower(),
	        		        		 df.format(nowPrice.getDangqianjiage()), 
	        		        		 "跌破支撑位置，请判断实际情况进行止操作"});
			riskPrice.setStopLoss(nowPrice.getDangqianjiage());
			String key=RedisKeyUtil.getLastHistoryPrice(number, DateUtils.getToday());
			redisUtil.set(key, JSON.toJSONString(riskPrice),86400L);
			logger.info(content);
			SendMsg(number, appSecret, content,"stoploss");
			return;
		}
				
		if(nowPrice.getDangqianjiage()>=riskPrice.getStopProfit() && status!=null) {	
					String content = MessageFormat.format("GS【止盈卖出提示】"+dateformat.format(now)
					 +"\n------------------------------------ \n股票代码：{0}\n股票名称：{1}\n压力位置:{2}\n变盘位置:{3}\n支撑位置:{4}\n当前价格:{5}\n当前趋势:{6}", 
	                 new Object[] {date.getNumber(), 
	        		 date.getName(), 
	        		 riskPrice.getBollDayUp(),
	        		 riskPrice.getBollDayMid(),
	        		 riskPrice.getBollDayLower(),
	        		 df.format(nowPrice.getDangqianjiage()), 
			         "请判断情况，适当止盈，检查交易量，KDJ,MACD等指标"});
					logger.info(content);
					SendMsg(number, appSecret, content,"sell");
					return;
		}
		if(nowPrice.getDangqianjiage()>=riskPrice.getBuyPointBegin() && nowPrice.getDangqianjiage()<=riskPrice.getBuyPointEnd() && status!=null) {	
			String content = MessageFormat.format("GS【买入信号提示】"+dateformat.format(now)
			 +"\n------------------------------------ \n股票代码：{0}\n股票名称：{1}\n压力位置:{2}\n变盘位置:{3}\n支撑位置:{4}\n当前价格:{5}\n当前趋势:{6}", 
	            new Object[] {date.getNumber(), 
		   		 date.getName(), 
		   		 riskPrice.getBollDayUp(),
		   		 riskPrice.getBollDayMid(),
		   		 riskPrice.getBollDayLower(),
		   		 df.format(nowPrice.getDangqianjiage()), 
		         "请判断情况，适当建仓，检查交易量，KDJ,MACD方向提示"});
			logger.info(content);
			SendMsg(number, appSecret, content,"buy");
		}
		if(nowPrice.getChengjiaogupiao()!=null && nowPrice.getChengjiaogupiao().longValue()>=riskPrice.getTop5volume()*2) {	
			String content = MessageFormat.format("GS【交易量大于暴增】"+dateformat.format(now)
			 +"\n------------------------------------ \n股票代码：{0}\n股票名称：{1}\n压力位置:{2}\n变盘位置:{3}\n支撑位置:{4}\n当前价格:{5}\n当前趋势:{6}", 
	            new Object[] {date.getNumber(), 
		   		 date.getName(), 
		   		 riskPrice.getBollDayUp(),
		   		 riskPrice.getBollDayMid(),
		   		 riskPrice.getBollDayLower(),
		   		 df.format(nowPrice.getDangqianjiage()), 
		         "出现放量情况，请注意走势动向把握机会"});
			logger.info(content);
			SendMsg(number, appSecret, content,"bigVolume");
		}
	}


	private void SendMsg(final String number, final String appSecret, String content,String tag) {
		Boolean isNotify;
		isNotify=getNotify(number,tag);
		if(isNotify==null) {
			isNotify=true;
			setNotify(number,tag,isNotify);
		}
		if(isNotify) {
			DingTalkRobotHTTPUtil.sendMsg(appSecret, content, null, false);
			isNotify=false;
			setNotify(number,tag,isNotify);
		}
	}
}

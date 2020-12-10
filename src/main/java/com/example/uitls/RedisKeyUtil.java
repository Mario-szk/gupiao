package com.example.uitls;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.model.RealTimeDo;

public class RedisKeyUtil {

	private final static SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd");
	private final static SimpleDateFormat dateformat_min = new SimpleDateFormat("yyyyMMddHHmm");
	
	public static String getStockName(String number) {
		return "stock_name_"+number;
	}
	
	//股票预警
	public static String getRiskStock(String number) {
		return "risk_stock_"+dateformat.format(new Date())+"_"+number;
	}
	
	//复盘数据
	public static String getRecheckStock(String number) {
		return "recheck_stock_"+dateformat.format(new Date())+"_"+number;
	}
	
	//实时通知降噪
	public static String getRealTimeNotify(String number,String tag) {
		return "realtime_"+dateformat.format(new Date())+"_"+number+"_"+tag;
	}
	
	//个股波段分析
	public static String getBoduanNotify(String number,String appId) {
		return "boduan_"+dateformat.format(new Date())+"_"+number+"_"+appId;
	}
	
	//个股波段分析
	public static String getStockSellNotify(String number,String appId) {
		return "stock_sell_"+dateformat.format(new Date())+"_"+number+"_"+appId;
	}
	
	public static String getEmaStockSellNotify(String number,String appId) {
		return "stock_ema_sell_"+dateformat.format(new Date())+"_"+number+"_"+appId;
	}

	//上一个趋势指标
	public static String getLastHistoryPrice(String number,String today) {
		return "last_history_price_"+today+"_"+number;
	}

	//实时状态判断
	public static String getRealTimeStatus(String number) {
		return "realtime_status_"+dateformat.format(new Date())+"_"+number;
	}

	public static String getRealTime(String number) {
		return "realtime_status_"+dateformat_min.format(new Date())+"_"+number;
	}
	
	public static String getRealTimeByRealTimeDo(RealTimeDo model) {
		return "realTime_RealTimeDo_"+model.getNumber()+"_"+model.getDate().replace("-", "")+"_"+model.getTime().replace(":", "");
	}
	
	public static String getRealTimeListByRealTimeDo(RealTimeDo model) {
		return "realTime_RealTimeDo_list_"+model.getNumber()+"_"+model.getDate().replace("-", "");
	}
}

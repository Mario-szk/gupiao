package com.example.uitls;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.example.demo.GuPiao;
import com.example.model.HistoryPriceDo;

@Service
public class ReadApiUrl {

	private static Logger logger = LoggerFactory.getLogger("api_log");
	
	@Resource
	private RedisUtil redisUtil;
	
	//http://api.finance.ifeng.com/akmin?scode=sz300073&type=60
	public List<HistoryPriceDo> readHistoryApiUrl(String number,int type) {
		String url = "http://api.finance.ifeng.com/akmin?scode=" + number+"&type="+type;
		String code = HttpClientUtil.doGet(url);
		logger.debug("http请求:"+url+" return:"+code);
		if (code == null) {
			logger.warn("http请求数据为空:"+url);
			return null;
		}
		List<HistoryPriceDo> list=new ArrayList<HistoryPriceDo>();
		if(JSON.parseObject(code)==null) {
			return null;
		}
		JSONArray  priceList=(JSONArray)JSON.parseObject(code).get("record");
		double ma20_5=0;
		Calendar calendar=Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY,-24);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String name="";
		
		if(redisUtil.hasKey(RedisKeyUtil.getStockName(number))) {
			name=(String)redisUtil.get(RedisKeyUtil.getStockName(number));
		}
		
		for(int i=0;i<priceList.size();i++) {
			 List<Object> priceObjList=(List<Object>)priceList.get(i);
			 HistoryPriceDo price=new HistoryPriceDo();
			 try {
				 	price.setNumber(number);
				 	price.setName(name);
		            Date date = sdf.parse((String)priceObjList.get(0));
		            price.setDateime(date);
		            price.setKaipanjia(getBigDecimal(priceObjList.get(1)));
		            price.setZuigaojia(getBigDecimal(priceObjList.get(2)));
		            price.setShoupanjia(getBigDecimal(priceObjList.get(3)));
		            price.setZuidijia(getBigDecimal(priceObjList.get(4)));
		            price.setChengjiaoliang(getBigDecimal(priceObjList.get(5)));
		            price.setJiagebiandong(getBigDecimal(priceObjList.get(6)));
		            price.setZhangdiefu(getBigDecimal(priceObjList.get(7)));
		            price.setMa5(getBigDecimal((String)priceObjList.get(8)));
		            price.setMa10(getBigDecimal(priceObjList.get(9)));
		            price.setMa20(getBigDecimal(priceObjList.get(10)));
		            price.setMa5number(getBigDecimal(priceObjList.get(11)));
		            price.setMa10number(getBigDecimal(priceObjList.get(12)));
		            price.setMa20number(getBigDecimal(priceObjList.get(13)));
		            price.setHuanshoulv(getBigDecimal(priceObjList.get(14)));
		            price.setPianlizhi(price.getMa20().divide(price.getShoupanjia(),BigDecimal.ROUND_HALF_UP));
		            if(ma20_5 <=0) {
		            	ma20_5=price.getMa20().doubleValue();
		            }
		            //收盘价比MA20高
		            if(price.getShoupanjia().compareTo(price.getMa20()) > -1) {
		            	if(price.getMa20().doubleValue() - ma20_5>0) {
		            		price.setUp(true);
	            		}
		            }
		            list.add(price);
		        } catch (ParseException e) {
		        	logger.error(e.getMessage(),e);
		        }
		}
		return list;
	}
	
	private static BigDecimal getBigDecimal(Object obj){
		if(obj instanceof String) {
			return new BigDecimal((String)obj);
		}
		String temp=JSON.toJSONString(obj);
		return new BigDecimal(temp);
	}
	
	/**
	 * 
	 * @param number
	 * @param isTemp   false 获取实时数据
	 * @return
	 */
	public GuPiao readRealTimeUrl(String number) {
		String url = "http://hq.sinajs.cn/list=" + number;
		String code = HttpClientUtil.doGet(url);
		logger.debug("http请求:"+url+" return:"+code);
		for(int i=1;i<=5;i++){
			if (code != null) {
				return hanldeData(number, code);
			}
			try {
				Thread.sleep(5000L);
				logger.warn("第"+i+"次，重试请求："+url);
				code= HttpClientUtil.doGet(url);
			} catch (InterruptedException e) {
				logger.warn("http异常："+e.getMessage());
			}
		}
		logger.warn("http请求："+url);
		if (code == null) {
			logger.warn("http请求数据为空："+url);
			return null;
		}

		return hanldeData(number, code);
	}



	private GuPiao hanldeData(String number, String code) {
		if (code.length() > 30) {
			String value = code.split("=")[1];
			value = value.replace("\"", "");
			String[] date = value.split(",");
			if (date.length < 32) {
				return null;
			}
			GuPiao gp = new GuPiao(number, date[0], date[1], date[2], date[3], date[4], date[5], date[6], date[7],
					date[8], date[9], date[10], date[11], date[12], date[13], date[14], date[15], date[16], date[17],
					date[18], date[19], date[20], date[21], date[22], date[23], date[24], date[25], date[26], date[27],
					date[28], date[29], date[30], date[31]);
			return gp;
		}
		return null;
	}

	public GuPiao readUrl(String number, boolean isCache) {
		GuPiao gp =null;
		try {
			if(!isCache) {
				gp= readRealTimeUrl( number);
				return gp;
			}
			String key = RedisKeyUtil.getRealTime(number);
			if(redisUtil.hasKey(key)) {
				gp=  (GuPiao)redisUtil.get(key);
				return gp;
			}
			gp=readRealTimeUrl(number);
			if(gp !=null) {
				redisUtil.set(key, gp, 10L);
			}
		}catch (Exception e) {
			logger.error("="+number+":"+e.getMessage(),e);
		}finally {
			if(gp !=null) {
				String key = RedisKeyUtil.getLastRealTime(number);
				redisUtil.set(key, gp, 36000L);
			}
		}
		
		
		
		return gp;
	}
	
	public GuPiao readUrl(int i, String title, boolean isCache) {
		String number = String.format("%05d", i);
		if(!isCache) {
			return readRealTimeUrl( title + number);
		}
		
		String key = RedisKeyUtil.getRealTime(number);
		if(redisUtil.hasKey(key)) {
			return (GuPiao)redisUtil.get(key);
		}
		GuPiao gp=readRealTimeUrl( title + number);
		redisUtil.set(key, gp, 10L);
		return gp;
	}
}

package com.example.service.task;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.alibaba.fastjson.JSON;
import com.example.demo.GuPiao;
import com.example.model.RealTimeDo;
import com.example.model.RiskStockDo;
import com.example.service.GuPiaoService;
import com.example.uitls.DingTalkRobotHTTPUtil;
import com.example.uitls.ReadApiUrl;
import com.example.uitls.RedisKeyUtil;
import com.example.uitls.RedisUtil;

public class UpdateRealTimeTask  implements Runnable {
	private static Logger ai_logger = LoggerFactory.getLogger("ai_log");
	private static Logger logger = LoggerFactory.getLogger("real_time");
	private String number;
	private GuPiaoService guPiaoService;
	private ReadApiUrl apiUrl;
	private RedisUtil redisUtil;
	
	@Override
	public void run() {
			try {
				GuPiao date=apiUrl.readRealTimeUrl(number,true);
				if(date !=null) {
					RealTimeDo model=new RealTimeDo();
					BeanUtils.copyProperties(date, model);
					if(model.getTop()==null ||model.getLow()==null||model.getKaipanjia()==null||model.getZuorishoupanjia()==null||model.getChengjiaogupiao()==null) {
						logger.error("空值数据："+model.getName()+" "+model.getNumber()+" "+JSON.toJSONString(model));
						return ;
					}
					if(model.getTop()<=0.1||model.getLow()<=0.1||model.getKaipanjia()<=0.1||model.getZuorishoupanjia()<=0.1||model.getChengjiaogupiao()<=0.1) {
						logger.error("异常数据："+model.getName()+" "+model.getNumber()+" 最高价:"+model.getTop()+" 最低价："+model.getLow()+" 开盘价："+model.getKaipanjia()+" 收盘价："+model.getZuorishoupanjia()+" 成交量："+model.getChengjiaogupiao());
						return ;
					}
					String key = RedisKeyUtil.getRealTimeByRealTimeDo(model);
					if(redisUtil.hasKey(key)) {
						logger.info("读取缓存:"+number+" "+model.getName()+" 时间:"+model.getDate()+" "+model.getTime()+" 当前价格:"+model.getDangqianjiage());
						return ;
					}
					redisUtil.set(key, model,60);
					String key2 =RedisKeyUtil.getRealTimeListByRealTimeDo(model);
					@SuppressWarnings("unchecked")
					Map<String,RealTimeDo> map=(Map<String,RealTimeDo>) redisUtil.get(key2);
					if(map == null) {
						map=new HashMap<String,RealTimeDo>();
					}
					String mapKey=model.getDate()+"_"+model.getTime();
					mapKey=mapKey.replace("-", "");
					mapKey=mapKey.replace(":", "");
					map.put(mapKey, model);
					redisUtil.set(key2, map,43200);
					String key3 =RedisKeyUtil.getRealTime(number);
					redisUtil.set(key3, date,30);
					logger.info("写入缓存成功:"+number+" "+model.getName()+" 时间:"+model.getDate()+" "+model.getTime()+" 当前价格:"+model.getDangqianjiage());
					String key4 = RedisKeyUtil.getRiskStock(number);
					if (!redisUtil.hasKey(key4)) {
						logger.error("查询失败,key不存在:"+key4);
						return ;
					}
					RiskStockDo riskStock =(RiskStockDo)redisUtil.get(key4);
					if(riskStock ==null ) {
						logger.error("转换对象失败,检查key的内容:"+key4);
						return;
					}
					String appSecret="477b77570a86de89c4c3a43a662e498d4262e7382ea0b0332563d88c93adc3fc";
					String tag="bigVolume";
					Boolean isNotify=getNotify(number,tag);
					if(isNotify==null) {
						isNotify=true;
						setNotify(number,tag,isNotify);
					}
					if(model.getDangqianjiage()>model.getKaipanjia() && model.getDangqianjiage()>=riskStock.getBollDayMid() 
							&& model.getChengjiaogupiao().longValue() > (riskStock.getTop5volume()*3) && isNotify) {
						String content="GS=========实时机会，量价突增==========\n股票编码："+number
								+"\n股票名称："+model.getName()
								+"\n 上轨价格："+riskStock.getBollDayUp()
								+"\n 中轨价格："+riskStock.getBollDayMid()
								+"\n 下轨价格："+riskStock.getBollDayLower()
								+"\n开盘价："+model.getKaipanjia()
								+"\n 现价："+model.getDangqianjiage()
								+"\n现在成交量："+model.getChengjiaogupiao().longValue()
								+"\n过去成交量："+riskStock.getTop5volume()
								+"\n配合MACD，JDK,形态操作："+riskStock.getTop5volume()
								;
						DingTalkRobotHTTPUtil.sendMsg(appSecret, content, null, false);
						isNotify=false;
						setNotify(number,tag,isNotify);
						ai_logger.info(content);
					}
				}
			} catch (Exception e) {
				logger.warn(e.getMessage(),e);
			}
	}

	private void setNotify(String number,String tag,Boolean isNotify) {
		redisUtil.set(RedisKeyUtil.getRealTimeNotify(number,tag), isNotify,1800L);
	}
	private Boolean getNotify(String number,String tag) {
		return (Boolean)redisUtil.get(RedisKeyUtil.getRealTimeNotify(number,tag));
	}
	
	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public GuPiaoService getGuPiaoService() {
		return guPiaoService;
	}

	public void setGuPiaoService(GuPiaoService guPiaoService) {
		this.guPiaoService = guPiaoService;
	}

	public UpdateRealTimeTask(GuPiaoService guPiaoService,String number,ReadApiUrl apiUrl,RedisUtil redisUtil) {
		this.guPiaoService = guPiaoService;
		this.number = number;
		this.apiUrl = apiUrl;
		this.redisUtil = redisUtil;
	}

	
}

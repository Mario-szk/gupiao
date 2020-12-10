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
	private static Logger logger = LoggerFactory.getLogger("real_time");
	private String number;
	private GuPiaoService guPiaoService;
	private ReadApiUrl apiUrl;
	private RedisUtil redisUtil;
	
	@Override
	public void run() {
			try {
				GuPiao date=apiUrl.readUrl(number,false);
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
					map.put(model.getDate()+model.getTime(), model);
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
					String appSecret="bb888ac7199ba68c327c8a0e44fbf0ee6b65b5b0f490beb39a209a295e132a4f";
					String tag="bigVolume";
					Boolean isNotify=getNotify(number,tag);
					if(isNotify==null) {
						isNotify=true;
						setNotify(number,tag,isNotify);
					}
					if(model.getChengjiaogupiao() > riskStock.getTop5volume()*1.5 && isNotify) {
						String content="TEST=========成交量比5日内平均成交量高1.5倍==========/n股票编码："+number+"/n股票名称："+model.getName();
						DingTalkRobotHTTPUtil.sendMsg(appSecret, content, null, false);
						isNotify=false;
						setNotify(number,tag,isNotify);
					}
					
				}else {
					logger.error("查询失败:"+number);
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

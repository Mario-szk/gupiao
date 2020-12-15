package com.example.ai;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.GuPiao;
import com.example.mapper.HistoryStockMapper;
import com.example.model.GuPiaoDo;
import com.example.model.HistoryPriceDo;
import com.example.model.HistoryStockDo;
import com.example.model.MockLog;
import com.example.uitls.DateUtils;
import com.example.uitls.DingTalkRobotHTTPUtil;
import com.example.uitls.ReadApiUrl;
import com.example.uitls.RedisUtil;

@Service
public class MockDeal {
	private static Logger logger = LoggerFactory.getLogger("mock_log");
	private static ConcurrentHashMap<String, Boolean> buyPorintMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, BigDecimal> maxPriceMap = new ConcurrentHashMap<String, BigDecimal>();
	private static ConcurrentHashMap<String, BigDecimal> minPriceMap = new ConcurrentHashMap<String, BigDecimal>();
	@Resource
	private RedisUtil redisUtil;
	@Autowired
	private ReadApiUrl apiUrl;
	@Autowired
	private HistoryStockMapper historyStockMapper;
	
	
	public  void sendMsgByList(List<String>listTest,String beginDate,String appSecret) {
		for(String number:listTest) {
			try {
				mockDeal( number, beginDate, appSecret,true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private  List<HistoryPriceDo> getHistoryDate(String number){
		try {
			List<HistoryPriceDo> list = apiUrl.readHistoryApiUrl(number, 60);
			if(list == null) {
				logger.warn("当前股票没有数据："+number);
				return null;
			}
			HistoryPriceDo nowPrice = list.get(list.size()-1);
			if(!DateUtils.isSameDay(nowPrice.getDateime())) {
				GuPiao date=apiUrl.readRealTimeUrl(nowPrice.getNumber(),true);
				if(date !=null) {
					GuPiaoDo nowRealTimePrice=new GuPiaoDo();
					BeanUtils.copyProperties(date, nowRealTimePrice);
					if(nowRealTimePrice.getDangqianjiage()>0) {
						nowPrice.setName(nowRealTimePrice.getName());
						nowPrice.setKaipanjia(new BigDecimal(nowRealTimePrice.getKaipanjia()).setScale(3,BigDecimal.ROUND_HALF_DOWN));
						nowPrice.setShoupanjia(new BigDecimal(nowRealTimePrice.getDangqianjiage()).setScale(3,BigDecimal.ROUND_HALF_DOWN) );
						nowPrice.setDateime(DateUtils.getDateForString(nowRealTimePrice.getDate(),nowRealTimePrice.getTime()));
						list.add(nowPrice);
					}
				}
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public  List<HistoryStockDo> cutList(String number,String beginTime,String endTime) {
		List<HistoryStockDo> tempList = historyStockMapper.getNumber(number);
		List<HistoryStockDo> list = new ArrayList<HistoryStockDo>();
		for(HistoryStockDo price:tempList) {
			if(DateUtils.belongCalendar2(DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),DateUtils.getDateForYYYYMMDDHHMM_NUMBER(beginTime),DateUtils.getDateForYYYYMMDDHHMM_NUMBER(endTime))) {
				list.add(price);
			}
		}
		return list;
	}
	
	
	
	
	public  MockLog mockDeal(String number,String beginDate,String appSecret,Boolean isSendMsg) {
		try {
			List<HistoryPriceDo> list = getHistoryDate(number);
			if(list == null) {
				logger.warn("当前股票没有数据："+number);
				return null;
			}
			MockLog mockLog=mockDeal(list, beginDate);
			if(isSendMsg) {
				DingTalkRobotHTTPUtil.sendMsg(appSecret, mockLog.getLogs(), null, false);
			}
			logger.info(mockLog.getLogs());
			return mockLog;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public  MockLog mockDeal(List<HistoryPriceDo> stortList, String beginDate) {
		MockLog mockLog=new MockLog();
		if (stortList == null || stortList.isEmpty()) {
			logger.warn("http请求数据为空");
			mockLog.setLogs("http请求数据为空");
			return mockLog;
		}
		
		if (stortList.size()<100) {
			logger.warn("数据量不满100个60分钟线");
			mockLog.setLogs("数据量不满100个60分钟线");
			return mockLog;
		}
		int maxPower=0;
		int powerValue = 0;
		int sellCount = 0;
		int num = 0;
		double init = 100000;
		BigDecimal keepPrice=new BigDecimal(0.0);
		BigDecimal zuotianPrice=new BigDecimal(0.0);
		int ma20_count=0;
		double ma20 = 0;
		double allwin = 0;
		double total = init;
		double boxMax=0.0;
		double boxMin=0.0;
		int keepDown=0;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
		for (HistoryPriceDo price : stortList) {
			if(StringUtils.isBlank(mockLog.getName())) {
				mockLog.setName(price.getName());
			}
			if(StringUtils.isBlank(mockLog.getNumber())) {
				mockLog.setNumber(price.getNumber());
			}
			
			try {
				Date date = price.getDateime();
				if(StringUtils.isBlank(beginDate) && mockLog.getBeginTime()==null) {
					mockLog.setBeginTime(date);
				}
				if (!StringUtils.isBlank(beginDate)) {
					Date bgtime = sdf1.parse(beginDate);
					if (bgtime.compareTo(date) >= 1) {
						continue;
					}
					if(mockLog.getBeginTime() == null) {
						mockLog.setBeginTime(bgtime);
					}
				}
				mockLog.setEndTime(date);
				
				String buyKey = sdf1.format(price.getDateime()) + "_" + price.getNumber();
				Boolean isBuy = buyPorintMap.get(buyKey);
				if (isBuy == null) {
					isBuy = true;
					buyPorintMap.put(buyKey, isBuy);
				}

				BigDecimal max = price.getZuigaojia();
				BigDecimal tempMax = maxPriceMap.get(price.getNumber());
				BigDecimal min = price.getZuidijia();
				BigDecimal tempMin = minPriceMap.get(price.getNumber());
				if(ma20 >0 ) {
					if(price.getMa20().doubleValue()-ma20>0) {
						price.setUp(true);
					}else {
						price.setUp(false);
					}
				}
				ma20_count++;
				if (ma20 <=0 || ma20_count % 6 == 0) {
					ma20 = price.getMa20().doubleValue();
				}
				if(boxMax<=0) {
					boxMax=price.getShoupanjia().doubleValue();
				}
				if(boxMin<=0) {
					boxMin=price.getShoupanjia().doubleValue();
				}
				if(keepPrice.compareTo(new BigDecimal(0.0)) < 1) {
					keepPrice=price.getShoupanjia();
				}
				if(zuotianPrice.compareTo(new BigDecimal(0.0)) < 1) {
					zuotianPrice=price.getShoupanjia();
				}
				if (null == tempMax || tempMax.compareTo(max) < 1) {
					maxPriceMap.put(price.getNumber(), max);
				}
				if (null == tempMin || tempMin.compareTo(min) > -1) {
					minPriceMap.put(price.getNumber(), min);
				}
				DecimalFormat df = new DecimalFormat("0.00");
				
				// 收盘价比MA20高
				if (price.getShoupanjia().compareTo(price.getMa20()) > -1) {
					//强势重置
					if(powerValue<=-20) {
						powerValue=-4;
						price.setUp(false);
						ma20=price.getMa20().doubleValue();
						String log= " 强势反弹 建议买入==> "
								+ sdf.format(price.getDateime()) 
								//+ " MA20:" + price.getMa20().doubleValue()
								//+ " 偏移量:" + price.getPianlizhi()
								//+ " 当前能量值：" + powerValue 
								+ " 当前价格：" + price.getShoupanjia();
								logger.info(log);
								mockLog.setLogs(mockLog.getLogs()+log+"\n");
					}else if(powerValue<0) {
						//重置
						powerValue=-1;
						ma20=price.getMa20().doubleValue();
					}
					if(price.getShoupanjia().doubleValue()>=boxMax) {
						boxMax=price.getShoupanjia().doubleValue();
						String log= "创新箱体新高 ==> "
								+ sdf.format(price.getDateime()) 
								+ " MA20:" + price.getMa20().doubleValue()
								+ " 偏移量:" + price.getPianlizhi()
								+ " 当前能量值：" + powerValue 
								+ " 箱体min：" + boxMin
								+ " 箱体max：" + boxMax
								+ " 当前价格：" + price.getShoupanjia();
								logger.info(log);
								//mockLog.setLogs(mockLog.getLogs()+log+"\n");
					}
					if(powerValue ==0) {
						price.setUp(true);
						boxMin=price.getMa20().doubleValue();
						String log= "强势变盘，重置箱体下限 ==> "
								+ sdf.format(price.getDateime()) 
								+ " MA20:" + price.getMa20().doubleValue()
								+ " 偏移量:" + price.getPianlizhi()
								+ " 当前能量值：" + powerValue 
								+ " 箱体min：" + boxMin
								+ " 箱体max：" + boxMax
								+ " 当前价格：" + price.getShoupanjia();
								logger.info(log);
								//mockLog.setLogs(mockLog.getLogs()+log+"\n");
					}
					//现在的收盘价少于上个一个收盘价，
					if(price.getShoupanjia().compareTo(zuotianPrice) == -1) {
						keepDown++;
						powerValue = powerValue -(keepDown * 1);
					}else {
						keepDown=0;
					}
					sellCount = 0;
					if (num > 0) {
						String log= " 持有==> "
						+ sdf.format(price.getDateime()) 
						+ " MA20:" + price.getMa20().doubleValue()
						+ " 偏移量:" + price.getPianlizhi()
						+ " 当前能量值：" + powerValue 
						+ " 当前价格：" + price.getShoupanjia() 
						+ " 净利价：" + df.format(price.getShoupanjia().subtract(keepPrice));
						//logger.info(log);
						//mockLog.setLogs(mockLog.getLogs()+log+"\n");
					}
					BigDecimal maybePrice=price.getMa20().multiply(new BigDecimal("1.02"));
					if (num <= 0 && price.getShoupanjia().compareTo(maybePrice) == -1 && powerValue >= 0 && powerValue <20  && buyPorintMap.get(buyKey) && price.isUp()) {
						
						buyPorintMap.put(buyKey, false);
						keepPrice = price.getShoupanjia();
						String log="";
						if((allwin / init) * 100<=-3) {
							num =  1;
							total = total - price.getShoupanjia().doubleValue() * 100 * num;
							log=" 小量买入观察 当前买入点==> "
									+ sdf.format(price.getDateime()) 
									//+ " 能量值：" + powerValue 
									+ " 买入价格价格：" + price.getShoupanjia() ;
//									+ " 数量：" + (num * 100)
//									+ " 余额：" + df.format(total);
						}else {
							num = (int) Math.floor(total / (price.getShoupanjia().doubleValue() * 100));
							total = total - price.getShoupanjia().doubleValue() * 100 * num;
							log=" 买入信号点==> "
									+ sdf.format(price.getDateime()) 
									//+ " 能量值：" + powerValue 
									//+ " 偏离值："+ (price.getMa20().longValue()/price.getShoupanjia().doubleValue())
									//+ " 20价格：" + price.getMa20()
									+ " 买入价格：" + price.getShoupanjia() ;
//									+ " 数量：" + (num * 100)
//									+ " 余额：" + df.format(total);
						}
						logger.info(log);
						mockLog.setLastBuyin(date);
						mockLog.setLogs(mockLog.getLogs()+log+"\n");
					}
					powerValue++;
				} else {
					if(price.getShoupanjia().doubleValue()<=boxMin) {
						boxMin=price.getShoupanjia().doubleValue();
						String log= "创箱体新低，主要风险 ==> "
								+ sdf.format(price.getDateime()) 
								+ " MA20:" + price.getMa20().doubleValue()
								+ " 偏移量:" + price.getPianlizhi()
								+ " 当前能量值：" + powerValue 
								+ " 箱体min：" + boxMin
								+ " 箱体max：" + boxMax
								+ " 当前价格：" + price.getShoupanjia();
								logger.info(log);
								//mockLog.setLogs(mockLog.getLogs()+log+"\n");
					}
					if(powerValue ==0) {
						boxMax=price.getMa20().doubleValue();
						String log= "强势变弱盘，重置箱体上限 ==> "
								+ sdf.format(price.getDateime()) 
								+ " MA20:" + price.getMa20().doubleValue()
								+ " 偏移量:" + price.getPianlizhi()
								+ " 当前能量值：" + powerValue 
								+ " 箱体min：" + boxMin
								+ " 箱体max：" + boxMax
								+ " 当前价格：" + price.getShoupanjia();
								logger.info(log);
								//mockLog.setLogs(mockLog.getLogs()+log+"\n");
					}
					sellCount++;
					// 止损逻辑
					double totaltemp = total + price.getShoupanjia().doubleValue() * 100 * num;
					double temp11 = (totaltemp-init)/init*100;
					if (temp11 < -3 && buyPorintMap.get(buyKey)) {
						total = total + price.getShoupanjia().doubleValue() * 100 * num;
						double win = total - init;
						allwin = allwin + win;
						num = 0;
						String log=" 止损卖出==> "
								+ sdf.format(price.getDateime()) 
							//	+ " 能量值：" + powerValue 
								+ " 卖出价格：" + df.format(price.getShoupanjia()) 
//								+ " 盈利：" + df.format(win)
								+ " 本次盈利率：" + df.format((win / init) * 100) + "%" ;
//								+ "% 累计:" + df.format(allwin) 
//								+ " 累计盈利率:" + df.format((allwin / init) * 100) + "%\n";
						logger.info(log);
						mockLog.setFail(mockLog.getFail()+1);
						mockLog.setLogs(mockLog.getLogs()+log+"\n");
						total = init;
						maxPower=0;
					}

					if (sellCount == 1 && num > 0 && buyPorintMap.get(buyKey)) {
						buyPorintMap.put(buyKey, false);
						total = total + price.getMa20().doubleValue() * 100 * num;
						double win = total - init;
						allwin = allwin + win;
						num = 0;
						String log=" 卖出信号点==> "
						+ sdf.format(price.getDateime()) 
					//	+ " 能量值：" + powerValue 
						+ " 卖出价格：" + df.format(price.getShoupanjia()) 
//						+ " 净利价：" + df.format(price.getShoupanjia().subtract(keepPrice))
//						+ " 盈利：" + df.format(win)
						+ " 盈利率：" + df.format((win / init) * 100)  + "%";
//						+ "% 累计:" + df.format(allwin) 
//						+ " 累计盈利率:" + df.format((allwin / init) * 100) + "%\n";
						logger.info(log);
						mockLog.setSuccess(mockLog.getSuccess()+1);
						mockLog.setLogs(mockLog.getLogs()+log+"\n");
						total = init;
						maxPower=0;
					}
					powerValue--;
				}
				zuotianPrice=price.getShoupanjia();
				String log=" 箱体==> "
						+ sdf.format(price.getDateime()) 
						+ " 能量值：" + powerValue 
						+ " 当前价格:" + price.getShoupanjia().doubleValue()
						+ " 边界上:" + price.getMa20().doubleValue()*1.03
						+ " 边界下:" + price.getMa20().doubleValue()
						+ " 箱体min：" + boxMin
						+ " 箱体max：" + boxMax
						+ " keepDown:"+keepDown;
						logger.info(log);
				//mockLog.setLogs(mockLog.getLogs()+log+"\n");
				
				price.setPowerValue(powerValue);
				mockLog.setPowerValue(powerValue);
				mockLog.setWin(allwin);
				mockLog.setWinRate((allwin / init) * 100);
				mockLog.setPrice(price);
				
			} catch (ParseException e) {
				logger.error(e.getMessage(), e);
			}  
		}
		if(num>0) {
			mockLog.setIsBuyin(true);
		}
		DecimalFormat df = new DecimalFormat("0.00");
		String context=mockLog.getLogs();
		if(StringUtils.isBlank(context)) {
			context="该走势还没找到合适的买入点";
		}
		String tempName=(String)redisUtil.get(mockLog.getNumber());
		if(!StringUtils.isBlank(tempName)) {
			mockLog.setName(tempName);
		}
		context = "GS=========测试AI操盘=================="
				 +"\n 股票编码："+ mockLog.getNumber()
				 +"\n 股票名称："+mockLog.getName()
				 +"\n 回测数据："+sdf.format(mockLog.getBeginTime())+"~"+sdf.format(mockLog.getEndTime())
//				 +"\n 初始资金："+df.format(init)
				 +"\n 成功次数："+mockLog.getSuccess()
				 +"\n 失败次数："+mockLog.getFail()
//				 +"\n 总盈利："+df.format(mockLog.getWin())
				 +"\n 总盈利率："+df.format(mockLog.getWinRate())+"%"
				 +"\n ======操作记录===================="
				 +"\n"+context;
		mockLog.setLogs(context);
		return mockLog;
	}
	
}

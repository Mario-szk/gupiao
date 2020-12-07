package com.example.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.example.ai.MockDeal;
import com.example.mapper.GuPiaoMapper;
import com.example.mapper.HistoryDayStockMapper;
import com.example.mapper.HistoryStockMapper;
import com.example.mapper.HolidayMapper;
import com.example.mapper.RealTimeMapper;
import com.example.mapper.RobotAccountMapper;
import com.example.mapper.RobotSetMapper;
import com.example.mapper.StockMapper;
import com.example.mapper.SubscriptionMapper;
import com.example.mapper.TradingRecordMapper;
import com.example.model.GuPiaoDo;
import com.example.model.HistoryDayStockDo;
import com.example.model.HistoryPriceDo;
import com.example.model.HistoryStockDo;
import com.example.model.HolidayDo;
import com.example.model.RealTimeDo;
import com.example.model.StockDo;
import com.example.model.SubscriptionDo;
import com.example.model.ths.HistoryRsDate;
import com.example.uitls.DateUtils;
import com.example.uitls.ReadApiUrl;
import com.example.uitls.RedisKeyUtil;
import com.example.uitls.RedisUtil;

import Ths.JDIBridge;

@Service
public class GuPiaoServiceImpl implements GuPiaoService, InitializingBean {

	ThreadPoolExecutor  pool = new ThreadPoolExecutor(10, 20, 5,TimeUnit.SECONDS,
			new LinkedBlockingDeque<Runnable>(1000), 
			Executors.defaultThreadFactory(), 
			new ThreadPoolExecutor.CallerRunsPolicy());

	
	private static Logger logger = LoggerFactory.getLogger(GuPiaoServiceImpl.class);

	@Autowired
	private GuPiaoMapper guPiaoMapper;

	@Autowired
	private RealTimeMapper realTimeMapper;

	@Autowired
	private StockMapper stockMapper;

	@Autowired
	private SubscriptionMapper subscriptionMapper;

	@Autowired
	private RobotSetMapper robotSetMapper;
	
	@Autowired
	private RobotAccountMapper robotAccountMapper;
	
	@Autowired
	private TradingRecordMapper tradingRecordMapper;
	
	@Autowired
	private HistoryDayStockMapper historyDayStockMapper;
	
	
	@Autowired
	private HistoryStockMapper historyStockMapper;
	
	@Autowired
	private HolidayMapper holidayMapper;
	
	@Autowired
	private MockDeal mockDeal;
	
	@Resource
	private RedisUtil redisUtil;
	
	@Autowired
	private ReadApiUrl readApiUrl;
	


	private void updateHistoryStockByDB(List<HistoryStockDo> list,Integer type,String remark) {
		BigDecimal max=new BigDecimal(0.00);
		BigDecimal min=new BigDecimal(100000.00);
		BigDecimal avg=new BigDecimal(0.00);
		for(HistoryStockDo stock:list) {
			if(stock.getHeight().compareTo(max)>= 1) {
				max=stock.getHeight();
			}
			if(stock.getLow().compareTo(min)<= -1) {
				min=stock.getLow();
			}
			avg=avg.add(stock.getShoupanjia());
		}
		avg=avg.divide(new BigDecimal(list.size()),3,BigDecimal.ROUND_HALF_UP);
		for(HistoryStockDo stock:list) {
			stock.setBoxMax(max);
			stock.setBoxMin(min);
			stock.setBoxAvg(avg);
			stock.setType(type);
			stock.setRemark(remark);
			try {
				historyStockMapper.updateHistoryStock(stock);
			}catch (Exception e) {
				e.printStackTrace();
				System.err.println(remark);
			}
		}
	}
	
	
	@Override
	public void updateHistoryStock(String number) {
		List<HistoryPriceDo> list = readApiUrl.readHistoryApiUrl(number, 60);
		if (list == null || list.isEmpty()) {
			logger.warn("没有获取到数据"+number);
			return;
		}
		logger.warn("获取到数据"+number);
		final SimpleDateFormat df1 = new SimpleDateFormat("yyyyMMdd");// 设置日期格式
		final SimpleDateFormat df2 = new SimpleDateFormat("yyyyMMddHHmm");// 设置日期格式
		for (HistoryPriceDo price : list) {
			HistoryStockDo tr = new HistoryStockDo();
			tr.setKaipanjia(price.getKaipanjia());
			tr.setShoupanjia(price.getShoupanjia());
			tr.setHeight(price.getZuigaojia());
			tr.setLow(price.getZuidijia());
			tr.setMa20Hour(price.getMa20());
			tr.setMa20Day(new BigDecimal(0));
			tr.setHistoryDay(df1.format(price.getDateime()));
			tr.setHistoryAll(df2.format(price.getDateime()));
			tr.setNumber(number);
			try {
				if (historyStockMapper.getByTime(tr) == null) {
					historyStockMapper.insert(tr);
					logger.info("补60分钟数据到数据库成功："+number+" 数据日期："+tr.getHistoryAll());
				}
			} catch (Exception e) {
				logger.warn(e.getMessage(),e);
			}	
		}
	}

	@Override
	public boolean realTimeInsert(RealTimeDo model) {
		try {
			int i = realTimeMapper.insert(model);
			return i > 0 ? true : false;
		} catch (Exception ex) {

		}
		return false;
	}

	@Override
	public boolean guPiaoInsert(GuPiaoDo model) {
		GuPiaoDo data = guPiaoMapper.getNumber(model.getNumber());
		if (data != null) {
			guPiaoMapper.delete(data.getId());
		}
		int i = guPiaoMapper.insert(model);
		return i > 0 ? true : false;
	}

	@Override
	public List<GuPiaoDo> listAll() {
		return guPiaoMapper.getAll();
	}

	@Override
	public StockDo getNumber(String number) {
		try {
			return stockMapper.getNumber(number);
		} catch (Exception ex) {
		}
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public void updateStock(String number, String name, int type) {
		StockDo obj = stockMapper.getNumber(number);
		if (obj == null) {
			obj = new StockDo();
			obj.setCreateDate(new Date());
			obj.setNumber(number);
			obj.setName(name);
			obj.setType(type);
			obj.setStatus(1);
			stockMapper.insert(obj);
		}
	}

	@Override
	public List<StockDo> getAllStock() {
		return stockMapper.getAll();
	}

	@Override
	public List<SubscriptionDo> listMemberAll() {
		try {
			return subscriptionMapper.getAll();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
		return null;
	}

	@Override
	public List<HolidayDo> getHolidayList() {
		return holidayMapper.getAll();
	}

	@Override
	public HistoryPriceDo getLastZhichengwei(String number) {
		HistoryPriceDo last=new HistoryPriceDo();
		String key=RedisKeyUtil.getLastHistoryPrice(number, DateUtils.getToday());
		if(redisUtil.hasKey(key)) {
			last=JSON.parseObject((String)redisUtil.get(key), HistoryPriceDo.class);
			if(last!=null && last.getYaliwei()!=null) {
				return last;
			}
		}
		
		List<HistoryStockDo>priceList=getLastHistoryStock(number,1);
		if(priceList.size()<1) {
			return null; 
		}
		HistoryStockDo price=priceList.get(0);
		last.setZhichengwei(price.getBoxMin());
		last.setYaliwei(price.getBoxMax());
		last.setMa20(price.getBoxAvg());
		last.setNumber(number);
		last.setName((String)redisUtil.get(RedisKeyUtil.getStockName(number)));
		logger.info("获取上一个趋势："+JSON.toJSONString(price));
		redisUtil.set(key, JSON.toJSONString(price),86400L);
		
		return last;
	}
	
	@Override
	public void timeInterval(String number) {
		logger.info("开始计算波段："+number);
		String returnStr="GS======测试波段区间分隔=========\n";
		returnStr=returnStr+"股票编码："+number+" \n";
		returnStr=returnStr+"股票名称："+(String)redisUtil.get(RedisKeyUtil.getStockName(number))+" \n";
		
		List<HistoryStockDo> stortList=getBoduanList(number);
		if(stortList == null) {
			returnStr=returnStr+"股票找不到波段，返回空";
			logger.warn(returnStr);
			return ;
		}
		if(stortList.size()<2) {
			returnStr=returnStr+"股票波段少于2，返回空";
			logger.warn(returnStr);
			return;
		}
		
		for(int i=1;i<stortList.size();i++) {
			HistoryStockDo lastPrice=stortList.get(i-1);
			HistoryStockDo nowPrice=stortList.get(i);
			BigDecimal subtract=nowPrice.getShoupanjia().subtract(lastPrice.getShoupanjia());
			long days=DateUtils.getDefDays(DateUtils.getDateForYYYYMMDDHHMM_NUMBER(lastPrice.getHistoryAll()),DateUtils.getDateForYYYYMMDDHHMM_NUMBER(nowPrice.getHistoryAll()),getHolidayList());
			int type=2;
			String str="下滑趋势";
			if(subtract.compareTo(new BigDecimal(0.0))>0) {
				str="上升趋势 ";
				type=1;
			}
			if(days <5 ) {
				str="震荡行情,"+str;
				type=3;
			}
			List<HistoryStockDo> list = mockDeal.cutList(number, lastPrice.getHistoryAll(), nowPrice.getHistoryAll());
			BigDecimal max=new BigDecimal(0.0);
			BigDecimal min=new BigDecimal(100000.0);
			BigDecimal avg=new BigDecimal(0.0);
			int count=0;
			
			boolean isPoint=true;
			for (HistoryStockDo price : list) {
				count++;
				avg=avg.add(price.getShoupanjia());
				if(price.getHeight().compareTo(max)>-1) {
					max=price.getHeight();
				}
				if(price.getLow().compareTo(min)< 1) {
					min=price.getLow();
				}
				if(isPoint && count==1 && StringUtils.contains(str, "下滑趋势")) {
					str=str+"\t 参考 卖出点1:"+price.getShoupanjia()+"\t";
					isPoint=false;
				}
				if(isPoint && count==1 && StringUtils.contains(str, "上升趋势")) {
					str=str+"\t 参考 买入点1:"+price.getShoupanjia()+"\t";
					isPoint=false;
				}
				if(isPoint && days>=3 && days<5 && StringUtils.contains(str, "上升趋势")) {
					str=str+"\t 买入点2:"+price.getShoupanjia()+"\t";
					isPoint=false;
				}
			}
			if(count == 0) {
				count=1;
			}
			
			avg = avg.divide(new BigDecimal(count),2, BigDecimal.ROUND_UP);
			max=max.setScale(2);
			min=min.setScale(2);
			String context=lastPrice.getHistoryDay()+"~"+nowPrice.getHistoryDay()
			+"\t 压力位:"+max
			+"\t 支撑位:"+min
			+"\t 平均值："+avg
			+"\t 趋势："+ str
			+"\t 相隔周期："+days+"交易日 \n";
			returnStr=returnStr+context;
			updateHistoryStockByDB(list,type,context);
		}
	}


	private List<HistoryStockDo> getBoduanList(String number) {
		List<HistoryStockDo> list=new ArrayList<HistoryStockDo>();
		List<HistoryStockDo> stortList = historyStockMapper.getNumber(number);
		if(stortList == null) {
			logger.warn("当前股票没有数据："+number);
			return null;
		}
		if (stortList.size()<100) {
			logger.warn("数据量不满100个60分钟线");
			return null;
		}
		int downCount=0;
		int upCount=0;
		Set<String> date=new HashSet<String>();
		BigDecimal max=new BigDecimal(0.0).setScale(3);
		BigDecimal min=new BigDecimal(0.0).setScale(3);
		HistoryStockDo lastPrice=null;
		for (HistoryStockDo price : stortList) {
			//初始化
			if(max.compareTo(new BigDecimal(0.0)) < 1 || min.compareTo(new BigDecimal(0.0)) < 1) {
				max=price.getHeight();
				min=price.getLow();
			}
			if(lastPrice ==null) {
				lastPrice=price;
			}
			
			if(price.getHeight().compareTo(max)>= 1) {
				max=price.getHeight();
			}
			if(price.getLow().compareTo(min)<= -1) {
				min=price.getLow();
			}
			//收盘价在20均线之上属于强势
			if(price.getShoupanjia().compareTo(price.getMa20Hour())>= 0 ) {
				upCount++;
				downCount=0;
				min=price.getMa20Hour();
			}
			//收盘价在20均线之下属于弱势
			if(price.getShoupanjia().compareTo(price.getMa20Hour())<= -1) {
				downCount++;
				upCount=0;
				max=price.getMa20Hour();
			}
			//优化买入卖出位
			BigDecimal goodSell=max.multiply(new BigDecimal(0.85));
			BigDecimal goodBuy=min.multiply(new BigDecimal(1.015));
			BigDecimal avgM20=price.getMa20Hour().multiply(new BigDecimal(1.015));
			goodSell=goodSell.setScale(3, BigDecimal.ROUND_UP);
			goodBuy=goodBuy.setScale(3, BigDecimal.ROUND_UP);
			avgM20=avgM20.setScale(3, BigDecimal.ROUND_UP);
			//买入点且最低价出现在MA20均值上
			if(upCount==1 && price.getHeight().compareTo(avgM20)< 1) {
				String key=price.getHistoryAll();
				if(!date.contains(key)) {
					list.add(price);
					date.add(key);
				}
			}
			
			//卖出点且最低价出现在MA20均值上
			if(downCount==1 ) {
				String key=price.getHistoryAll();
				if(!date.contains(key)) {
					list.add(price);
					date.add(key);
				}
			}
			lastPrice=price;
		}
		String key=lastPrice.getHistoryAll();
		if(!date.contains(key)) {
			list.add(lastPrice);
			date.add(key);
		}
		return list;
	}


	@Override
	public List<HistoryStockDo> getLastHistoryStock(String number, Integer size) {
		List<HistoryStockDo> rsList=new  ArrayList<HistoryStockDo>();
		List<HistoryStockDo> list=historyStockMapper.getNumber(number);
		Collections.reverse(list);
		String temp="";
		for(HistoryStockDo stock:list) {
			if(rsList.size()>=size) {
				break;
			}
			if(!StringUtils.equalsAnyIgnoreCase(temp, stock.getRemark())) {
				temp=stock.getRemark();
				rsList.add(stock);
			}
		}
		Collections.reverse(rsList);
		return rsList;
	}


	@Override
	public void updateDayStockByThs() {
		logger.info(System.getProperty("java.library.path"));
		System.load("D:\\API\\bin\\x64\\iFinDJava_x64.dll");
		//JDIBridge.THS_iFinDLogin("wmg027", "644850");
		JDIBridge.THS_iFinDLogin("sjjk010", "273645");
		List<StockDo> list = getAllStock();
		for(int i=0;i<list.size();i++) {
			StockDo stock=list.get(i);
			String number = stock.getNumber();
			logger.info("updateDayStockByThs:"+number+"==>"+i+"/"+list.size());
			if(StringUtils.isBlank(number)) {
				return ;
			}
			if(number.contains("sz")) {
				updateDay(number.replace("sz", "")+".SZ"); 
			}
			if(number.contains("sh")) {
				updateDay(number.replace("sh", "")+".SH"); 
			}
		}
		JDIBridge.THS_iFinDLogout();
	}

	private void updateDay(String number) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		Calendar calendar=Calendar.getInstance();
		 
		String today=dateFormat.format(calendar.getTime());
		calendar.set(Calendar.DATE,-1);
		String yesterday=dateFormat.format(calendar.getTime());
		
		String strResulthis = JDIBridge.THS_HistoryQuotes(number,"close,avgPrice,open,low,high,volume","Interval:D,CPS:1,baseDate:1900-01-01,Currency:YSHB,fill:Previous",yesterday,today);
		logger.info("THS_iFinDhis ==> " + strResulthis);
		if(strResulthis == null) {
			return;
		}
		HistoryRsDate rs=JSON.parseObject(strResulthis,HistoryRsDate.class);
		if(rs.getTables().get(0).getTable().getClose() == null) {
			String myNumber="";
			
			if(number.contains(".SZ")) {
				myNumber="sz"+number.replace(".SZ", "");
			}
			if(number.contains(".SH")) {
				myNumber="sh"+number.replace(".SH", "");
			}
			StockDo obj=stockMapper.getNumber(myNumber);
			if(obj!=null) {
				stockMapper.delete(obj);
			}
			return ;
		}
		
		List<BigDecimal> closeList =rs.getTables().get(0).getTable().getClose();
		List<BigDecimal> avgPriceList = rs.getTables().get(0).getTable().getAvgPrice();
		List<BigDecimal> openList = rs.getTables().get(0).getTable().getOpen();
		List<BigDecimal> lowList = rs.getTables().get(0).getTable().getLow();
		List<BigDecimal> highList = rs.getTables().get(0).getTable().getHigh();
		List<Long> volumeList = rs.getTables().get(0).getTable().getVolume();
		
		List<String> times=rs.getTables().get(0).getTime();
		int total=times.size();
		for(int i=0;i<total;i++) {
			String time = times.get(i).replace("-", "");
			BigDecimal avg=avgPriceList.get(i).setScale(2,BigDecimal.ROUND_HALF_UP);
			BigDecimal open=openList.get(i).setScale(2,BigDecimal.ROUND_HALF_UP);
			BigDecimal close=closeList.get(i).setScale(2,BigDecimal.ROUND_HALF_UP);
			BigDecimal low=lowList.get(i).setScale(2,BigDecimal.ROUND_HALF_UP);
			BigDecimal high=highList.get(i).setScale(2,BigDecimal.ROUND_HALF_UP);
			Long volume = volumeList.get(i);
			HistoryDayStockDo obj =new HistoryDayStockDo();
			if(number.contains(".SZ")) {
				obj.setNumber("sz"+number.replace(".SZ", "")); 
			}
			if(number.contains(".SH")) {
				obj.setNumber("sh"+number.replace(".SH", "")); 
			}
			obj.setOpen(open);
			obj.setClose(close);
			obj.setAvg(avg);
			obj.setHigh(high);
			obj.setLow(low);
			obj.setHistoryDay(time);
			obj.setVolume(volume);
			if(historyDayStockMapper.getByTime(obj) == null) {
				System.out.println(obj.getNumber());
				historyDayStockMapper.insert(obj);
			}
			
		}
	}

}

package com.example.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import com.alibaba.fastjson.JSON;
import com.example.chart.base.entity.Candle;
import com.example.chart.base.entity.Entry;
import com.example.chart.entity.BollEntity;
import com.example.chart.entity.EMAEntity;
import com.example.chart.entity.MACDEntity;
import com.example.chart.entity.MAEntity;
import com.example.mapper.HistoryDayStockMapper;
import com.example.mapper.HistoryStockMapper;
import com.example.mapper.RiskStockMapper;
import com.example.mapper.RobotAccountMapper;
import com.example.mapper.RobotSetMapper;
import com.example.model.HistoryDayStockDo;
import com.example.model.HistoryPriceDo;
import com.example.model.MockLog;
import com.example.model.RealTimeDo;
import com.example.model.RiskStockDo;
import com.example.model.RobotAccountDo;
import com.example.model.RobotSetDo;
import com.example.model.StockPriceVo;
import com.example.model.TradingRecordDo;
import com.example.uitls.DateUtils;
import com.example.uitls.RedisKeyUtil;
import com.example.uitls.RedisUtil;

@Service
public class TrendStrategyServiceImpl implements TrendStrategyService {

	private static Logger logger = LoggerFactory.getLogger(TrendStrategyServiceImpl.class);

	@Resource
	private RedisUtil redisUtil;

	@Autowired
	private RobotAccountMapper robotAccountMapper;

	@Autowired
	private RobotSetMapper robotSetMapper;

	@Autowired
	private HistoryDayStockMapper historyDayStockMapper;
	
	@Autowired
	private RiskStockMapper riskStockMapper;

	@Autowired
	private HistoryStockMapper historyStockMapper;

	@Override
	public BarSeries transformBarSeriesByStockPrice(List<StockPriceVo> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		BarSeries series = new BaseBarSeries(list.get(0).getNumber());
		for (StockPriceVo stock : list) {
			try {
				Timestamp timestamp = new Timestamp(DateUtils.getDateForYYYYMMDDHHMM_NUMBER(stock.getHistoryAll()).getTime());
				ZonedDateTime date = ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
				double open = stock.getOpen().doubleValue();
				double high = stock.getHigh().doubleValue();
				double low = stock.getLow().doubleValue();
				double close = stock.getClose().doubleValue();
				double volume = stock.getVolume();
				series.addBar(date, open, high, low, close, volume);
			} catch (Exception e) {
				logger.error(JSON.toJSONString(stock)+":"+e.getMessage(),e);
			}
		}
		return series;
	}

	@Override
	public List<Candle> transformStockPrice(List<StockPriceVo> list) {
		List<Candle> rsList = new ArrayList<Candle>();
		for (StockPriceVo stock : list) {
			Candle c= new Candle(
					DateUtils.getDateForYYYYMMDDHHMM_NUMBER(stock.getHistoryAll()).getTime(),
					stock.getHigh().floatValue(),
					stock.getLow().floatValue(),
					stock.getOpen().floatValue(),
					stock.getClose().floatValue(),
					stock.getVolume()
					);
			rsList.add(c);
		}
		return rsList;
	}

	@Override
	public List<StockPriceVo> transformByDayLine(List<HistoryDayStockDo> list) {
		List<StockPriceVo> rsList = new ArrayList<StockPriceVo>();
		String name ="";
		if(!list.isEmpty()) {
			name = (String) redisUtil.get(RedisKeyUtil.getStockName(list.get(0).getNumber()));
		}
		for (HistoryDayStockDo dayStock : list) {
			if(dayStock.getVolume()==null || dayStock.getVolume()<=0) {
				continue;
			}
			StockPriceVo stock = new StockPriceVo();
			stock.setClose(dayStock.getClose());
			stock.setOpen(dayStock.getOpen());
			stock.setLow(dayStock.getLow());
			stock.setHigh(dayStock.getHigh());
			stock.setVolume(dayStock.getVolume());
			stock.setNumber(dayStock.getNumber());
			stock.setHistoryAll(dayStock.getHistoryDay() + "1500");
			stock.setHistoryDay(dayStock.getHistoryDay());
			stock.setName(name);
			rsList.add(stock);
		}
		return rsList;
	}

	@Override
	public List<StockPriceVo> transformByMinuteLine(List<HistoryPriceDo> list) {
		List<StockPriceVo> rsList = new ArrayList<StockPriceVo>();
		for (HistoryPriceDo minuteStock : list) {
			StockPriceVo stock = new StockPriceVo();
			stock.setClose(minuteStock.getShoupanjia());
			stock.setOpen(minuteStock.getKaipanjia());
			stock.setLow(minuteStock.getZuidijia());
			stock.setHigh(minuteStock.getZuigaojia());
			stock.setNumber(minuteStock.getNumber());
			stock.setMa20hour(minuteStock.getMa20());
			stock.setHistoryAll(DateUtils.getDateForYYYYMMDDHHMMByDate(minuteStock.getDateime()));
			stock.setHistoryDay(DateUtils.getDateForYYYYMMDDByDate(minuteStock.getDateime()));
			String name = (String) redisUtil.get(RedisKeyUtil.getStockName(minuteStock.getNumber()));
			stock.setName(name);
			rsList.add(stock);
		}
		return rsList;
	}

	@Override
	public List<StockPriceVo> transformByRealTime(List<RealTimeDo> list) {
		List<StockPriceVo> rsList = new ArrayList<StockPriceVo>();
		for (RealTimeDo rtStock : list) {
			StockPriceVo stock = new StockPriceVo();
			stock.setClose(new BigDecimal(rtStock.getKaipanjia()));
			stock.setOpen(new BigDecimal(rtStock.getDangqianjiage()));
			stock.setLow(new BigDecimal(rtStock.getLow()));
			stock.setHigh(new BigDecimal(rtStock.getTop()));
			stock.setNumber(rtStock.getNumber());
			stock.setHistoryAll(rtStock.getDate() + rtStock.getTime());
			stock.setHistoryDay(rtStock.getDate());
			String name = (String) redisUtil.get(RedisKeyUtil.getStockName(rtStock.getNumber()));
			stock.setName(name);
			rsList.add(stock);
		}
		return rsList;
	}

	@Override
	public MockLog transformByTradingRecord(List<TradingRecordDo> list) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TradingRecordDo> getStrategyByBand(List<StockPriceVo> list, RobotAccountDo account, RobotSetDo config) {
		List<TradingRecordDo> rsList = new ArrayList<TradingRecordDo>();

		for (StockPriceVo stock : list) {

		}
		return null;
	}

	/**
	 * 突破10天的上轨平均值，且macd是正数,站稳
	 */
	@Override
	public Boolean getStrategyByBox(List<StockPriceVo> list) {
		if(list.size()<26) {
			return false;
		}
		StockPriceVo last = list.get(list.size()-1);
		if(last.getClose().doubleValue()>40 || last.getClose().doubleValue()<10) {
			return false;
		}
		
		
		MACDEntity macdEntity=buildMacdEntry(list);
		for(int i=macdEntity.size-3;i<macdEntity.size;i++) {
			if(macdEntity.macd.get(i).getY()<0) {
				return false;
			}
		}
		
		
		BollEntity boll= buildBollEntry(list);
		double upAvg=0.0;
		int count=0;
		for(int i=boll.getUpList().size()-7;i<boll.getUpList().size()-1;i++) {
			upAvg+=boll.getUpList().get(i).getY();
			count++;
		}
		upAvg=upAvg/count;
		
		if(last.getClose().doubleValue()>=upAvg) {
			return true;
		}
		return false;
	}

	@Override
	public List<TradingRecordDo> getStrategyByRebound(List<StockPriceVo> list, RobotAccountDo account,
			RobotSetDo config) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<TradingRecordDo> getStrategyByMA(List<StockPriceVo> list, RobotAccountDo account, RobotSetDo config) {
		List<TradingRecordDo> rslist=new ArrayList<TradingRecordDo>();
		MAEntity ma = buildMaEntry(list);
	    
	    for(int i=0;i<list.size();i++) {
	    	StockPriceVo price=list.get(i);
	    	Entry maValue=ma.ma20.get(i);
	    	double buyPoint=maValue.getY()*0.97;
	    	double sellPoint=maValue.getY()*1.05;
	    	double stopLossPoint=maValue.getY()*0.95;
	    	
	    	if(price.getOpen().doubleValue() <= buyPoint && price.getOpen().doubleValue()>=stopLossPoint) {
	    		double sotck=account.getTotal().intValue() * 0.2/ (price.getOpen().intValue()*100);
	    		int num=(int)sotck*100;
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				config.getDtId(),
	    				price.getOpen(),
	    				num,
	    				TradingRecordDo.options_buy,
	    				"低于MA均线买入"
	    				);
	    		rslist.add(buyRecord);
	    	}
	    	if(price.getOpen().doubleValue() <= stopLossPoint) {
	    		double sotck=account.getTotal().intValue() * 0.2/ (price.getOpen().intValue()*100);
	    		int num=(int)sotck*100;
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				config.getDtId(),
	    				price.getOpen(),
	    				num,
	    				TradingRecordDo.options_sell,
	    				"跌破止损卖出"
	    				);
	    		rslist.add(buyRecord);
	    	}
	    	if(price.getOpen().doubleValue() >= sellPoint) {
	    		double sotck=account.getTotal().intValue() * 0.2/ (price.getOpen().intValue()*100);
	    		int num=(int)sotck*100;
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				config.getDtId(),
	    				price.getOpen(),
	    				num,
	    				TradingRecordDo.options_sell,
	    				"止盈卖出"
	    				);
	    		rslist.add(buyRecord);
	    	}
	    }
		return rslist;
	}
	
	@Override
	public List<TradingRecordDo> getStrategyByEMA(List<StockPriceVo> list ) {
		List<TradingRecordDo> rslist=new ArrayList<TradingRecordDo>();
		EMAEntity ema = buildEmaEntry(list);
	    if(ema==null) {
	    	return rslist;
	    }
	    for(int i=0;i<list.size();i++) {
	    	StockPriceVo price=list.get(i);
	    	//89
	    	Entry maValue1=ema.getEmaList1().get(i);
	    	//144
	    	Entry maValue2=ema.getEmaList2().get(i);
	    	double buyPoint=maValue1.getY();
	    	
	    	
	    	//趋势判断 10天必须比现在低
	    	if(maValue1.getY() > maValue2.getY()) {
	    		continue;
	    	}
	    	
	    	//开盘价在MA1之下，收盘价在MA1之上
	    	if(i>144 && price.getOpen().doubleValue() <= maValue1.getY() && price.getOpen().doubleValue() <= maValue2.getY() 
	    			&& price.getClose().doubleValue()>=maValue1.getY() && price.getClose().doubleValue() >= maValue2.getY() ) {
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				"test",
	    				price.getOpen(),
	    				100,
	    				TradingRecordDo.options_buy,
	    				"买入信号"+getLastPrice(list,i,10)
	    				);
	    		rslist.add(buyRecord);
	    	}
	    }
		return rslist;
	}

	private String  getLastPrice(List<StockPriceVo> list, int i, int j) {
		int jump=list.size()-1;
		if(list.size()>=i+j) {
			jump=i+j-1;
		}
		String rs=" 策略失败";
		BigDecimal top=list.get(jump).getClose();
		for(int l=i+1;l<=jump;l++) {
			if(top.doubleValue()<list.get(l).getClose().doubleValue()) {
				top=list.get(l).getClose();
			}
		}
		if( top.doubleValue() > list.get(i).getClose().doubleValue()) {
			rs=" 策略成功";
		}
		
		if( top.doubleValue() == list.get(i).getClose().doubleValue()) {
			rs=" 持股观察";
		}
		return  "现价："+list.get(i).getClose()+" "+"未来最高的收盘价："+top+" "+ rs;
	}

	@Override
	public EMAEntity buildEmaEntry(List<StockPriceVo> list) {
		if(list.isEmpty()||list.size()<144) {
			return null;
		}
		BarSeries series = transformBarSeriesByStockPrice(list);
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		EMAIndicator avg1 = new EMAIndicator(closePrice, 89);
		EMAIndicator avg2 = new EMAIndicator(closePrice, 144);
	    List<Entry> maList1 =new  ArrayList<Entry>();
	    List<Entry> maList2 =new  ArrayList<Entry>();
	    for(int i=0;i<list.size();i++) {
	    	Entry entry1=new Entry();
	    	entry1.setX(list.get(i).getHistoryAll());
	    	entry1.setY(avg1.getValue(i).doubleValue());
	    	entry1.setData(list.get(i));
	    	maList1.add(entry1);
	    	
	    	Entry entry2=new Entry();
	    	entry2.setX(list.get(i).getHistoryAll());
	    	entry2.setY(avg2.getValue(i).doubleValue());
	    	entry2.setData(list.get(i));
	    	maList2.add(entry2);
	    }
	   
	    return new EMAEntity(maList1,maList2);
	}
	
	
	@Override
	public MAEntity buildMaEntry(List<StockPriceVo> list) {
		BarSeries series = transformBarSeriesByStockPrice(list);
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		SMAIndicator avg5 = new SMAIndicator(closePrice, 5);
	    SMAIndicator avg20 = new SMAIndicator(closePrice, 20);
	    SMAIndicator avg200 = new SMAIndicator(closePrice, 200);
	    List<Entry> ma5List =new  ArrayList<Entry>();
	    List<Entry> ma20List =new  ArrayList<Entry>();
	    List<Entry> ma200List =new  ArrayList<Entry>();
	    for(int i=0;i<list.size();i++) {
	    	Entry entry5=new Entry();
	    	entry5.setX(list.get(i).getHistoryAll());
	    	entry5.setY(avg5.getValue(i).doubleValue());
	    	entry5.setData(list.get(i));
	    	ma5List.add(entry5);
	    	
	    	Entry entry20=new Entry();
	    	entry20.setX(list.get(i).getHistoryAll());
	    	entry20.setY(avg20.getValue(i).doubleValue());
	    	entry20.setData(list.get(i));
	    	ma20List.add(entry20);
	    	
	    	Entry entry200=new Entry();
	    	entry200.setX(list.get(i).getHistoryAll());
	    	entry200.setY(avg200.getValue(i).doubleValue());
	    	entry200.setData(list.get(i));
	    	ma200List.add(entry200);
	    }
	    return new MAEntity(ma5List,ma20List,ma200List);
	}

	@Override
	public List<TradingRecordDo> getStrateByBoll(List<StockPriceVo> list, RobotAccountDo account, RobotSetDo config) {
		List<TradingRecordDo> rslist=new ArrayList<TradingRecordDo>();
		BollEntity boll= buildBollEntry(list);
		for(int i=20;i<list.size();i++) {
	    	StockPriceVo price=list.get(i);
	    	Entry upValue=boll.getUpList().get(i);
	    	Entry midValue=boll.getMidList().get(i);
	    	Entry lowerValue=boll.getLowerList().get(i);
	    	boolean isSell=false;
	    	boolean isBuy=false;
	    	double buyPoint=midValue.getY()*0.97;
	    	double stopLossPoint=midValue.getY()*0.95;
	    	double sellPoint=upValue.getY();
	    	if(price.getOpen().doubleValue()==0) {
	    		price.setOpen(price.getClose());
	    	}
	    	
	    	double avg=(price.getOpen().doubleValue()+price.getClose().doubleValue())/2;
	    	int numList=0;
	    	for(TradingRecordDo rc:rslist) {
	    		if(rc.getOptions()==TradingRecordDo.options_buy) {
	    			isSell=true;
	    			numList+=rc.getNum();
	    		}
	    		if(rc.getOptions()==TradingRecordDo.options_sell) {
	    			isSell=false;
	    			numList-=rc.getNum();
	    		}
	    	}
	    	if(isSell && avg >= sellPoint) {
	    		double total=account.getTotal().doubleValue()+numList*avg;
	    		account.setTotal(new BigDecimal(total));
	    		
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				config.getDtId(),
	    				price.getOpen(),
	    				numList,
	    				TradingRecordDo.options_sell,
	    				"\n操作建议：分批止盈"
	    				+"\n买入点："+new BigDecimal(buyPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(midValue.getY()*0.99).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止损点："+new BigDecimal(stopLossPoint).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止盈点："+new BigDecimal(sellPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(upValue.getY()*1.02).setScale(2,BigDecimal.ROUND_DOWN)
	    				);
	    		rslist.add(buyRecord);
	    		continue;
	    	}
	    	if(isSell && avg <= stopLossPoint) {
	    		double total=account.getTotal().doubleValue()+numList*avg;
	    		account.setTotal(new BigDecimal(total));
	    		
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				config.getDtId(),
	    				price.getOpen(),
	    				numList,
	    				TradingRecordDo.options_sell,
	    				"\n操作建议：分批止损"
	    				+"\n买入点："+new BigDecimal(buyPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(midValue.getY()*0.99).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止损点："+new BigDecimal(stopLossPoint).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止盈点："+new BigDecimal(sellPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(upValue.getY()*1.02).setScale(2,BigDecimal.ROUND_DOWN)
	    				);
	    		rslist.add(buyRecord);
	    		continue;
	    	}
	    	
	    	//趋势向上
	    	if(boll.getMidList().get(i-10).getY() < boll.getMidList().get(i).getY()*1.0065) {
	    		isBuy=true;
	    	} 
	    	//开盘与收盘价格需要覆盖中轨低1% 价格要覆盖
	    	if(isBuy && price.getLow().doubleValue() <= buyPoint &&  price.getLow().doubleValue() >= stopLossPoint) {
	    		isBuy=true;
	    	}else {
	    		isBuy=false;
	    	}
	    	
	    	if(isBuy) {
				double sotck = account.getTotal().intValue() * 0.25 / (avg * 100);
				int num = (int) (sotck - 1) * 100;
	    		if(num <=0) {
	    			continue;
	    		}
	    		double total=account.getTotal().doubleValue()-num*avg;
	    		account.setTotal(new BigDecimal(total));
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				config.getDtId(),
	    				new BigDecimal(avg),
	    				num,
	    				TradingRecordDo.options_buy,
	    				"\n操作建议：分批买入"
	    				+"\n买入点："+new BigDecimal(buyPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(midValue.getY()*0.99).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止损点："+new BigDecimal(stopLossPoint).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止盈点："+new BigDecimal(sellPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(upValue.getY()*1.02).setScale(2,BigDecimal.ROUND_DOWN)
	    				);
	    		
	    		rslist.add(buyRecord);
	    	}else {
	    		String remark="\n操作建议：空仓观望";
	    		if(isSell) {
	    			remark="\n操作建议：持股待涨";
				}
	    		TradingRecordDo buyRecord=new TradingRecordDo(
	    				DateUtils.getDateForYYYYMMDDHHMM_NUMBER(price.getHistoryAll()),
	    				price.getNumber(),
	    				price.getName(),
	    				config.getDtId(),
	    				new BigDecimal(avg),
	    				0,
	    				TradingRecordDo.options_nothink,
	    				remark+"\n买入点："+new BigDecimal(buyPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(midValue.getY()*0.99).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止损点："+new BigDecimal(stopLossPoint).setScale(2,BigDecimal.ROUND_DOWN)
	    				+"\n止盈点："+new BigDecimal(sellPoint).setScale(2,BigDecimal.ROUND_DOWN)+"-"+new BigDecimal(upValue.getY()*1.02).setScale(2,BigDecimal.ROUND_DOWN)
	    				);
	    		rslist.add(buyRecord);
	    	}
	    }
		return rslist;
	}

	public MACDEntity buildMacdEntry(List<StockPriceVo> spList) {
		BarSeries series =transformBarSeriesByStockPrice(spList);
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		MACDIndicator macd = new MACDIndicator(closePrice, 9, 26);
		EMAIndicator ema1 = new EMAIndicator(closePrice, 12);
		EMAIndicator ema2 = new EMAIndicator(closePrice, 26);
		EMAIndicator ema3 = new EMAIndicator(closePrice, 9);
		MACDEntity macdEntiry=new MACDEntity();
		List<Entry> macdList=new ArrayList<Entry>();
	    List<Entry> diffList=new ArrayList<Entry>();
	    List<Entry> deaList=new ArrayList<Entry>();
		
		for(int i=0;i<spList.size();i++) {
			double macdValue=macd.getValue(i).doubleValue();
			double dif=ema1.getValue(i).minus(ema2.getValue(i)).doubleValue();
			double dem=ema3.getValue(i).doubleValue();
			macdEntiry.macd.add(new Entry(spList.get(i).getHistoryDay(),macdValue));
			macdEntiry.diff.add(new Entry(spList.get(i).getHistoryDay(),dif));
			macdEntiry.dea.add(new Entry(spList.get(i).getHistoryDay(),dem));
		}
		macdEntiry.size=spList.size();
		return macdEntiry;
	}
	
	@Override
	public BollEntity buildBollEntry(List<StockPriceVo> list) {
		BollEntity boll;
		BarSeries series = transformBarSeriesByStockPrice(list);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator avg = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator sd20 = new StandardDeviationIndicator(closePrice, 20);

        // Bollinger bands
        BollingerBandsMiddleIndicator middleBBand = new BollingerBandsMiddleIndicator(avg);
        BollingerBandsLowerIndicator lowBBand = new BollingerBandsLowerIndicator(middleBBand, sd20);
        BollingerBandsUpperIndicator upBBand = new BollingerBandsUpperIndicator(middleBBand, sd20);
        //存储上轨数据
        List<Entry> upList=new ArrayList<Entry>();
        //存储中轨数据
        List<Entry> midList=new ArrayList<Entry>();
        //存储下轨数据
        List<Entry> lowerList=new ArrayList<Entry>();
        
        for(int i=0;i<list.size();i++) {
        	Entry up=new Entry();
        	up.setX(list.get(i).getHistoryAll());
        	up.setY(upBBand.getValue(i).doubleValue());
        	up.setData(list.get(i));
        	upList.add(up);
        	
        	Entry mid=new Entry();
        	mid.setX(list.get(i).getHistoryAll());
        	mid.setY(middleBBand.getValue(i).doubleValue());
        	mid.setData(list.get(i));
        	midList.add(mid);
        	
        	Entry lower=new Entry();
        	lower.setX(list.get(i).getHistoryAll());
        	lower.setY(lowBBand.getValue(i).doubleValue());
        	lower.setData(list.get(i));
        	lowerList.add(lower);
        }
        return new BollEntity(upList, midList, lowerList);
	}

	@Override
	public void reRisk(String number) {
		try {
			List<StockPriceVo> spList=transformByDayLine(historyDayStockMapper.getNumber(number));
			BollEntity boll= buildBollEntry(spList);
			Entry up=boll.getUpList().get(boll.getUpList().size()-1);
			Entry mid=boll.getMidList().get(boll.getMidList().size()-1);
			Entry lower=boll.getLowerList().get(boll.getLowerList().size()-1);
			EMAEntity ema= buildEmaEntry(spList);
			MAEntity ma= buildMaEntry(spList);
			
			RiskStockDo obj=new RiskStockDo();
			setBoll(obj,up,mid,lower);
			setEma(obj,ema);
			setMa(obj,ma);
			
			obj.setName(spList.get(0).getName());
			obj.setNumber(number);
			obj.setStatus(1);
			obj.setOpen(spList.get(spList.size()-1).getOpen().doubleValue());
			obj.setClose(spList.get(spList.size()-1).getClose().doubleValue());
			
			obj.setTop5volume(spList.get(spList.size()-1).getVolume());
			obj.setUpdateTime(new Date());
			if(riskStockMapper.getNumber(number) != null) {
				riskStockMapper.delete(obj);
			}
			riskStockMapper.insert(obj);
			System.out.println(number);
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	private void setMa(RiskStockDo obj,MAEntity ma) {
		if(ma ==null || ma.ma200==null ||ma.ma200.size()<=0) {
			return;
		}
		obj.setMa5(ma.ma5.get(ma.ma5.size()-1).getY());
		obj.setMa20(ma.ma20.get(ma.ma20.size()-1).getY());
		obj.setMa200(ma.ma200.get(ma.ma200.size()-1).getY());
	}

	private void setEma(RiskStockDo obj,EMAEntity ema) {
		if(ema ==null || ema.getEmaList2()==null ||ema.getEmaList2().size()<=0) {
			return;
		}
		obj.setEma144(ema.getEmaList1().get(ema.getEmaList1().size()-1).getY());
		obj.setEma89(ema.getEmaList2().get(ema.getEmaList2().size()-1).getY());
	}

	private void setBoll(RiskStockDo obj,Entry up,Entry mid,Entry lower) {
		if(up.getY() <=0 || mid.getY()<=0 ||lower.getY()<=0) {
			return;
		}
		obj.setBollDayUp(up.getY());
		obj.setBollDayMid(mid.getY());
		obj.setBollDayLower(lower.getY());
		obj.setBuyPointBegin(mid.getY()*1.01);
		obj.setBuyPointEnd(mid.getY()*0.99);
		obj.setStopLoss(lower.getY()*1.01);
		obj.setStopProfit(up.getY()*0.99);
	}

	@Override
	public List<RiskStockDo> getTodayList() {
		try {
			return riskStockMapper.getTodayList();
		}catch (Exception e) {
			return null;
		}
	}

	@Override
	public RiskStockDo getRiskStock(String number) {
		try {
			return riskStockMapper.getNumber(number);
		}catch (Exception e) {
			return null;
		}
	}

}

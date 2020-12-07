package com.example.demo;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.example.chart.base.entity.Entry;
import com.example.chart.entity.BollEntity;
import com.example.chart.entity.EMAEntity;
import com.example.chart.entity.MAEntity;
import com.example.mapper.HistoryDayStockMapper;
import com.example.mapper.RiskStockMapper;
import com.example.model.RiskStockDo;
import com.example.model.RobotAccountDo;
import com.example.model.RobotSetDo;
import com.example.model.StockDo;
import com.example.model.StockPriceVo;
import com.example.model.TradingRecordDo;
import com.example.service.GuPiaoService;
import com.example.service.TrendStrategyService;
import com.example.service.task.DataTask;
import com.example.uitls.ReadApiUrl;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { GupiaoApplication.class })
public class StrategyTest {
	@Autowired
	private TrendStrategyService trendStrategyService;
	
	@Autowired
	private HistoryDayStockMapper historyDayStockMapper;
	
	@Autowired
	private ReadApiUrl readApiUrl;
	
	//@Autowired
	private GuPiaoService guPiaoService;
	
	//@Autowired
	private DataTask dataTask;
	
	@Autowired
	private RiskStockMapper riskStockMapper;
	
	private static String appSecret = "bb888ac7199ba68c327c8a0e44fbf0ee6b65b5b0f490beb39a209a295e132a4f";
	private String number="sz002030";
	private static final SimpleDateFormat DF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
	
	
	@Test
	public void TestRiskStockMapper() {
		try {
			List<StockPriceVo> spList=trendStrategyService.transformByDayLine(historyDayStockMapper.getNumber(number));
			BollEntity boll= trendStrategyService.buildBollEntry(spList);
			Entry up=boll.getUpList().get(boll.getUpList().size()-1);
			Entry mid=boll.getMidList().get(boll.getMidList().size()-1);
			Entry lower=boll.getLowerList().get(boll.getLowerList().size()-1);
			EMAEntity ema= trendStrategyService.buildEmaEntry(spList);
			MAEntity ma= trendStrategyService.buildMaEntry(spList);
			
			RiskStockDo obj=new RiskStockDo();
			setBoll(obj,up,mid,lower);
			setEma(obj,ema);
			setMa(obj,ma);
			
			obj.setName(spList.get(0).getName());
			obj.setNumber(number);
			obj.setStatus(1);
			
			long total=0;
			for(int i=spList.size()-6;i<spList.size()-1;i++) {
				total+=spList.get(i).getVolume();
			}
			obj.setTop5volume(total/5);
			obj.setUpdateTime(new Date());
			if(riskStockMapper.getNumber(number) != null) {
				riskStockMapper.delete(obj);
			}
			riskStockMapper.insert(obj);
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
	
	//@Test
	public void TestStrategyByEMa() {
		List<StockDo> list1= new ArrayList<StockDo>();
		List<StockDo> list=guPiaoService.getAllStock();
		int k=list.size()/5;
		list1=list.subList(0, k);
		for(StockDo sk:list1) {
			List<StockPriceVo> spList=trendStrategyService.transformByDayLine(historyDayStockMapper.getNumber(sk.getNumber()));
			RobotAccountDo account=new RobotAccountDo();
			RobotSetDo config=new RobotSetDo();
			account.setTotal(new BigDecimal(100000));
			List<TradingRecordDo> rtList=trendStrategyService.getStrategyByEMA(spList, account, config);
			for(TradingRecordDo rt:rtList) {
				BigDecimal total =rt.getTotal().add(account.getTotal());
				total=total.setScale(2, BigDecimal.ROUND_UP);
				System.out.println(DF_YYYY_MM_DD.format(rt.getCreateDate())+" "+rt.getNumber()+" 当天均价："+rt.getPrice()+" "+rt.getRemark());
			}
		}
		
	}
	
	
	//@Test
	public void TestStrateByBoll() {
		
		List<StockPriceVo> spList=trendStrategyService.transformByDayLine(historyDayStockMapper.getNumber(number));
		RobotAccountDo account=new RobotAccountDo();
		RobotSetDo config=new RobotSetDo();
		account.setTotal(new BigDecimal(100000));
		List<TradingRecordDo> rtList=trendStrategyService.getStrateByBoll(spList, account, config);
		for(TradingRecordDo rt:rtList) {
			BigDecimal total =rt.getTotal().add(account.getTotal());
			total=total.setScale(2, BigDecimal.ROUND_UP);
			System.out.println(DF_YYYY_MM_DD.format(rt.getCreateDate())+" "+rt.getNumber()+" 当天均价："+rt.getPrice()+" "+rt.getRemark());
		}
	}
	
	
	//@Test
	public void excuteRunListenTest() {
		guPiaoService.updateHistoryStock("sh600305");
		guPiaoService.timeInterval("sh600305");
		dataTask.excuteRunListen("sh600305",appSecret,"20201115");
		while(true) {
			try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

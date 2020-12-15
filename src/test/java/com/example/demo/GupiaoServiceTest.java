package com.example.demo;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.alibaba.fastjson.JSON;
import com.example.ai.MockDeal;
import com.example.mapper.HistoryDayStockMapper;
import com.example.mapper.HistoryStockMapper;
import com.example.mapper.RiskStockMapper;
import com.example.mapper.RobotAccountMapper;
import com.example.mapper.RobotSetMapper;
import com.example.mapper.TradingRecordMapper;
import com.example.model.HistoryDayStockDo;
import com.example.model.HistoryStockDo;
import com.example.model.RiskStockDo;
import com.example.model.RobotAccountDo;
import com.example.model.RobotSetDo;
import com.example.model.TradingRecordDo;
import com.example.model.ths.HistoryRsDate;
import com.example.service.GuPiaoService;
import com.example.service.task.DataTask;
import com.example.service.task.MonitorTask;
import com.example.uitls.ReadApiUrl;
import com.example.uitls.RedisUtil;

import Ths.JDIBridge;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { GupiaoApplication.class })
public class GupiaoServiceTest {
	private static String appSecret = "bb888ac7199ba68c327c8a0e44fbf0ee6b65b5b0f490beb39a209a295e132a4f";

	@Autowired
	private RobotSetMapper robotSetMapper;
	@Autowired
	private RobotAccountMapper robotAccountMapper;
	@Autowired
	private TradingRecordMapper tradingRecordMapper;
	
	@Autowired
	private HistoryStockMapper historyStockMapper;
	@Autowired
	private GuPiaoService guPiaoService;
	@Autowired
	private MockDeal mockDeal;

	@Resource
	private RedisUtil redisUtil;
	@Autowired
	private ReadApiUrl readApiUrl;
	@Autowired
	private MonitorTask monitorTask;
	
	@Autowired
	private HistoryDayStockMapper historyDayStockMapper;
	

	
	private String number="sh600305";
	
	@Autowired
	private DataTask dataTask;
	

	
	
	
	//@Test
	public void AiBuyIn() {
		dataTask.EmaGupiao();
	}
	
	//@Test
	public void dataTask() {
		try {
			dataTask.afterPropertiesSet();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//@Test
	public void boduan() {
		
		
		List<HistoryStockDo> list = guPiaoService.getLastHistoryStock(number,3);
		for(HistoryStockDo stock:list) {
			System.out.println(stock.getBoxMax()+"  "+stock.getBoxMin()+ " "+ stock.getRemark());
		}
	}
	
	//@Test
	public void MockAi() {
		mockDeal.mockDeal("sz002202", "2020-11-01", appSecret, true);
	}
	
	//@Test
	public void updateDayStock() {
		updateDay(number.replace("sh", "")+".SH"); 
	}
	
	//@Test
	public void TestDay() {
		guPiaoService.updateDayStockByThs();
	}
	
	
	public void updateDay(String number) {
		final SimpleDateFormat DF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		
		System.out.println(System.getProperty("java.library.path"));
		System.load("D:\\API\\bin\\x64\\iFinDJava_x64.dll");
		//JDIBridge.THS_iFinDLogin("wmg027", "644850");
		JDIBridge.THS_iFinDLogin("sjjk010", "273645");
		String strResulthis = JDIBridge.THS_HistoryQuotes(number,"close,avgPrice,open,low,high,volume","Interval:D,CPS:1,baseDate:1900-01-01,Currency:YSHB,fill:Previous","2020-01-01",DF_YYYY_MM_DD.format(new Date()));
		System.out.println("THS_iFinDhis ==> " + strResulthis);
		HistoryRsDate rs=JSON.parseObject(strResulthis,HistoryRsDate.class);
		if(rs.getTables().get(0).getTable().getClose() == null) {
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
		JDIBridge.THS_iFinDLogout();
	}
	
	//@Test
	public void readUrl() {
		readApiUrl.readHistoryApiUrl(number, 60);
	}
	
	
	//@Test
	public void mock() throws Exception {
		guPiaoService.getLastZhichengwei(number);
	}
	
	
	// @Test
	public void ramTest() {
		redisUtil.set("test", "34323423");
		System.out.println(redisUtil.get("test"));
	}

	//@Test
	public void mockDeal() {
		mockDeal.mockDeal("sz399006", "2020-07-15", appSecret, true);
	}

	
	// @Test
	public void updateHistory() {
		guPiaoService.updateHistoryStock("sh601702");
	}

	// @Test
	public void addHistory() {
		HistoryStockDo tr = new HistoryStockDo();
		tr.setKaipanjia(new BigDecimal("10.223"));
		tr.setMa20Day(new BigDecimal("10.223"));
		tr.setMa20Hour(new BigDecimal("10.223"));
		tr.setShoupanjia(new BigDecimal("10.223"));
		tr.setHistoryDay("2323");
		tr.setNumber("3434");
		historyStockMapper.insert(tr);
	}

	// @Test
	public void delHistory() {
		for (HistoryStockDo rs : historyStockMapper.getAll()) {
			System.out.println(JSON.toJSONString(rs));
			historyStockMapper.delete(rs.getId());
		}
		;
	}

	// @Test
	public void addTradingRecord() {
		TradingRecordDo tr = new TradingRecordDo();
		tr.setCreateDate(new Date());
		tr.setDtId("3f3f33f3f");
		tr.setName("nadfdf");
		tr.setNum(323423);
		tr.setNumber("sdc2v2323");
		tr.setOptions(2);
		tr.setPrice(new BigDecimal("10.223"));
		tr.setTotal(new BigDecimal("10.343223"));
		tr.setRemark("cdcdc");
		tradingRecordMapper.insert(tr);
	}

	// @Test
	public void delTradingRecord() {
		for (TradingRecordDo tr : tradingRecordMapper.getAll()) {
			System.out.println(JSON.toJSONString(tr));
			tradingRecordMapper.delete(tr.getId());
		}
		;
	}

	// @Test
	public void addRobotAccount() {
		RobotAccountDo ra = new RobotAccountDo();
		ra.setRobotId(342343234L);
		ra.setTotal(new BigDecimal("10.223"));
		robotAccountMapper.insert(ra);
	}

	// @Test
	public void delRobotAccount() {
		for (RobotAccountDo ra : robotAccountMapper.getAll()) {
			System.out.println(JSON.toJSONString(ra));
			robotAccountMapper.delete(ra.getId());
		}
		;
	}

	// @Test
	public void addRobotSet() {
		RobotSetDo rs = new RobotSetDo();
		rs.setBeginTime(new Date());
		rs.setEndTime(new Date());
		rs.setDtId("3333");
		rs.setExecutionSale(true);
		rs.setName("name");
		rs.setNumber("number");
		rs.setRobotName("testname");
		rs.setStopLossesPrice(new BigDecimal("10.223"));
		robotSetMapper.insert(rs);
	}

	// @Test
	public void delRobotSet() {
		for (RobotSetDo rs : robotSetMapper.getAll()) {
			System.out.println(JSON.toJSONString(rs));
			robotSetMapper.delete(rs.getId());
		}
	}

}

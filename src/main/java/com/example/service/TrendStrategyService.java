package com.example.service;

import java.util.List;

import org.ta4j.core.BarSeries;

import com.example.chart.base.entity.Candle;
import com.example.chart.entity.BollEntity;
import com.example.chart.entity.EMAEntity;
import com.example.chart.entity.MAEntity;
import com.example.model.HistoryDayStockDo;
import com.example.model.HistoryPriceDo;
import com.example.model.MockLog;
import com.example.model.RealTimeDo;
import com.example.model.RiskStockDo;
import com.example.model.RobotAccountDo;
import com.example.model.RobotSetDo;
import com.example.model.StockPriceVo;
import com.example.model.TradingRecordDo;

public interface TrendStrategyService {

	/**
	 * 日线转换
	 * @param list
	 * @return
	 */
	List<StockPriceVo> transformByDayLine(List<HistoryDayStockDo> list);
	
	/**
	 * 分钟线转换
	 * @param list
	 * @return
	 */
	List<StockPriceVo> transformByMinuteLine(List<HistoryPriceDo> list);
	
	/**
	 * 实时转换
	 * @param list
	 * @return
	 */
	List<StockPriceVo> transformByRealTime(List<RealTimeDo> list);
	
	/**
	 * 指标转换
	 * @param list
	 * @return
	 */
	List<Candle> transformStockPrice(List<StockPriceVo> list);
	
	/**
	 * 指标转换
	 * @param list
	 * @return
	 */
	BarSeries transformBarSeriesByStockPrice(List<StockPriceVo> list);
	
	
	/**
	 * 转换到模拟结果
	 * @param list
	 * @return
	 */
	MockLog transformByTradingRecord(List<TradingRecordDo> list);
	
	/**
	 * 波段策略（60分钟，日线）
	 * @param list
	 * @param account 资金
	 * @param config  参数
	 * @return
	 */
	List<TradingRecordDo> getStrategyByBand(List<StockPriceVo> list, RobotAccountDo account,RobotSetDo config);
	
	/**
	 * 箱体操作（60分钟，日线）
	 * @param list
	 * @param account 资金
	 * @param config  参数
	 * @return
	 */
	Boolean getStrategyByBox(List<StockPriceVo> list);
	
	/**
	 * 反弹（60分钟，日线）
	 * @param list
	 * @param account 资金
	 * @param config  参数
	 * @return
	 */
	List<TradingRecordDo> getStrategyByRebound(List<StockPriceVo> list,RobotAccountDo account,RobotSetDo config);
	
	/**
	 * 移动平均线
	 * @param list
	 * @param account 资金
	 * @param config  参数
	 * @return
	 */
	List<TradingRecordDo> getStrategyByMA(List<StockPriceVo> list,RobotAccountDo account,RobotSetDo config);
	
	/**
	 * EMA买入点
	 * @param list
	 * @param account 资金
	 * @param config  参数
	 * @return
	 */
	List<TradingRecordDo> getStrategyByEMA(List<StockPriceVo> list);
	
	
	/**
	 * 布林线
	 * @param list
	 * @param account
	 * @param config
	 * @return
	 */
	List<TradingRecordDo> getStrateByBoll(List<StockPriceVo> list,RobotAccountDo account,RobotSetDo config);
	
	BollEntity buildBollEntry(List<StockPriceVo> list);
	
	EMAEntity buildEmaEntry(List<StockPriceVo> list);
	
	MAEntity buildMaEntry(List<StockPriceVo> list);
	
	void reRisk(String number);
	
	List<RiskStockDo> getTodayList();
	
	RiskStockDo getRiskStock(String number);
}

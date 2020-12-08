package com.example.mapper;

import java.util.List;

import com.example.model.RiskStockDo;

public interface RiskStockMapper {
	
	int insert(RiskStockDo obj);

	int delete(RiskStockDo obj);
	
	RiskStockDo getNumber(String string);
	
	List<RiskStockDo> getTodayList();
}

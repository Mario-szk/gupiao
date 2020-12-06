package com.example.mapper;

import com.example.model.RiskStockDo;

public interface RiskStockMapper {
	
	int insert(RiskStockDo obj);

	int delete(RiskStockDo obj);
	
	RiskStockDo getNumber(String string);
}

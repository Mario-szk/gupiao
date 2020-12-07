package com.example.model;

import java.io.Serializable;
import java.util.Date;

public class RiskStockDo implements Serializable {

	private static final long serialVersionUID = -7331163695847362878L;

	private Long id;
	private Date updateTime;
	private String number;
	private String name;
	private Double ma5;
	private Double ma20;
	private Double ma200;
	private Double ema89;
	private Double ema144;
	private Double bollDayUp;
	private Double bollDayMid;
	private Double bollDayLower;
	private Long top5volume;
	private Double stopProfit;
	private Double stopLoss;
	private Double buyPointBegin;
	private Double buyPointEnd;
	private Double open;
	private Double close;
	private Integer status;
	
	public Double getOpen() {
		return open;
	}

	public void setOpen(Double open) {
		this.open = open;
	}

	public Double getClose() {
		return close;
	}

	public void setClose(Double close) {
		this.close = close;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getMa5() {
		return ma5;
	}

	public void setMa5(Double ma5) {
		this.ma5 = ma5;
	}

	public Double getMa20() {
		return ma20;
	}

	public void setMa20(Double ma20) {
		this.ma20 = ma20;
	}

	public Double getMa200() {
		return ma200;
	}

	public void setMa200(Double ma200) {
		this.ma200 = ma200;
	}

	public Double getEma89() {
		return ema89;
	}

	public void setEma89(Double ema89) {
		this.ema89 = ema89;
	}

	public Double getEma144() {
		return ema144;
	}

	public void setEma144(Double ema144) {
		this.ema144 = ema144;
	}

	public Double getBollDayUp() {
		return bollDayUp;
	}

	public void setBollDayUp(Double bollDayUp) {
		this.bollDayUp = bollDayUp;
	}

	public Double getBollDayMid() {
		return bollDayMid;
	}

	public void setBollDayMid(Double bollDayMid) {
		this.bollDayMid = bollDayMid;
	}

	public Double getBollDayLower() {
		return bollDayLower;
	}

	public void setBollDayLower(Double bollDayLower) {
		this.bollDayLower = bollDayLower;
	}

	public Long getTop5volume() {
		return top5volume;
	}

	public void setTop5volume(Long top5volume) {
		this.top5volume = top5volume;
	}

	public Double getStopProfit() {
		return stopProfit;
	}

	public void setStopProfit(Double stopProfit) {
		this.stopProfit = stopProfit;
	}

	public Double getStopLoss() {
		return stopLoss;
	}

	public void setStopLoss(Double stopLoss) {
		this.stopLoss = stopLoss;
	}

	public Double getBuyPointBegin() {
		return buyPointBegin;
	}

	public void setBuyPointBegin(Double buyPointBegin) {
		this.buyPointBegin = buyPointBegin;
	}

	public Double getBuyPointEnd() {
		return buyPointEnd;
	}

	public void setBuyPointEnd(Double buyPointEnd) {
		this.buyPointEnd = buyPointEnd;
	}

}

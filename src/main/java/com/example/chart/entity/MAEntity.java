package com.example.chart.entity;


import java.util.List;

import com.example.chart.base.entity.Entry;

/**
 * Created by Rex on 2018/11/14.
 */
public class MAEntity implements ChartEntity {
	public List<Entry> ma5;
    public List<Entry> ma20;
    public List<Entry> ma200;

    public MAEntity(List<Entry> ma5,List<Entry> ma20,List<Entry> ma200) {
    	this.ma5=ma5;
    	this.ma20=ma20;
    	this.ma200=ma200;
    }
    
	public List<Entry> getMa5() {
		return ma5;
	}


	public void setMa5(List<Entry> ma5) {
		this.ma5 = ma5;
	}


	public List<Entry> getMa20() {
		return ma20;
	}


	public void setMa20(List<Entry> ma20) {
		this.ma20 = ma20;
	}


	public List<Entry> getMa200() {
		return ma200;
	}


	public void setMa200(List<Entry> ma200) {
		this.ma200 = ma200;
	}


	@Override
    public void clearValues() {
        if (ma5 != null) {
            ma5.clear();
        }
        if (ma20 != null) {
            ma20.clear();
        }
        if (ma200 != null) {
            ma200.clear();
        }
    }


}

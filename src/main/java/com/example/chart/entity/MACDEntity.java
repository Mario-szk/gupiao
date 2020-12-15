package com.example.chart.entity;


import java.util.ArrayList;
import java.util.List;

import com.example.chart.base.entity.BarEntry;
import com.example.chart.base.entity.Entry;

public class MACDEntity implements ChartEntity {
    public List<Entry> macd;
    public List<Entry> diff;
    public List<Entry> dea;
    public String indexDes;
    public int size;
    public MACDEntity() {
        this.macd = new ArrayList<>();
        this.diff = new ArrayList<>();
        this.dea = new ArrayList<>();
    }

    @Override
    public void clearValues() {
        if (macd != null) {
        	macd.clear();
        }
        if (diff != null) {
            diff.clear();
        }
        if (dea != null) {
            dea.clear();
        }
    }
}

package com.example.demo;

import java.io.Serializable;

public class GuPiao implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7289460169416455501L;
	public GuPiao() {
		super();
	}
	
	public GuPiao(String number, String name, String kaipanjia, String zuorishoupanjia, String dangqianjiage,
			String top, String low, String jbuy1, String jsell1, String chengjiaogupiao, String chengjiaojine,
			String buy1number, String buy1, String buy2number, String buy2, String buy3number, String buy3,
			String buy4number, String buy4, String buy5number, String buy5, String sell1number, String sell1,
			String sell2number, String sell2, String sell3number, String sell3, String sell4number, String sell4,
			String sell5number, String sell5, String date, String time) {
		super();
		this.number = number;
		this.name = name;
		this.kaipanjia =Double.valueOf(kaipanjia);
		this.zuorishoupanjia = Double.valueOf(zuorishoupanjia);
		this.dangqianjiage = Double.valueOf(dangqianjiage);
		this.top = Double.valueOf(top);
		this.low = Double.valueOf(low);
		this.jbuy1 = Double.valueOf(jbuy1);
		this.jsell1 = Double.valueOf(jsell1);
		this.chengjiaogupiao =Double.valueOf(chengjiaogupiao);
		this.chengjiaojine =Double.valueOf(chengjiaojine) ;
		this.buy1number = Double.valueOf(buy1number);
		this.buy1 = Double.valueOf(buy1);
		this.buy2number = Double.valueOf(buy2number);
		this.buy2 = Double.valueOf(buy2);
		this.buy3number = Double.valueOf(buy3number);
		this.buy3 = Double.valueOf(buy3);
		this.buy4number = Double.valueOf(buy4number);
		this.buy4 = Double.valueOf(buy4);
		this.buy5number = Double.valueOf(buy5number);
		this.buy5 = Double.valueOf(buy5);
		this.sell1number = Double.valueOf(sell1number);
		this.sell1 = Double.valueOf(sell1);
		this.sell2number = Double.valueOf(sell2number);
		this.sell2 = Double.valueOf(sell2);
		this.sell3number = Double.valueOf(sell3number);
		this.sell3 = Double.valueOf(sell3);
		this.sell4number = Double.valueOf(sell4number);
		this.sell4 = Double.valueOf(sell4);
		this.sell5number = Double.valueOf(sell5number);
		this.sell5 = Double.valueOf(sell5);
		this.date = date;
		this.time = time;
	}
	
	
	private String number;
	private String name;
	private Double kaipanjia;
	private Double zuorishoupanjia;
	private Double dangqianjiage;
	private Double top;
	private Double low;
	private Double jbuy1;
	private Double jsell1;
	private Double chengjiaogupiao;
	private Double chengjiaojine;
	private Double buy1number;
	private Double buy1;
	private Double buy2number;
	private Double buy2;
	private Double buy3number;
	private Double buy3;
	private Double buy4number;
	private Double buy4;
	private Double buy5number;
	private Double buy5;
	private Double sell1number;
	private Double sell1;
	private Double sell2number;
	private Double sell2;
	private Double sell3number;
	private Double sell3;
	private Double sell4number;
	private Double sell4;
	private Double sell5number;
	private Double sell5;
	private String date;
	private String time;
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


	public Double getKaipanjia() {
		return kaipanjia;
	}


	public void setKaipanjia(Double kaipanjia) {
		this.kaipanjia = kaipanjia;
	}


	public Double getZuorishoupanjia() {
		return zuorishoupanjia;
	}


	public void setZuorishoupanjia(Double zuorishoupanjia) {
		this.zuorishoupanjia = zuorishoupanjia;
	}


	public Double getDangqianjiage() {
		return dangqianjiage;
	}


	public void setDangqianjiage(Double dangqianjiage) {
		this.dangqianjiage = dangqianjiage;
	}


	public Double getTop() {
		return top;
	}


	public void setTop(Double top) {
		this.top = top;
	}


	public Double getLow() {
		return low;
	}


	public void setLow(Double low) {
		this.low = low;
	}


	public Double getJbuy1() {
		return jbuy1;
	}


	public void setJbuy1(Double jbuy1) {
		this.jbuy1 = jbuy1;
	}


	public Double getJsell1() {
		return jsell1;
	}


	public void setJsell1(Double jsell1) {
		this.jsell1 = jsell1;
	}


	public Double getChengjiaogupiao() {
		return chengjiaogupiao;
	}


	public void setChengjiaogupiao(Double chengjiaogupiao) {
		this.chengjiaogupiao = chengjiaogupiao;
	}


	public Double  getChengjiaojine() {
		return chengjiaojine;
	}


	public void setChengjiaojine(Double  chengjiaojine) {
		this.chengjiaojine = chengjiaojine;
	}


	public Double getBuy1number() {
		return buy1number;
	}


	public void setBuy1number(Double buy1number) {
		this.buy1number = buy1number;
	}


	public Double getBuy1() {
		return buy1;
	}


	public void setBuy1(Double buy1) {
		this.buy1 = buy1;
	}


	public Double getBuy2number() {
		return buy2number;
	}


	public void setBuy2number(Double buy2number) {
		this.buy2number = buy2number;
	}


	public Double getBuy2() {
		return buy2;
	}


	public void setBuy2(Double buy2) {
		this.buy2 = buy2;
	}


	public Double getBuy3number() {
		return buy3number;
	}


	public void setBuy3number(Double buy3number) {
		this.buy3number = buy3number;
	}


	public Double getBuy3() {
		return buy3;
	}


	public void setBuy3(Double buy3) {
		this.buy3 = buy3;
	}


	public Double getBuy4number() {
		return buy4number;
	}


	public void setBuy4number(Double buy4number) {
		this.buy4number = buy4number;
	}


	public Double getBuy4() {
		return buy4;
	}


	public void setBuy4(Double buy4) {
		this.buy4 = buy4;
	}


	public Double getBuy5number() {
		return buy5number;
	}


	public void setBuy5number(Double buy5number) {
		this.buy5number = buy5number;
	}


	public Double getBuy5() {
		return buy5;
	}


	public void setBuy5(Double buy5) {
		this.buy5 = buy5;
	}


	public Double getSell1number() {
		return sell1number;
	}


	public void setSell1number(Double sell1number) {
		this.sell1number = sell1number;
	}


	public Double getSell1() {
		return sell1;
	}


	public void setSell1(Double sell1) {
		this.sell1 = sell1;
	}


	public Double getSell2number() {
		return sell2number;
	}


	public void setSell2number(Double sell2number) {
		this.sell2number = sell2number;
	}


	public Double getSell2() {
		return sell2;
	}


	public void setSell2(Double sell2) {
		this.sell2 = sell2;
	}


	public Double getSell3number() {
		return sell3number;
	}


	public void setSell3number(Double sell3number) {
		this.sell3number = sell3number;
	}


	public Double getSell3() {
		return sell3;
	}


	public void setSell3(Double sell3) {
		this.sell3 = sell3;
	}


	public Double getSell4number() {
		return sell4number;
	}


	public void setSell4number(Double sell4number) {
		this.sell4number = sell4number;
	}


	public Double getSell4() {
		return sell4;
	}


	public void setSell4(Double sell4) {
		this.sell4 = sell4;
	}


	public Double getSell5number() {
		return sell5number;
	}


	public void setSell5number(Double sell5number) {
		this.sell5number = sell5number;
	}


	public Double getSell5() {
		return sell5;
	}


	public void setSell5(Double sell5) {
		this.sell5 = sell5;
	}


	public String getDate() {
		return date;
	}


	public void setDate(String date) {
		this.date = date;
	}


	public String getTime() {
		return time;
	}


	public void setTime(String time) {
		this.time = time;
	}
	
	
}

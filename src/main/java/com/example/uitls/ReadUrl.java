package com.example.uitls;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.GuPiao;
import com.example.demo.WebContent;

public class ReadUrl {
	private static Map<String, String> map = new ConcurrentHashMap<>();
	
	public static GuPiao readUrl(String title,boolean isTemp) {
		String url ="http://hq.sinajs.cn/list="+title;
		String code = HttpClientUtil.doGet(url);
		if(!isTemp) {
			return hanldeData(title, code);
		}
		String temp="";
		if(map.containsKey(title )) {
			temp=map.get(title );
		}
		if(code!=null && !code.equalsIgnoreCase(temp)) {
			map.put(title ,code);
			return hanldeData(title, code);
		}
		return null;
	}
	
	public static GuPiao readUrl(final int i,String title,boolean isTemp) {
		String number = String.format("%05d", i);
		String url ="http://hq.sinajs.cn/list="+title + number;
		String code = HttpClientUtil.doGet(url);
		if(!isTemp) {
			return hanldeData(title+number, code);
		}
		String temp="";
		if(map.containsKey(title + number)) {
			temp=map.get(title + number);
		}
		if(code!=null && !code.equalsIgnoreCase(temp)) {
			map.put(title + number,code);
			return hanldeData(title+number, code);
		}
		return null;
	}
	

	private static GuPiao hanldeData(String number, String code) {
		if(code.length()>30) {
			String value = code.split("=")[1];
			value=value.replace("\"", "");
//			System.out.println(value);
			String[] date = value.split(",");
			if(date.length<32) {
				return null;
			}
			GuPiao gp=new GuPiao(number,date[0],date[1],date[2],date[3],date[4],
								 date[5], date[6],date[7],date[8],date[9],
								 date[10],date[11],date[12],date[13],date[14],
								 date[15], date[16],date[17],date[18],date[19],
								 date[20],date[21],date[22],date[23],date[24],
								 date[25], date[26],date[27],date[28],date[29],
								 date[30],date[31]
					);
			return gp;
		}
		return null;
	}
}

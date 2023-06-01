/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

	public static final SimpleDateFormat COMMON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String getDateFormatStr(SimpleDateFormat dateFormat,Date date) {
		if(date!=null) {
			return dateFormat.format(date);
		}
		return null;
	}
	
	public static void main(String[] args) {
		Date nowTime=new Date(); 
		System.out.println(nowTime); 
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss"); 
		System.out.println(dateFormat.format(nowTime)); 

	}

}

package com.lemontruck.thermo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

public class Temperature {
	private long id;
	private int temperature;
	private Date datetime;
	private String country;
	private String city;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public int getTemperature() {
		return temperature;
	}
	
	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}
	
	public void setDatetime(String datetime) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		try {
			this.datetime = formatter.parse(datetime);;
		} catch (ParseException e) {
			Log.e(MainActivity.LOG, "Error when converting string to date");
		}
	}
	
	public Date getDatetime() {
		return datetime;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCity(String city) {
		this.city = city;
	}
	
	public String getCity() {
		return city;
	}
}

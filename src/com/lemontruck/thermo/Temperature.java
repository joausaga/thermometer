package com.lemontruck.thermo;

public class Temperature {
	private long id;
	private int temperature;
	private String datetime;
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
		this.datetime = datetime;
	}
	
	public String getDatetime() {
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

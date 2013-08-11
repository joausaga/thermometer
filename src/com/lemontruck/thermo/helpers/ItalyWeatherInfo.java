package com.lemontruck.thermo.helpers;

import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.ParseException;

public class ItalyWeatherInfo extends WeatherInfo {
	private final static String source = "http://www.meteotrentino.it/Default.aspx";
	
	private final HashMap<String,String> locationIds = new HashMap<String,String>();

	public ItalyWeatherInfo() {
		super();
		
		locationIds.put("Trento", "ultimi_dati1_GridUltimiDati_ctl02_txtTemperatura");
		locationIds.put("Rovereto", "ultimi_dati1_GridUltimiDati_ctl03_txtTemperatura");
		locationIds.put("Pergine", "ultimi_dati1_GridUltimiDati_ctl04_txtTemperatura");
		locationIds.put("Arco", "ultimi_dati1_GridUltimiDati_ctl05_txtTemperatura");
		locationIds.put("Cles", "ultimi_dati1_GridUltimiDati_ctl06_txtTemperatura");
		locationIds.put("Cavalese", "ultimi_dati1_GridUltimiDati_ctl07_txtTemperatura");
		locationIds.put("Male", "ultimi_dati1_GridUltimiDati_ctl08_txtTemperatura");
		locationIds.put("Tione", "ultimi_dati1_GridUltimiDati_ctl09_txtTemperatura");
	}
	
	public static String getSourceInfo() {
		return source;
	}
	
	/**
     * Read and return the weather information of the location passed as a parameter.
     * 
     * @param location The id of the location that is desired to get the temperature.
     * @return the current temperature at the location as well as the .
     * @throws ApiException If any connection or server error occurs.
     * @throws ParseException If there are problems parsing the response.
     */
	@Override
	public HashMap<String, String> getWeatherInfo(String location) 
	throws ApiException, ParseException 
	{
		HashMap<String,String> currentTempInfo = new HashMap<String,String>();
		String idLocation = "";
		
        String content = getUrlContent(source);
        Document doc = Jsoup.parse(content);
        
        idLocation = locationIds.get(location);
        Element contentTemp = doc.getElementById(idLocation);
		currentTempInfo.put("temp", contentTemp.text().split(",")[0]);
		
    	return currentTempInfo;
	}

}

package com.lemontruck.thermo.helpers;

import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.lemontruck.thermo.MainActivity;
import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.LocationException;
import com.lemontruck.thermo.exceptions.ParseException;

public class ParaguayWeatherInfo extends WeatherInfo {
	private final String LOG = "com.lemontruck.thermo";
	private final static String source = "http://www.meteorologia.gov.py";
	private final HashMap<String,String> locationIds = new HashMap<String,String>();
	
	public ParaguayWeatherInfo() {
		super();
		
		locationIds.put("Asuncion", "11");
		locationIds.put("Ciudad del Este", "10");
	}
	
	public static String getSourceInfo() {
		return source;
	}
	
	/**
     * Read and return the weather information of the location passed as a parameter.
     * 
     * @param idLocation The id of the location that is desired to get the temperature.
     * @return the current temperature at the location as well as the .
     * @throws ApiException If any connection or server error occurs.
     * @throws ParseException If there are problems parsing the response.
	 * @throws LocationException 
     */
	@Override
	public HashMap<String, String> getWeatherInfo(String idLocation) 
	throws ApiException, ParseException, LocationException 
	{
		HashMap<String,String> currentTempInfo = new HashMap<String,String>();
		idLocation = idLocation.replace("—", "o");
        String content = getUrlContent(source);
        Document doc = Jsoup.parse(content);
        Elements e = doc.getElementsByAttributeValue("class","Estilo7");
        Integer location = Integer.parseInt(locationIds.get(idLocation));
        Element temp = null;
        switch (location) {
        	case 11: //Asuncion
        		temp = e.get(0);
        		break;
        	case 10: //Ciudad del Este
        		temp = e.get(2);
        		break;
        	default:
        		throw new LocationException("Could not find the location");
        }
        currentTempInfo.put("temp", temp.text().split(",")[0]);
    	return currentTempInfo;
	}

}

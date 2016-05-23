package com.lemontruck.thermo.helpers;

import java.text.Normalizer;
import java.util.HashMap;
import java.math.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;
import android.util.SparseIntArray;

import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.LocationException;
import com.lemontruck.thermo.exceptions.ParseException;


public class ParaguayWeatherInfo extends WeatherInfo {
	private final String LOG = "com.lemontruck.thermo";
	private final static String source = "http://www.meteorologia.gov.py/index.php";
	private final HashMap<String,Integer> locationIds = new HashMap<String,Integer>();
	
	public ParaguayWeatherInfo() {
		super();
		
		locationIds.put("Vallemi", 18);
		locationIds.put("Ayolas", 17);
		locationIds.put("Saltos del Guaira", 16);
		locationIds.put("San Pedro", 15);
		locationIds.put("Concepcion", 14);
		locationIds.put("Pilar", 13);
		locationIds.put("Capitan Meza", 12);
		locationIds.put("Villarrica", 11);
		locationIds.put("Lambare", 10);
		locationIds.put("Luque", 9);
		locationIds.put("Tte. Irala Fernandez", 8);
		locationIds.put("Caazapa", 7);
		locationIds.put("San Bernardino", 6);
		locationIds.put("Hohenau", 5);
		locationIds.put("Pozo Colorado", 4);
		locationIds.put("San Lorenzo", 3);
		locationIds.put("Asuncion", 1);
		locationIds.put("Ciudad del Este", 0);
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
		HashMap<String, String> currentTempInfo = new HashMap<String, String>();
		SparseIntArray tempsInfo = new SparseIntArray();
		
		idLocation = Normalizer.normalize(idLocation, Normalizer.Form.NFD);
		idLocation = idLocation.replaceAll("[^\\p{ASCII}]", "");
		Integer location = locationIds.get(idLocation);
        String content = getUrlContent(source);
        Document doc = Jsoup.parse(content);
        Elements els = doc.select(".news-item");
        for (Element el : els) {
        	String txt = el.text();
        	String[] parsedTxt = txt.split("Temp:");
        	String locName = parsedTxt[0].trim();
        	String temp = parsedTxt[1].split("C")[0];
        	temp = temp.replaceAll("[^\\d.]", "");  // Remove all non numeric characters
        	Float fTemp = Float.valueOf(temp);
        	Integer intTemp = Math.round(fTemp); 
        	if (locName.contains("Guarani")) {
        		tempsInfo.put(0, intTemp);   // Ciudad del Este
        	} else if (locName.contains("Deportes")) {
        		tempsInfo.put(1, intTemp);   // Asuncion
        	} else if (locName.contains("San Lorenzo")) {
        		tempsInfo.put(3, intTemp);
        	} else if (locName.contains("Pozo Colorado")) {
        		tempsInfo.put(4, intTemp);
        	} else if (locName.contains("Hohenau")) {
        		tempsInfo.put(5, intTemp);
        	} else if (locName.contains("San Bernardino")) {
        		tempsInfo.put(6, intTemp);
        	} else if (locName.contains("Caazap")) {
        		tempsInfo.put(7, intTemp);
        	} else if (locName.contains("Irala")) {
        		tempsInfo.put(8, intTemp);
        	} else if (locName.contains("Pettirossi")) {
        		tempsInfo.put(9, intTemp);
        	} else if (locName.contains("Lambar")) {
        		tempsInfo.put(10, intTemp);
        	} else if (locName.contains("Villarrica")) {
        		tempsInfo.put(11, intTemp);
        	} else if (locName.contains("Meza")) {
        		tempsInfo.put(12, intTemp);
        	} else if (locName.contains("Pilar")) {
        		tempsInfo.put(13, intTemp);
        	} else if (locName.contains("Concepci")) {
        		tempsInfo.put(14, intTemp);
        	} else if (locName.contains("San Pedro")) {
        		tempsInfo.put(15, intTemp);
        	} else if (locName.contains("Saltos del Guair")) {
        		tempsInfo.put(16, intTemp);
        	} else if (locName.contains("Ayolas")) {
        		tempsInfo.put(17, intTemp);
        	} else if (locName.contains("Vallemi")) {
        		tempsInfo.put(18, intTemp);
        	}
        }
        
        Integer temp = 0;
        temp = tempsInfo.get(location, -999);
        
        if (temp == -999) {
        	throw new LocationException("Could not find the location");
        }
        
        currentTempInfo.put("temp", temp.toString());
    	return currentTempInfo;
	}

}

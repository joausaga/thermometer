package com.lemontruck.thermo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.lemontruck.thermo.helpers.TemperatureDataSource;

public class StatisticProvider extends IntentService {
	private static final String PREFS_NAME = "com.lemontruck.thermo.ThermoWidget";
	private int result = Activity.RESULT_CANCELED;
	
	public StatisticProvider() {
		super("StatisticProvider");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String country = settings.getString("country","Italy");
    	String city = settings.getString("location","Trento");
    	Exception exception = null;
    	
    	Context context = getApplicationContext();
    	
    	/* Send back the statistic information */
    	Bundle extras = intent.getExtras();
    	result = Activity.RESULT_OK;
        if (extras != null) {
        	Integer filter = (Integer) extras.get("FILTER");
        	
        	ArrayList<Object> ret = new ArrayList<Object>();
        	TemperatureDataSource datasource = new TemperatureDataSource(context);
    		datasource.open();
    		List<Temperature> temperatures = datasource.getTemperatures(country, city, filter);
    		datasource.close();
        	
    		/* Aggregate the temperature values according to the filter */
    		if (!temperatures.isEmpty())  {
    			ret.add(temperatures);
    			HashMap<String,Object> tempValues = null;
    			
    			switch(filter) {
	        		case StatisticFragment.TODAY: case StatisticFragment.SAMEDAYLASTYEAR:
	        			tempValues = aggregateTempByHours(temperatures);
	        			break;
	        		case StatisticFragment.THISWEEK: case StatisticFragment.SAMEWEEKLASTYEAR:
	        			tempValues = aggregateTempByDays(temperatures);
	        			break;
	        		case StatisticFragment.THISMONTH: case StatisticFragment.SAMEMONTHLASTYEAR:
	        			tempValues = aggregateTempByDays(temperatures);
	        			break;
	        		case StatisticFragment.THISYEAR: case StatisticFragment.THISSEMESTER:
	        			tempValues = aggregateTempByMonths(temperatures);
	        			break;
	        		default:
	        			Log.e(MainActivity.LOG, "Error!, unkown filter type");
	        			break;
    			}
    			ret.add(tempValues);
    		}
    		
        	Messenger messenger = (Messenger) extras.get("MESSENGER");
	        Message msg = Message.obtain();
	    	msg.arg1 = result;
	        if (result == Activity.RESULT_OK)
	        	msg.obj = ret;
	        else
	        	msg.obj = exception;
	        try {
	        	messenger.send(msg);
	        } catch (android.os.RemoteException e1) {
	        	Log.w(getClass().getName(), "Exception sending message", e1);
	        }
        }
	}
	
	/* To call when TODAY and THESAMEDAYLASTYEAR option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByHours(List<Temperature> temperatures) {
    	String timeFormat = "HH"; /* Interested in getting the hour of the date */
    	Integer durationPeriod = 24; /* Here the function aggregates, per hours, the temperature values of the whole day */
    	return aggregateTempByTimePeriod(temperatures, durationPeriod, timeFormat);
    }
    
    /* To call when THISWEEK, THESAMEWEEKLASTYEAR, THISMONTH and THESAMEMONTLASTYEAR option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByDays(List<Temperature> temperatures) {
    	String timeFormat = "dd"; /* Interested in getting the day of the date */
    	Integer durationPeriod = 31; /* Here the function aggregates, per days, the temperature values of the whole week */
    	return aggregateTempByTimePeriod(temperatures, durationPeriod, timeFormat); 
    }
    
    /* To call when THISYEAR and THISSEMESTER option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByMonths(List<Temperature> temperatures) {
    	String timeFormat = "MM"; /* Interested in getting the month of the date */
    	Integer durationPeriod = 12;
    	return aggregateTempByTimePeriod(temperatures, durationPeriod, timeFormat); 
    }
    
    private HashMap<String,Object> aggregateTempByTimePeriod(List<Temperature> temperatures,
    														 Integer durationPeriod,
    												   		 String timeFormat) {
    	HashMap<String,Object> tempList = new HashMap<String,Object>();
    	ArrayList<int[]> tempByTime = new ArrayList<int[]>();
    	tempByTime = reserveSpace(tempByTime, durationPeriod);
    	HashMap<Integer, Date> dates = new HashMap<Integer, Date>();
    	tempList.put("arrayValues", tempByTime);
    	tempList.put("hashDates", dates);
    	int index = 0;
    	
    	Log.i(MainActivity.LOG, "Temperature size: " + temperatures.size());
    	for (int i = 0; i < temperatures.size(); i++) {
    		Date tempDateTime = temperatures.get(i).getDatetime();
    		Integer tempVal = temperatures.get(i).getTemperature();
    		SimpleDateFormat dateFormat = new SimpleDateFormat(timeFormat);
    		Integer time = Integer.parseInt(dateFormat.format(tempDateTime));
    		if (timeFormat.equals("HH")) index = time; /* Avoiding negative index when we have time = 0 (0 hours) */
    		else index = time - 1;
    		int[] tempMeasure = tempByTime.get(index);
    		if (tempMeasure == null) {
    			tempMeasure = new int[2];
    			tempMeasure[1] = 1;
    			dates.put(index, tempDateTime);
    		}
    		else {
    			tempMeasure[1] += 1;
    		}
    		tempMeasure[0] += tempVal; /* Total measures */
    		tempByTime.set(index, tempMeasure);
    	}
		
    	return tempList;
    }

    private static ArrayList<int[]> reserveSpace(ArrayList<int[]> array, Integer maxElements) {
    	for (int i = 0; i < maxElements; i++)
    		array.add(null);

    	return array;
    }
	
}

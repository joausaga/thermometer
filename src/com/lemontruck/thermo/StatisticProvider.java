package com.lemontruck.thermo;

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

import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.LocationException;
import com.lemontruck.thermo.exceptions.ParseException;
import com.lemontruck.thermo.helpers.ItalyWeatherInfo;
import com.lemontruck.thermo.helpers.ParaguayWeatherInfo;
import com.lemontruck.thermo.helpers.TemperatureDataSource;
import com.lemontruck.thermo.helpers.WeatherInfo;

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
        	TemperatureDataSource datasource = new TemperatureDataSource(context);
    		datasource.open();
    		List<Temperature> temperatures = datasource.getTemperatures(country, city, filter);
    		datasource.close();
        	
        	Messenger messenger = (Messenger) extras.get("MESSENGER");
	        Message msg = Message.obtain();
	    	msg.arg1 = result;
	        if (result == Activity.RESULT_OK)
	        	msg.obj = temperatures;
	        else
	        	msg.obj = exception;
	        try {
	        	messenger.send(msg);
	        } catch (android.os.RemoteException e1) {
	        	Log.w(getClass().getName(), "Exception sending message", e1);
	        }
        }
	}
	
}

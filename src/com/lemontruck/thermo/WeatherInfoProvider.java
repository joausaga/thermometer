package com.lemontruck.thermo;

import java.util.HashMap;

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

public class WeatherInfoProvider extends IntentService {
	private static final String PREFS_NAME = "com.lemontruck.thermo.ThermoWidget";
	private int result = Activity.RESULT_CANCELED;
	private final static String LOG = "com.lemontruck.thermo";
	
	public WeatherInfoProvider() {
		super("WeatherInfoProvider");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String country = settings.getString("country","Italy");
    	String location = settings.getString("location","Trento");
    	Exception exception = null;
    	
    	Context context = getApplicationContext();
		
    	WeatherInfo weatherInfoHelper = null;
    	if (country.equalsIgnoreCase("italy")) {
    		weatherInfoHelper = new ItalyWeatherInfo();
    		weatherInfoHelper.prepareUserAgent(context);
    	}
    	else {
    		weatherInfoHelper = new ParaguayWeatherInfo();
    		weatherInfoHelper.prepareUserAgent(context);
    	}
    	
    	HashMap<String, String> weatherInfo = null;
    	try {
			weatherInfo = weatherInfoHelper.getWeatherInfo(location);
			result = Activity.RESULT_OK;
			TemperatureDataSource datasource = new TemperatureDataSource(this);
			datasource.open();
			datasource.saveTemperature(weatherInfo.get("temp"), country, location);
			datasource.close();
			Log.i(LOG,"New temperature: " + weatherInfo.get("temp"));
		} catch (ApiException e) {
			exception = e;
		} catch (ParseException e) {
			exception = e;
		} catch (LocationException e) {
			exception = e;
		}
    	
    	/* Send back the weather information */
    	Bundle extras = intent.getExtras();
        if (extras != null) {
        	Messenger messenger = (Messenger) extras.get("MESSENGER");
	        Message msg = Message.obtain();
	    	msg.arg1 = result;
	        if (result == Activity.RESULT_OK)
	        	msg.obj = weatherInfo;
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

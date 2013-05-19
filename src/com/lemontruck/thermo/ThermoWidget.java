package com.lemontruck.thermo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.LocationException;
import com.lemontruck.thermo.exceptions.NoNetworkException;
import com.lemontruck.thermo.exceptions.ParseException;

public class ThermoWidget extends AppWidgetProvider
{
	private final static String LOG = "com.lemontruck.thermo";
	public static final String WIDGET_ID_KEY ="thermowidgetid";
	private static final String PREFS_NAME = "com.lemontruck.thermo.ThermoWidget";
	private static boolean networkAvailable = true;
	
	public static void forceUpdate(Context context, AppWidgetManager appWidgetManager,
								   boolean newConfiguration) 
	{
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), 
												  R.layout.widget_layout);
		updateViews = cleanWidget(updateViews);
		if (existNetworkConnection(context)) {
			if (newConfiguration) {
				startOverWidget(context,updateViews);
				networkAvailable = true;
			}
			else
				if (networkAvailable) {
					updateTemperature(context, updateViews);
					networkAvailable = true;
				}
				else {
					startOverWidget(context,updateViews);
					networkAvailable = true;
				}
		}
		else {
			networkAvailable = false;
			turnOffWidget(context,updateViews,"");
		}
	}
	
	private static void startOverWidget(Context context, RemoteViews views) {
		views = turnOnMessageContainer(views);
		views.setViewVisibility(R.id.updating_widget, View.VISIBLE);
		Resources res = context.getResources();
		views.setTextViewText(R.id.message, res.getString(R.string.loading));
		doUpdate(context,views);
		context.startService(new Intent(context, UpdateService.class));
	}
	
	@Override
	public void onEnabled(Context context) {
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), 
												  R.layout.widget_layout);
		startOverWidget(context,updateViews);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.hasExtra(WIDGET_ID_KEY)) {
	        int[] ids = intent.getExtras().getIntArray(WIDGET_ID_KEY);
	        this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
	    } 
	    else { 
	    	super.onReceive(context, intent);
	    }
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
						 int[] appWidgetIds) 
	{
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), 
												  R.layout.widget_layout);
		updateViews = cleanWidget(updateViews);
		if (existNetworkConnection(context)) {
			if (networkAvailable) {
				updateTemperature(context, updateViews);
				networkAvailable = true;
			}
			else {
				startOverWidget(context,updateViews);
				networkAvailable = true;
			}
		}
		else {
			networkAvailable = false;
			turnOffWidget(context, updateViews, "");
		}
	}
	
	public static void doUpdate(Context context, RemoteViews updateViews) {
		ComponentName thisWidget = new ComponentName(context, ThermoWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(thisWidget, updateViews);
	}
	
	private static boolean existNetworkConnection(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null)
			return false;
		else
			return true;
	}
	
	private static void updateTemperature(Context context, RemoteViews views) {
		views = turnOnTempInfoContainers(views);
		views = showUpdatingWheel(context, views);
		doUpdate(context,views);
		context.startService(new Intent(context, UpdateService.class));
	}
	
	private static RemoteViews showUpdatingWheel(Context context, RemoteViews updateViews) {
		updateViews.setViewVisibility(R.id.temp_info, View.GONE);
		updateViews.setViewVisibility(R.id.updating_temp, View.VISIBLE);
		return updateViews;
	}
	
	public static RemoteViews hideUpdatingWheel(RemoteViews views) {
		views.setViewVisibility(R.id.updating_temp, View.GONE);
		views.setViewVisibility(R.id.temp_info, View.VISIBLE);
		return views;
	}
	
	private static void turnOffWidget(Context context, RemoteViews updateViews, 
									  String message) {
		updateViews = turnOnMessageContainer(updateViews);
		Resources res = context.getResources();
		if (message.equals(""))
			message = res.getString(R.string.no_network_message);
		updateViews.setTextViewText(R.id.message, message);
		UpdateService.bindActionOnClick(context, updateViews);
		doUpdate(context,updateViews);
	}
	
	public static RemoteViews cleanWidget(RemoteViews views) {
		views.setViewVisibility(R.id.temp_info_container_left, View.GONE);
		views.setViewVisibility(R.id.temp_info_container_right, View.GONE);
		views.setViewVisibility(R.id.messenger, View.GONE);
		views.setViewVisibility(R.id.updating_temp, View.GONE);
		views.setViewVisibility(R.id.updating_widget, View.GONE);
		return views;
	}
	
	public static RemoteViews turnOnTempInfoContainers(RemoteViews views) {
		views.setViewVisibility(R.id.temp_info_container_left, View.VISIBLE);
		views.setViewVisibility(R.id.temp_info_container_right, View.VISIBLE);
		return views;
	}
	
	public static RemoteViews turnOnMessageContainer(RemoteViews views) {
		views.setViewVisibility(R.id.messenger, View.VISIBLE);
		return views;
	}
	
	public static RemoteViews updateViewsVisibility(RemoteViews views, Integer visibility) {
		views.setViewVisibility(R.id.temp_icon, visibility);
		views.setViewVisibility(R.id.temp_desc, visibility);
		views.setViewVisibility(R.id.temp_info, visibility);
		views.setViewVisibility(R.id.last_updated, visibility);
		views.setViewVisibility(R.id.location, visibility);
		return views;
	}
	
	public static class UpdateService extends Service {
		
		@Override
        public void onStart(Intent intent, int startId) { 
			RemoteViews updateViews = new RemoteViews(this.getPackageName(), 
													  R.layout.widget_layout);
			Resources res = this.getResources();
			try {
				updateViews = buildUpdate(this, updateViews);
				bindActionOnClick(this,updateViews);
				doUpdate(this, updateViews);
			}
			catch (NoNetworkException e) {
				Log.e(LOG, "Could not update the widget, cause: No network available");
				ThermoWidget.turnOffWidget(this, updateViews, "");
			}
			catch (LocationException e) {
				Log.e(LOG, "Could not update the widget, cause: Unkown location");
				ThermoWidget.turnOffWidget(this, updateViews, res.getString(R.string.error));
			}
			catch (ApiException e) {
				Log.e(LOG, "Could not update the widget, cause: API error");
				ThermoWidget.turnOffWidget(this, updateViews, res.getString(R.string.error));
			}
			catch (ParseException e) {
				Log.e(LOG, "Could not update the widget, cause: HTML paser error");
				ThermoWidget.turnOffWidget(this, updateViews, res.getString(R.string.error));
			}
        }
		
		public void doUpdate(Context context, RemoteViews views) {
			ComponentName thisWidget = new ComponentName(context, ThermoWidget.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			manager.updateAppWidget(thisWidget, views);
		}
		
		public static void bindActionOnClick(Context context, RemoteViews views) {
			// When a user presses on the widget it shows a dialog window 
			// asking him/her to choose what he/she wants to do
			Intent intent = new Intent(context, ThermoDialog.class);
			PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.widget, pending);
		}
		
		public RemoteViews buildUpdate(Context context, RemoteViews views) 
		throws LocationException, ApiException, ParseException, NoNetworkException 
		{
			Resources res = context.getResources();
			HashMap<String,String> infoLocation = null;
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    	String location = settings.getString("location","Trento");
			
			Calendar cal = Calendar.getInstance();
        	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        	String updateTime = sdf.format(cal.getTime());
			
			
			infoLocation = getLocation(res, location);
	        String idLocation = infoLocation.get("id_location");
			String idLastUpdated = infoLocation.get("id_last_updated");
			String sourceInfoURL = res.getString(R.string.info_source);
			
			HashMap<String, String> weatherInfo = null;
			
            // Try retrieving the weather info
            if (ThermoWidget.existNetworkConnection(context)) {
				WeatherInfoHelper.prepareUserAgent(context);
	            weatherInfo = WeatherInfoHelper.getWeatherInfo(sourceInfoURL,idLocation, idLastUpdated);
            }
            else {
            	throw new NoNetworkException("No network connection");
            }
            
			views = ThermoWidget.cleanWidget(views);
			views = turnOnTempInfoContainers(views);
			views = updateWidgetViews(views,weatherInfo.get("temp"),location,
							  		  updateTime,
							  		  res.getStringArray(R.array.temperature_descriptions),
							  		  settings,
							  		  res);
			ThermoWidget.hideUpdatingWheel(views);
            
			return views;
		}
		
		private RemoteViews updateWidgetViews(RemoteViews views, String temperature, 
											  String location, String updateTime,
											  String[] tempDescriptions,
											  SharedPreferences settings,
											  Resources res) 
		{
			String unit = settings.getString("unit","Celsius");
			Integer intTemperature = Integer.parseInt(temperature);
			if (unit.equalsIgnoreCase("Fahrenheit"))
				intTemperature = (intTemperature * 9)/5+32; //Convert the temperature in celsius to fahrenheit
			views.setTextViewText(R.id.temp_info, intTemperature.toString() + "\u00B0");
            views.setTextViewText(R.id.location, location);
            views.setTextViewText(R.id.last_updated, res.getString(R.string.last_updated) + " " + updateTime);
            views = updateTempIconAndDesc(views, temperature, 
            					  		  tempDescriptions);
            return views;
		}
		
		private HashMap<String,String> getLocation(Resources res, String locSelected) 
		throws LocationException 
		{
			HashMap<String,String> infoLocation = new HashMap<String,String>();
			String[] locations = res.getStringArray(R.array.locations);
			int indexLocation = -1;
			for (int i = 0; i < locations.length; i++) {
				if (locations[i].equalsIgnoreCase(locSelected)) {
					indexLocation = i;
					break;
				}
			}
			if (indexLocation == -1)
				throw new LocationException("Could not find the location");
			else {
				String [] dataLocation;
				switch(indexLocation) {
					case 0: 
						dataLocation = res.getStringArray(R.array.info_location_0);
						break;
					case 1: 
						dataLocation = res.getStringArray(R.array.info_location_1);
						break;
					case 2: 
						dataLocation = res.getStringArray(R.array.info_location_2);
						break;
					case 3: 
						dataLocation = res.getStringArray(R.array.info_location_3);
						break;
					case 4: 
						dataLocation = res.getStringArray(R.array.info_location_4);
						break;
					case 5: 
						dataLocation = res.getStringArray(R.array.info_location_5);
						break;
					case 6: 
						dataLocation = res.getStringArray(R.array.info_location_6);
						break;
					case 7: 
						dataLocation = res.getStringArray(R.array.info_location_7);
						break;
					default:
						throw new LocationException("Could not find the location");
				}
				infoLocation.put("id_location", dataLocation[0]);
				infoLocation.put("id_last_updated", dataLocation[1]);
			}
			
			return infoLocation;
		}
		
		private RemoteViews updateTempIconAndDesc(RemoteViews updateViews, String strTemp, 
										   String[] temp_desc) {
			Integer temperature = Integer.valueOf(strTemp);
			if (temperature <= 0) {
				updateViews.setImageViewResource(R.id.temp_icon, R.drawable.lowest_blue);
				updateViews.setTextViewText(R.id.temp_desc, temp_desc[0]);
			}
			if (temperature >= 1 && temperature <= 15) {
				updateViews.setImageViewResource(R.id.temp_icon, R.drawable.blue);
				updateViews.setTextViewText(R.id.temp_desc, temp_desc[1]);
			}
			if (temperature >= 16 && temperature <= 26) {
				updateViews.setImageViewResource(R.id.temp_icon, R.drawable.orange);
				updateViews.setTextViewText(R.id.temp_desc, temp_desc[2]);
			}
			if (temperature >= 27 && temperature <= 35) {
				updateViews.setImageViewResource(R.id.temp_icon, R.drawable.red);
				updateViews.setTextViewText(R.id.temp_desc, temp_desc[3]);
			}
			if (temperature >= 36) {
				updateViews.setImageViewResource(R.id.temp_icon, R.drawable.purple);
				updateViews.setTextViewText(R.id.temp_desc, temp_desc[4]);
			}
			return updateViews;
		}
		
		@Override
        public IBinder onBind(Intent intent) {
			// We don't need to bind to this service
            return null;
        }
		
	}
}

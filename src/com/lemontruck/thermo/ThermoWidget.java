package com.lemontruck.thermo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.lemontruck.thermo.exceptions.LocationException;;

public class ThermoWidget extends AppWidgetProvider
{
	private final static String LOG = "com.lemontruck.thermo";
	public static final String WIDGET_ID_KEY ="thermowidgetid";
	private static final String PREFS_NAME = "com.lemontruck.thermo.ThermoWidget";
	private static boolean networkAvailable = true;
	
	public static void remakeWidget(Context context,
									AppWidgetManager appWidgetManager,
									int appWidgetId) 
	{
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), 
												  R.layout.widget_layout);
		updateViews = cleanWidget(updateViews);
		if (existNetworkConnection(context)) {
			startOverWidget(context,updateViews);
			networkAvailable = true;
		}
		else {
			networkAvailable = false;
			turnOffWidget(context,updateViews,"");
		}
	}
	
	private static void startOverWidget(Context context, RemoteViews views) {
		Log.i(LOG, "Starting startOverWidget method...");
		views = turnOnMessageContainer(views);
		views.setViewVisibility(R.id.updating_widget, View.VISIBLE);
		Resources res = context.getResources();
		views.setTextViewText(R.id.message, res.getString(R.string.loading));
		doUpdate(context,views);
		getWeatherInfo(context, views);
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
		Log.i(LOG, "On onUpdate method...");
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
		getWeatherInfo(context, views);
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
		bindActionOnClick(context, updateViews);
		doUpdate(context,updateViews);
	}
	
	public static RemoteViews cleanWidget(RemoteViews views) {
		views.setViewVisibility(R.id.temp_info_container_left, View.GONE);
		views.setViewVisibility(R.id.temp_info_container_right, View.GONE);
		views.setViewVisibility(R.id.options_container, View.GONE);
		views.setViewVisibility(R.id.messenger, View.GONE);
		views.setViewVisibility(R.id.updating_temp, View.GONE);
		views.setViewVisibility(R.id.updating_widget, View.GONE);
		return views;
	}
	
	public static RemoteViews turnOnTempInfoContainers(RemoteViews views) {
		views.setViewVisibility(R.id.temp_info_container_left, View.VISIBLE);
		views.setViewVisibility(R.id.temp_info_container_right, View.VISIBLE);
		views.setViewVisibility(R.id.options_container, View.VISIBLE);
		return views;
	}
	
	public static RemoteViews turnOnMessageContainer(RemoteViews views) {
		views.setViewVisibility(R.id.temp_info_container_left, View.INVISIBLE);
		views.setViewVisibility(R.id.temp_info_container_right, View.INVISIBLE);
		views.setViewVisibility(R.id.messenger, View.VISIBLE);
		return views;
	}
	
	private static void getWeatherInfo(Context context, RemoteViews views)  
	{
		// Try to retrieve the weather info
		Intent intent = new Intent(context, WeatherInfoProvider.class);
	    // Create a new Messenger for the communication back
		UpdateHandler handler = new UpdateHandler(context, views);
	    Messenger messenger = new Messenger(handler);
	    intent.putExtra("MESSENGER", messenger);
	    context.startService(intent);
	}
	
	public static RemoteViews buildUpdate(Context context, RemoteViews views,
								   HashMap<String, String> weatherInfo) 
	throws LocationException 
	{
		Resources res = context.getResources();
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		String country = settings.getString("country","Italy");
    	String location = settings.getString("location","Trento");
						
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	    String updateTime = sdf.format(cal.getTime());
	            
		views = cleanWidget(views);
		views = turnOnTempInfoContainers(views);
		views = updateWidgetViews(views,weatherInfo.get("temp"),location,
						  		  country, updateTime,
						  		  res.getStringArray(R.array.temperature_descriptions),
						  		  settings,
						  		  res);
		hideUpdatingWheel(views);
        
		return views;
	}
			
	private static RemoteViews updateWidgetViews(RemoteViews views, String temperature, 
										  String location, String country,
										  String updateTime,
										  String[] tempDescriptions,
										  SharedPreferences settings,
										  Resources res) 
	throws LocationException 
	{
		String unit = settings.getString("unit","Celsius");
		Integer intTemperature = Integer.parseInt(temperature);
		if (unit.equalsIgnoreCase("Fahrenheit"))
			intTemperature = (intTemperature * 9)/5+32; //Convert the temperature in celsius to fahrenheit
		views.setTextViewText(R.id.temp_info, intTemperature.toString() + "\u00B0");
        views.setTextViewText(R.id.location, location);
        views.setTextViewText(R.id.country, country);
        views.setTextViewText(R.id.last_update, updateTime);
        views = updateTempIconAndDesc(views, temperature, 
        					  		  tempDescriptions);
        return views;
	}
			
	private static RemoteViews updateTempIconAndDesc(RemoteViews updateViews, String strTemp, 
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
	
	public static void bindActionOnClick(Context context, RemoteViews views)  
	{
       /* When the user presses the widget 
        * opens the config application */
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context,ThermoWidget.class));
        Intent intent = new Intent();
	    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[0]);
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setComponent(new ComponentName("com.lemontruck.thermo",
											  "com.lemontruck.thermo.MainActivity"));
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
												  intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget, pendingIntent);
		
		/* When the user clicks on the refresh icon, the widget 
	     * has to call this to update the widget */
		intent = new Intent();
	    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
	    intent.putExtra(ThermoWidget.WIDGET_ID_KEY, ids);
	    pendingIntent = PendingIntent.getBroadcast(context, 0, 
        										 intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.options_container, pendingIntent);
	}
	
	private static class UpdateHandler extends Handler {
		private Context context;
		private RemoteViews views;
		private Resources res;
	
		public UpdateHandler(Context context, RemoteViews views) {
			this.context = context;
			this.views = views;
			this.res = context.getResources();
		}
		
		public void handleMessage(Message message) {
			if (message.arg1 == Activity.RESULT_OK) {
				HashMap<String, String> weatherInfo = (HashMap<String, String>) message.obj;
				try {
					views = buildUpdate(context,views,weatherInfo);
					bindActionOnClick(context, views);
					ThermoWidget.doUpdate(context, views);
				} catch (LocationException e) {
					Log.e(LOG, "Could not update the widget, cause: Unkown location");
					ThermoWidget.turnOffWidget(context, views, res.getString(R.string.error));
				}
			} 
			else {
				Exception e = (Exception) message.obj;
				if (e.getClass().getName().equals("ApiException"))
					Log.e(LOG, "Could not update the widget, cause: API error");
				else
					if (e.getClass().getName().equals("ParseException"))
						Log.e(LOG, "Could not update the widget, cause: HTML paser error");
					else
						if (e.getClass().getName().equals("LocationException"))
							Log.e(LOG, "Could not update the widget, cause: Unkown location");
						else
							Log.e(LOG, "Could not update the widget, cause: Unkown cause");
				ThermoWidget.turnOffWidget(context, views, res.getString(R.string.error));
			}
		}
	}
}

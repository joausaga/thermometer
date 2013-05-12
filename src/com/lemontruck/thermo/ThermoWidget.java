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
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.LocationException;
import com.lemontruck.thermo.exceptions.ParseException;

public class ThermoWidget extends AppWidgetProvider
{
	private final static String LOG = "com.lemontruck.thermo";
	public static final String WIDGET_ID_KEY ="thermowidgetid";
	
	@Override
	public void onEnabled(Context context) {
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		updateViews.setViewVisibility(R.id.updating_circle, View.VISIBLE);
		updateViews.setTextViewText(R.id.location, "Loading...");
		ComponentName thisWidget = new ComponentName(context, ThermoWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(thisWidget, updateViews);
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
		// To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
	}
	
	public static class UpdateService extends Service {
		private static String PREDEFINED_LOCATION = "Trento";
		
		@Override
        public void onStart(Intent intent, int startId) {
			RemoteViews updateViews = null;
			if (existNetworkConnection(this)) {
				updateViews = showUpdatingCircle(this);
				doUpdate(this,updateViews); //Making the updating circle appears
				updateViews = buildUpdateWithNC(this,updateViews);
			}
			else {
				updateViews = buildUpdateWithoutNC(this);
			}
			
			bindActionOnClick(this,updateViews);
			doUpdate(this, updateViews);
        }
		
		public void doUpdate(Context context, RemoteViews views) {
			ComponentName thisWidget = new ComponentName(context, ThermoWidget.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			manager.updateAppWidget(thisWidget, views);
		}
		
		private void bindActionOnClick(Context context, RemoteViews views) {
			// When a user presses on the widget it shows a dialog window 
			// asking him/her to choose what he/she wants to do
			Intent intent = new Intent(this, ThermoDialog.class);
			PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.widget, pending);
		}
		
		public RemoteViews buildUpdateWithoutNC(Context context) {
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			updateViews = updateViewsVisibility(updateViews, View.INVISIBLE);
			Resources res = context.getResources();
			updateViews.setTextViewText(R.id.location, res.getString(R.string.no_network_message));
			return updateViews;
		}
		
		private RemoteViews showUpdatingCircle(Context context) {
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			updateViews.setViewVisibility(R.id.temp_info, View.GONE);
			updateViews.setViewVisibility(R.id.updating_circle, View.VISIBLE);
			return updateViews;
		}
		
		public RemoteViews buildUpdateWithNC(Context context, RemoteViews views) {
			Resources res = context.getResources();
			HashMap<String,String> infoLocation = null;
			
			Calendar cal = Calendar.getInstance();
        	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        	String updateTime = sdf.format(cal.getTime());
			
			try {
				infoLocation = getLocation(res, PREDEFINED_LOCATION);
			} catch (LocationException e) {
				views = hideUpdatingCircle(views);
				Log.e(LOG, e.getMessage());
			}
	        String idLocation = infoLocation.get("id_location");
			String idLastUpdated = infoLocation.get("id_last_updated");
			String sourceInfoURL = res.getString(R.string.info_source);
			
			HashMap<String, String> weatherInfo = null;
			try {
                // Try retrieving the weather info
                WeatherInfoHelper.prepareUserAgent(context);
                weatherInfo = WeatherInfoHelper.getWeatherInfo(sourceInfoURL,idLocation, idLastUpdated);
            } catch (ApiException e) {
            	views = hideUpdatingCircle(views);
                Log.e(LOG, "Couldn't the weather info source", e);
            } catch (ParseException e) {
            	views = hideUpdatingCircle(views);
                Log.e(LOG, "Couldn't parse the weather info", e);
            }
			
			views = updateViewsVisibility(views, View.VISIBLE);
			views = updateWidgetViews(views,weatherInfo.get("temp"),PREDEFINED_LOCATION,
							  		  updateTime,
							  		  res.getStringArray(R.array.temperature_descriptions));
            views = hideUpdatingCircle(views);
            
			return views;
		}
		
		private RemoteViews updateWidgetViews(RemoteViews views, String temperature, 
											  String location, String updateTime,
											  String[] tempDescriptions) 
		{
			views.setTextViewText(R.id.temp_info, temperature + "\u00B0");
            views.setTextViewText(R.id.location, location);
            views.setTextViewText(R.id.last_updated, "Last Updated " + updateTime);
            views = updateTempIconAndDesc(views, temperature, 
            					  		  tempDescriptions);
            return views;
		}
		
		private boolean existNetworkConnection(Context context) {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			if (ni == null)
				return false;
			else
				return true;
		}
		
		private RemoteViews updateViewsVisibility(RemoteViews views, Integer visibility) {
			views.setViewVisibility(R.id.temp_icon, visibility);
			views.setViewVisibility(R.id.temp_desc, visibility);
			views.setViewVisibility(R.id.temp_info, visibility);
			views.setViewVisibility(R.id.last_updated, visibility);
			return views;
		}
		
		private RemoteViews hideUpdatingCircle(RemoteViews views) {
			views.setViewVisibility(R.id.updating_circle, View.GONE);
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

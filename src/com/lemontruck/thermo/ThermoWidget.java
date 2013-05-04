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
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.LocationException;
import com.lemontruck.thermo.exceptions.ParseException;

public class ThermoWidget extends AppWidgetProvider
{
	private final static String LOG = "com.lemontruck.thermo";
	public static final String WIDGET_ID_KEY ="thermowidgetid";
	
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
						 int[] appWidgetIds) {
		// To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
	}
	
	public static class UpdateService extends Service {
		private static String PREDEFINED_LOCATION = "Trento";
		private static String PREDEFINED_CLICK_ACTION = "Update";
		
		@Override
        public void onStart(Intent intent, int startId) {
			// Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, ThermoWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }
		
		private HashMap<String,String> getLocation(Resources res, String locSelected) 
		throws LocationException {
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
		
		public RemoteViews buildUpdate(Context context) {
			Resources res = context.getResources();
			HashMap<String,String> infoLocation = null;
			
			Calendar cal = Calendar.getInstance();
        	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        	String updateTime = sdf.format(cal.getTime());
			
			try {
				infoLocation = getLocation(res, PREDEFINED_LOCATION);
			} catch (LocationException e) {
				Log.e(LOG, e.getMessage());
			}
	        String idLocation = infoLocation.get("id_location");
			String idLastUpdated = infoLocation.get("id_last_updated");
			String sourceInfoURL = res.getString(R.string.info_source);
			
			RemoteViews updateViews = null;
			HashMap<String, String> weatherInfo = null;
			try {
                // Try retrieving the weather info
                WeatherInfoHelper.prepareUserAgent(context);
                weatherInfo = WeatherInfoHelper.getWeatherInfo(sourceInfoURL,idLocation, idLastUpdated);
            } catch (ApiException e) {
                Log.e(LOG, "Couldn't the weather info source", e);
            } catch (ParseException e) {
                Log.e(LOG, "Couldn't parse the weather info", e);
            }
			
			// Build an update that holds the updated widget contents
            updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            updateViews.setTextViewText(R.id.temp_info, weatherInfo.get("temp") + "\u00B0");
            updateViews.setTextViewText(R.id.location, PREDEFINED_LOCATION);
            //updateViews.setTextViewText(R.id.last_updated, "Last Updated " + weatherInfo.get("last_updated"));
            updateViews.setTextViewText(R.id.last_updated, "Last Updated " + updateTime);
            updateTempIconAndDesc(updateViews, weatherInfo.get("temp"), 
            					  res.getStringArray(R.array.temperature_descriptions));
            
            // When user clicks on widget force update
            configureWidgetClick(context, updateViews);
            
			return updateViews;
		}
		
		private void configureWidgetClick(Context context, RemoteViews view) {
			PendingIntent pendingIntent = null;
			if (PREDEFINED_CLICK_ACTION.equalsIgnoreCase("update")) {
				AppWidgetManager man = AppWidgetManager.getInstance(context);
			    int[] ids = man.getAppWidgetIds(new ComponentName(context,ThermoWidget.class));
			    Intent updateIntent = new Intent();
			    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			    updateIntent.putExtra(ThermoWidget.WIDGET_ID_KEY, ids);
			    pendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, 
						 								   PendingIntent.FLAG_UPDATE_CURRENT);
			}
			else {  /* Visit the source web page */
				Resources res = context.getResources();
				String webInfoSource = res.getString(R.string.web_info_source);
		        Intent defineIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webInfoSource));
		        pendingIntent = PendingIntent.getActivity(context, 0, defineIntent, 0);
			}
			view.setOnClickPendingIntent(R.id.widget, pendingIntent);
		}
		
		private void updateTempIconAndDesc(RemoteViews updateViews, String strTemp, 
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
		}
		
		@Override
        public IBinder onBind(Intent intent) {
			// We don't need to bind to this service
            return null;
        }
		
	}
}

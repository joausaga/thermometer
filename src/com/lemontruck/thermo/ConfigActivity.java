package com.lemontruck.thermo;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Spinner;

public class ConfigActivity extends Activity {
	private final static String LOG = "com.lemontruck.thermo";
	private static final String PREFS_NAME= "com.lemontruck.thermo.ThermoWidget";
	
	int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	Spinner location;
	RadioButton unit;
	
	
	public ConfigActivity() {
		super();
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Context context = getApplicationContext();
        
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        setContentView(R.layout.config_activity);
        
		setSelectedLocation(context);
        setSelectedUnit(context);
        
        // Bind the action for the save button.
        findViewById(R.id.save_button).setOnClickListener(handlerSaveButton);
        findViewById(R.id.cancel_button).setOnClickListener(handlerCancelButton);
        
        // Find the widget id from the intent. 
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        	Log.w(LOG, "Invalid App Id");
            finish();
        }

    }
    
    private void setSelectedUnit(Context context) {
    	Resources res = context.getResources();
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	String currentUnit = settings.getString("unit","celsius");
    	String fahrenheit = res.getString(R.string.fahrenheit);
    	
    	if (currentUnit.equalsIgnoreCase(fahrenheit)) 
    		unit = (RadioButton)findViewById(R.id.unit_fahrenheit);
    	else
    		unit = (RadioButton)findViewById(R.id.unit_celsius);
    	
    	unit.setChecked(true);
    }
    
    private void setSelectedLocation(Context context) 
    {	
    	Resources res = context.getResources();
    	String[] locations = res.getStringArray(R.array.locations);
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	String currentLocation = settings.getString("location","Trento");
    	
		int indexLocation = -1;
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equalsIgnoreCase(currentLocation)) {
				indexLocation = i;
				break;
			}
		}
		
		location = (Spinner)findViewById(R.id.location_list);
        location.setSelection(indexLocation);
    }
    
    View.OnClickListener handlerSaveButton = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = ConfigActivity.this;

            // When the button is clicked, save the string in our prefs and return that they
            // clicked OK.
            String locationSelected = location.getSelectedItem().toString();
            RadioButton rCelsius = (RadioButton) findViewById(R.id.unit_celsius);
            String unitSelected = rCelsius.isChecked() ? "celsius" : "fahrenheit";
            
            SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
            prefs.putString("location", locationSelected);
            prefs.putString("unit", unitSelected);
            prefs.commit();

            Log.i(LOG, "Changed the configuration of the widget to Location: " + locationSelected + " and Unit: " + unitSelected);
            
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ThermoWidget.forceUpdate(context, appWidgetManager, true);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };
    
    View.OnClickListener handlerCancelButton = new View.OnClickListener() {
    	 public void onClick(View v) {
    		 Intent resultValue = new Intent();
             resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
             setResult(RESULT_CANCELED, resultValue);
             finish();
    	 }
    };
}

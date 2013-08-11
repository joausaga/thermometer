package com.lemontruck.thermo;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.lemontruck.thermo.helpers.ItalyWeatherInfo;
import com.lemontruck.thermo.helpers.ParaguayWeatherInfo;

public class ConfigFragment extends Fragment {
	private final static String LOG = "com.lemontruck.thermo";
	private final static String PREFS_NAME= "com.lemontruck.thermo.ThermoWidget";
	public final static String ARG_SECTION_NUMBER = "section_number";
	private Activity activity;
	public static Context context;

	Spinner countries;
	Spinner locations;
	RadioButton unit;
	
	public ConfigFragment() {
		super();
	}
	
	@Override
    public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
    }
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			 				 Bundle savedInstanceState) 
    {
    	View rootView = inflater.inflate(R.layout.config_fragment, container, false);
    	setHasOptionsMenu(true);
    	
    	context = activity.getApplicationContext();
        
		setSelectedCountry(context,rootView);
		loadLocations(context, rootView);
        setSelectedUnit(context,rootView);
        

        return rootView;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        //MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save:
                saveSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void setSelectedUnit(Context context, View rootView) {
    	Resources res = context.getResources();
    	SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
    	String currentUnit = settings.getString("unit","celsius");
    	String fahrenheit = res.getString(R.string.fahrenheit);
    	
    	if (currentUnit.equalsIgnoreCase(fahrenheit)) 
    		unit = (RadioButton) rootView.findViewById(R.id.unit_fahrenheit);
    	else
    		unit = (RadioButton) rootView.findViewById(R.id.unit_celsius);
    	
    	unit.setChecked(true);
    }
    
    private void setSelectedCountry(final Context context,
    								final View rootView) 
    {	
    	final Resources res = context.getResources();
    	String[] listCountries = res.getStringArray(R.array.countries);
    	SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
    	String currentCountry = settings.getString("country","Italy");
    	
		int indexLocation = -1;
		for (int i = 0; i < listCountries.length; i++) {
			if (listCountries[i].equalsIgnoreCase(currentCountry)) {
				indexLocation = i;
				break;
			}
		}
		
		countries = (Spinner) rootView.findViewById(R.id.country_list);
        countries.setSelection(indexLocation);
        countries.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int pos, long id) {
				locations = (Spinner) rootView.findViewById(R.id.location_list);
				ArrayAdapter<CharSequence> adapter = null;
				String country = "";
				if (pos == 0) {
					adapter =  ArrayAdapter.createFromResource(parent.getContext(),
							  				R.array.locations_it, android.R.layout.simple_spinner_item);
					country = "italy";
				}
				else {
					adapter =  ArrayAdapter.createFromResource(parent.getContext(),
			  								R.array.locations_py, android.R.layout.simple_spinner_item);
					country = "paraguay";
				}
				bindLinkFootText(context,country,rootView);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				locations.setAdapter(adapter);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Log.i(LOG, "The user does not select any option");
			}
        });
    }
    
    private void bindLinkFootText(final Context context, 
    							  final String country,
    							  final View rootView) {
    	Resources res = context.getResources();
    	TextView footText = (TextView) rootView.findViewById(R.id.foot_text);
    	footText.setPaintFlags(footText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    	if (country.equalsIgnoreCase("italy"))
			footText.setText(res.getString(R.string.poweredby_it));
    	else
			footText.setText(res.getString(R.string.poweredby_py));
    	
        footText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri url = null;
				if (country.equalsIgnoreCase("italy"))
					url = Uri.parse(ItalyWeatherInfo.getSourceInfo());
				else
					url = Uri.parse(ParaguayWeatherInfo.getSourceInfo());
				Intent intent = new Intent(Intent.ACTION_VIEW, url);
		        startActivity(intent);
			}
		});
    }
    
    private void loadLocations(Context context, View rootView) {
    	SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
    	String currentCountry = settings.getString("country","Italy");
    	locations = (Spinner) rootView.findViewById(R.id.location_list);
    	ArrayAdapter<CharSequence> adapter = null;
    	
    	if (currentCountry.equalsIgnoreCase("italy"))
    		adapter =  ArrayAdapter.createFromResource(context,
	  				   R.array.locations_it, android.R.layout.simple_spinner_item);
    	else
    		adapter =  ArrayAdapter.createFromResource(context,
	  				   R.array.locations_py, android.R.layout.simple_spinner_item);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		locations.setAdapter(adapter);
    }
    
    private void saveSettings() {
        Context context = ConfigFragment.context;

        // When the button is clicked, save the string in our prefs and return that they
        // clicked OK.
        String countrySelected = countries.getSelectedItem().toString();
        String locationSelected = locations.getSelectedItem().toString();
        RadioButton rCelsius = (RadioButton) activity.findViewById(R.id.unit_celsius);
        String unitSelected = rCelsius.isChecked() ? "celsius" : "fahrenheit";
        
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString("country", countrySelected);
        prefs.putString("location", locationSelected);
        prefs.putString("unit", unitSelected);
        prefs.commit();

        Log.i(LOG, "Changed the configuration of the widget to Location: " + 
        			locationSelected + " and Unit: " + unitSelected);
        
        /*AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ThermoWidget.forceUpdate(context, true);
        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        Bundle extras = resultValue.getExtras();
        ComponentName provider = new ComponentName(context,ThermoWidget.class);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetManager.getAppWidgetIds(provider));
        activity.setResult(Activity.RESULT_OK, resultValue);
        activity.finish();*/
    }
}

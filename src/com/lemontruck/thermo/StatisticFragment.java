package com.lemontruck.thermo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.lemontruck.thermo.helpers.TemperatureDataSource;

public class StatisticFragment extends Fragment {
	public static final String ARG_SECTION_NUMBER = "section_number";
	
	/* FILTERS */
	public final static int TODAY = 5000;
	public final static int THISWEEK = 5001;
	public final static int THISMONTH = 5002;
	public final static int THISSEMESTER = 5003;
	public final static int THISYEAR = 5004;
	public final static int SAMEDAYLASTYEAR = 5005;
	
	private Activity activity;
	private Context context;
	private int filter;
	
	public StatisticFragment() {
		super();
	}
	
	@Override
    public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            				 Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.statistic_fragment, container, false);
        context = activity.getApplicationContext();
		filter = TODAY;
        updateLayout(view);
        return view;
    }

    private void addListenerSpinner(View view) {
    	Spinner filterSp = (Spinner) view.findViewById(R.id.filter);
    	filterSp.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int pos, long id) 
        	{
        		if (pos == 0) filter = TODAY;
            	if (pos == 1) filter = THISWEEK;
            	if (pos == 2) filter = THISMONTH;
            	if (pos == 3) filter = THISSEMESTER;
            	if (pos == 4) filter = THISYEAR;
            	if (pos == 5) filter =SAMEDAYLASTYEAR;
            	updateLayout(view);
        	}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Log.i(MainActivity.LOG, "The user does not select any option");
			}
    	});
    }
    
    private void setCurrentLocation(View view) {
    	SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
    	String currentCountry = settings.getString("country","Italy");
    	String currentCity = settings.getString("location","Trento");
    	
    	TextView location = (TextView) view.findViewById(R.id.label_location);
    	location.setText(currentCity + ", " + currentCountry);
    }
    
    private void updateLayout(View view) {
    	/* Update Location Label */
        setCurrentLocation(view);
        /* Update Temperature Values */
    	HashMap<String,String> tempValues = getTemperatureValues(view);
    	/* Update AVG Temperature */
    	TextView avgTemp = (TextView) view.findViewById(R.id.avg_temp);
    	avgTemp.setText(tempValues.get("avg")+"\u00B0");
    	/* Update MAX Temperature */
    	TextView maxTemp = (TextView) view.findViewById(R.id.max_temp);
    	maxTemp.setText(tempValues.get("max")+"\u00B0");
    	/* Update MIN Temperature */
    	TextView minTemp = (TextView) view.findViewById(R.id.min_temp);
    	minTemp.setText(tempValues.get("min")+"\u00B0");
    }
    
    private List<Temperature> getStatistics(View view) {    	
    	/* Get the current location (country and city) */
    	SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
    	String country = settings.getString("country","Italy");
    	String city = settings.getString("location","Trento");
    	
    	TemperatureDataSource datasource = new TemperatureDataSource(context);
		datasource.open();
		List<Temperature> temperatures = datasource.getTemperatures(country, city, filter);
		datasource.close();
		
		return temperatures;
    }
    
    private HashMap<String,String> getTemperatureValues(View view) {
    	List<Temperature> temperatures = getStatistics(view);
    	HashMap<String,String> tempValues = new HashMap<String,String>();
    	/* Initializing the variables */
    	Integer temp = temperatures.get(0).getTemperature();
    	Integer avg = 0;
    	Integer total = temp;
    	Integer max = temp;
    	Integer min = temp;
    	
    	for (int i = 1; i < temperatures.size(); i++) {
    		temp = temperatures.get(i).getTemperature();
    		if (temp > max)
    			max = temp;
    		if (temp < min)
    			min = temp;
    		total += temp;
    	}
    	avg = total / temperatures.size();
    	
    	tempValues.put("avg", avg.toString());
    	tempValues.put("min", min.toString());
    	tempValues.put("max", max.toString());
    	
    	return tempValues;
    }
    
    private ArrayList<int[]> aggregateTempByHour(List<Temperature> temperatures) {
    	ArrayList<int[]> tempByHour = new ArrayList<int[]>(24);
    	
    	for (int i = 0; i < temperatures.size(); i++) {
    		Date tempDateTime = temperatures.get(i).getDatetime();
    		Integer tempVal = temperatures.get(i).getTemperature();
    		SimpleDateFormat dateFormat = new SimpleDateFormat("HH");
    		Integer hour = Integer.parseInt(dateFormat.format(tempDateTime));
    		int[] tempMeasure = tempByHour.get(hour); 
    		if (tempMeasure == null) {
    			tempMeasure = new int[2];
    			tempMeasure[1] = 0;
    		}
    		else {
    			tempMeasure[1] += 1;
    		}
    		tempMeasure[0] += tempVal;
    		tempByHour.add(hour, tempMeasure);
    	}
    	
    	return tempByHour;
    }
    
    private ArrayList<int[]> aggregateTempByDay(List<Temperature> temperatures) {
    	ArrayList<int[]> tempByDay = new ArrayList<int[]>(7);
    	
    	for (int i = 0; i < temperatures.size(); i++) {
    		Date tempDateTime = temperatures.get(i).getDatetime();
    		Integer tempVal = temperatures.get(i).getTemperature();
    		SimpleDateFormat dateFormat = new SimpleDateFormat("dd");
    		Integer day = Integer.parseInt(dateFormat.format(tempDateTime));
    		int[] tempMeasure = tempByDay.get(day); 
    		if (tempMeasure == null) {
    			tempMeasure = new int[2];
    			tempMeasure[1] = 0;
    		}
    		else {
    			tempMeasure[1] += 1;
    		}
    		tempMeasure[0] += tempVal;
    		tempByDay.add(day, tempMeasure);
    	}
    	
    	return tempByDay;
    }
}

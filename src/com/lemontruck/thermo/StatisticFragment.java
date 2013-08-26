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
import android.widget.ArrayAdapter;
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
	private List<Temperature> temperatures;
	
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
    	populateTemperatureList(view);
    }
    
    private void populateTemperatureList(View view) {
    	Spinner filterSp = (Spinner) view.findViewById(R.id.filter);
    	HashMap<String,Object> tempValues = null;
    	SimpleDateFormat dateFormat = null;
    	ArrayList<String> listValues = new ArrayList<String>();
    	
    	switch(filter) {
    		case TODAY: case SAMEDAYLASTYEAR:
    			tempValues = aggregateTempByHours();
    			dateFormat = new SimpleDateFormat("HH");
    			break;
    		case THISWEEK:
    			tempValues = aggregateTempByDays();
    			dateFormat = new SimpleDateFormat("E");
    			break;
    		case THISMONTH:
    			tempValues = aggregateTempByWeeks();
    			dateFormat = new SimpleDateFormat("dd");
    			break;
    		case THISYEAR: case THISSEMESTER:
    			if (filter == THISYEAR) tempValues = aggregateTempByMonths(true);
    			else tempValues = aggregateTempByMonths(false);
    			dateFormat = new SimpleDateFormat("MM");
    			break;
    		default:
    			Log.e(MainActivity.LOG, "Error!, unkown filter type");
    			break;
    	}
    	
    	HashMap<Integer, Date> dates = (HashMap<Integer, Date>) tempValues.get("hashDates");
		ArrayList<int[]> tempByTime = (ArrayList<int[]>) tempValues.get("arrayValues");
		Date date = null;
		for (int i = 0; i < tempByTime.size(); i++) {
			date = dates.get(i);
			int avgTemp = tempByTime.get(i)[0] / tempByTime.get(i)[1]; 
			listValues.add(dateFormat.format(date) + "\tAvg: " + avgTemp);
		}
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context,
											android.R.layout.simple_list_item_1, listValues);
		filterSp.setAdapter(arrayAdapter); 
    }
    
    private void getStatistics(View view) {    	
    	/* Get the current location (country and city) */
    	SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
    	String country = settings.getString("country","Italy");
    	String city = settings.getString("location","Trento");
    	
    	TemperatureDataSource datasource = new TemperatureDataSource(context);
		datasource.open();
		temperatures = datasource.getTemperatures(country, city, filter);
		datasource.close();
    }
    
    private HashMap<String,String> getTemperatureValues(View view) {
    	getStatistics(view);
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
    
    /* To call when TODAY and THESAMEDAYLASTYEAR option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByHours() {
    	String timeFormat = "HH"; /* Interested in getting the hour of the date */
    	Integer durationPeriod = 24; /* Here the function aggregates, per hours, the temperature values of the whole day */
    	return aggregateTempByTimePeriod(durationPeriod, timeFormat);
    }
    
    /* To call when THISWEEK option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByDays() {
    	String timeFormat = "dd"; /* Interested in getting the day of the date */
    	Integer durationPeriod = 7; /* Here the function aggregates, per days, the temperature values of the whole week */
    	return aggregateTempByTimePeriod(durationPeriod, timeFormat); 
    }
    
    /* To call when THISMONTH option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByWeeks() {
    	String timeFormat = "W"; /* Interested in getting the week of the date in the month*/
    	Integer durationPeriod = 5; /* Here the function aggregates, per weeks, the temperatures values of the whole month */
    	return aggregateTempByTimePeriod(durationPeriod, timeFormat); 
    }
    
    /* To call when THISYEAR and THISSEMESTER option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByMonths(Boolean isYear) {
    	String timeFormat = "MM"; /* Interested in getting the month of the date */
    	Integer durationPeriod = 0;
    	if (isYear) durationPeriod = 12; /* Here the function aggregates, per months, the temperatures values of the whole year */
    	else durationPeriod = 6; /* Here the function aggregates, per months, the temperatures values of the semester */
    	
    	return aggregateTempByTimePeriod(durationPeriod, timeFormat); 
    }
    
    private HashMap<String,Object> aggregateTempByTimePeriod(Integer durationPeriod,
    												   		 String timeFormat) {
    	HashMap<String,Object> tempList = new HashMap<String,Object>();
    	ArrayList<int[]> tempByTime = new ArrayList<int[]>();
    	tempByTime.ensureCapacity(durationPeriod);
    	HashMap<Integer, Date> dates = new HashMap<Integer, Date>();
    	tempList.put("arrayValues", tempByTime);
    	tempList.put("hashDates", dates);
    	
    	for (int i = 0; i < temperatures.size(); i++) {
    		Date tempDateTime = temperatures.get(i).getDatetime();
    		Integer tempVal = temperatures.get(i).getTemperature();
    		SimpleDateFormat dateFormat = new SimpleDateFormat(timeFormat);
    		Log.i(MainActivity.LOG, "DATE:" + dateFormat.format(tempDateTime));
    		Integer time = Integer.parseInt(dateFormat.format(tempDateTime));
    		int[] tempMeasure = tempByTime.get(time); 
    		if (tempMeasure == null) {
    			tempMeasure = new int[2];
    			tempMeasure[1] = 0;
    			dates.put(time, tempDateTime);
    		}
    		else {
    			tempMeasure[1] += 1;
    		}
    		tempMeasure[0] += tempVal; /* Total measures */
    		tempByTime.add(time, tempMeasure);
    	}
    	
    	return tempList;
    }
}
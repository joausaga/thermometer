package com.lemontruck.thermo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

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
	public final static int SAMEWEEKLASTYEAR = 5006;
	public final static int SAMEMONTHLASTYEAR = 5007;
	
	private Activity activity;
	private Context context;
	private int filter;
	private int spinnerCurrentPos;
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
		spinnerCurrentPos = 0;
		addListenerSpinner(view);
        setCurrentLocation(view);  /* Update Location Label */
        HashMap<String,String> tempValues = getTemperatureValues(); /* Get Temperature Statistics */
    	if (tempValues != null)
    		updateLayout(view, tempValues);
        return view;
    }

    private void addListenerSpinner(final View staView) {
    	final Spinner filterSp = (Spinner) staView.findViewById(R.id.filter);
    	filterSp.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int pos, long id) 
        	{
        		Resources res = context.getResources();
        		int newFilter = 0;
        		
        		if (pos == 0) newFilter = TODAY;
            	if (pos == 1) newFilter = THISWEEK;
            	if (pos == 2) newFilter = THISMONTH;
            	if (pos == 3) newFilter = THISSEMESTER;
            	if (pos == 4) newFilter = THISYEAR;
            	if (pos == 5) newFilter = SAMEDAYLASTYEAR;
            	if (pos == 6) newFilter = SAMEWEEKLASTYEAR;
            	if (pos == 7) newFilter = SAMEMONTHLASTYEAR;
            	
            	if (newFilter != filter) {
            		filter = newFilter;
            		HashMap<String,String> tempValues = getTemperatureValues(); /* Get Temperature Statistics */
                	if (tempValues != null) {
                		spinnerCurrentPos = pos;
            			ProgressDialog progressDialog = new ProgressDialog(activity);
                		progressDialog.setMessage(res.getString(R.string.loading_statistics_message));
                		progressDialog.show();
                		updateLayout(staView, tempValues);
                		progressDialog.dismiss();
                	}
                	else {
                		filterSp.setSelection(spinnerCurrentPos);
                	}
            	}
        		
        	}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				Log.i(MainActivity.LOG, "The user does not select any option");
			}
    	});
    }
    
    private void setCurrentLocation(View view) {
    	SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
    	Resources res = context.getResources();
    	
    	String currentCountry = settings.getString("country","Italy");
    	String currentCity = settings.getString("location","Trento");
    	
    	TextView location = (TextView) view.findViewById(R.id.label_location);
    	location.setText(res.getString(R.string.label_filter) + " " + currentCity + ", " + currentCountry);
    }
    
    private View updateLayout(View view, HashMap<String,String> tempValues) {
    	/* Update AVG Temperature */
    	TextView avgTemp = (TextView) view.findViewById(R.id.avg_temp);
    	avgTemp.setText(tempValues.get("avg")+"\u00B0");
    	/* Update MAX Temperature */
    	TextView maxTemp = (TextView) view.findViewById(R.id.max_temp);
    	maxTemp.setText(tempValues.get("max")+"\u00B0");
    	/* Update MIN Temperature */
    	TextView minTemp = (TextView) view.findViewById(R.id.min_temp);
    	minTemp.setText(tempValues.get("min")+"\u00B0");
    	view = populateTemperatureList(view);
    	
    	return view;
    }
    
    private View populateTemperatureList(View view) {
    	TableLayout staTable = (TableLayout) view.findViewById(R.id.statistic_table);
    	String header = "";
    	Resources res = context.getResources();
    	String tail = "";
    	
    	HashMap<String,Object> tempValues = null;
    	SimpleDateFormat dateFormat = null;
    	
    	switch(filter) {
    		case TODAY: case SAMEDAYLASTYEAR:
    			tempValues = aggregateTempByHours();
    			dateFormat = new SimpleDateFormat("HH");
    			header = res.getString(R.string.table_title_hour);
    			tail = ":00";
    			break;
    		case THISWEEK: case SAMEWEEKLASTYEAR:
    			tempValues = aggregateTempByDays();
    			dateFormat = new SimpleDateFormat("EEEE");
    			header = res.getString(R.string.table_title_day);
    			break;
    		case THISMONTH: case SAMEMONTHLASTYEAR:
    			tempValues = aggregateTempByWeeks();
    			dateFormat = new SimpleDateFormat("dd");
    			header = res.getString(R.string.table_title_date);
    			break;
    		case THISYEAR: case THISSEMESTER:
    			if (filter == THISYEAR) tempValues = aggregateTempByMonths(true);
    			else tempValues = aggregateTempByMonths(false);
    			dateFormat = new SimpleDateFormat("MMMM");
    			header = res.getString(R.string.table_title_month);
    			break;
    		default:
    			Log.e(MainActivity.LOG, "Error!, unkown filter type");
    			return view;
    	}
    	
    	HashMap<Integer, Date> dates = (HashMap<Integer, Date>) tempValues.get("hashDates");
		ArrayList<int[]> tempByTime = (ArrayList<int[]>) tempValues.get("arrayValues");
		staTable.removeAllViews();
		staTable = setTableHeaders(staTable,header);
		for (int i = 0; i < tempByTime.size(); i++) {
			if (tempByTime.get(i) != null) {
				if (tempByTime.get(i)[1] == 0) tempByTime.get(i)[1] = 1; //A ad-hoc hack to solve (temporally) bad registries in the DB  
				Integer avgTemp = tempByTime.get(i)[0] / tempByTime.get(i)[1];
				Date dateTemp = dates.get(i);
				staTable = addToStatisticTable(staTable, dateFormat.format(dateTemp), avgTemp.toString(), tail);
			}
		}
		
		return view;
    }
    
    private TableLayout setTableHeaders(TableLayout table, String dateColName) {
    	Resources res = context.getResources();
    	
    	TableRow row = new TableRow(context);
    	TableRow.LayoutParams rowParams = new TableRow.LayoutParams(); 
    	rowParams.gravity = Gravity.LEFT;
    	
    	TableRow.LayoutParams colParams = new TableRow.LayoutParams();
    	colParams.gravity = Gravity.CENTER;
    	TextView dateCol = new TextView(context);  
        dateCol.setText(dateColName);
        row.addView(dateCol, colParams);
        
        TableRow.LayoutParams colParams2 = new TableRow.LayoutParams();
        colParams2.gravity = Gravity.CENTER;
        TextView avgTempCol = new TextView(context);  
        avgTempCol.setText(res.getString(R.string.table_title_avg_temp));
        row.addView(avgTempCol, colParams2);
        
        table.addView(row, rowParams);
    	
    	return table;
    }
    
    private TableLayout addToStatisticTable(TableLayout table, String date, String avgTemp, String tail) {
    	TableRow row = new TableRow(context);
    	TableRow.LayoutParams rowParams = new TableRow.LayoutParams(); 
    	rowParams.gravity = Gravity.LEFT;
    	
    	TableRow.LayoutParams colParams = new TableRow.LayoutParams();
    	colParams.gravity = Gravity.CENTER;
    	TextView dateText = new TextView(context);  
        dateText.setText(date + tail);
        dateText.setTextColor(Color.WHITE);
        row.addView(dateText, colParams);
        
        TableRow.LayoutParams colParams2 = new TableRow.LayoutParams();
        colParams2.gravity = Gravity.CENTER;
        TextView avgTempText = new TextView(context);  
        avgTempText.setText(avgTemp+"\u00B0");
        avgTempText.setTextColor(Color.WHITE);
        row.addView(avgTempText, colParams2);
        
        table.addView(row, rowParams);
        
        return table; 
    }
    
    private void getStatistics() {    	
    	/* Get the current location (country and city) */
    	SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
    	String country = settings.getString("country","Italy");
    	String city = settings.getString("location","Trento");
    	
    	TemperatureDataSource datasource = new TemperatureDataSource(context);
		datasource.open();
		temperatures = datasource.getTemperatures(country, city, filter);
		//temperatures = datasource.getTemperatures(country, city, SAMEWEEKLASTYEAR);
		datasource.close();
    }
    
    private HashMap<String,String> getTemperatureValues() {
    	HashMap<String,String> tempValues = null;
    	
    	getStatistics();
    	if (!temperatures.isEmpty()) {
	    	tempValues = new HashMap<String,String>();
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
    	} else {
    		Toast.makeText(context, R.string.statistic_exception, Toast.LENGTH_LONG).show();
    	}
    	
    	return tempValues;
    }
    
    /* To call when TODAY and THESAMEDAYLASTYEAR option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByHours() {
    	String timeFormat = "HH"; /* Interested in getting the hour of the date */
    	Integer durationPeriod = 24; /* Here the function aggregates, per hours, the temperature values of the whole day */
    	return aggregateTempByTimePeriod(durationPeriod, timeFormat);
    }
    
    /* To call when THISWEEK and THESAMEWEEKLASTYEAR option is selected from the filter spinner */
    private HashMap<String,Object> aggregateTempByDays() {
    	String timeFormat = "dd"; /* Interested in getting the day of the date */
    	Integer durationPeriod = 7; /* Here the function aggregates, per days, the temperature values of the whole week */
    	return aggregateTempByTimePeriod(durationPeriod, timeFormat); 
    }
    
    /* To call when THISMONTH and THESAMEMONTLASTYEAR option is selected from the filter spinner */
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
    	tempByTime = reserveSpace(tempByTime, durationPeriod);
    	HashMap<Integer, Date> dates = new HashMap<Integer, Date>();
    	tempList.put("arrayValues", tempByTime);
    	tempList.put("hashDates", dates);
    	
    	for (int i = 0; i < temperatures.size(); i++) {
    		Date tempDateTime = temperatures.get(i).getDatetime();
    		Integer tempVal = temperatures.get(i).getTemperature();
    		SimpleDateFormat dateFormat = new SimpleDateFormat(timeFormat);
    		Integer time = Integer.parseInt(dateFormat.format(tempDateTime));
    		int[] tempMeasure = tempByTime.get(time-1); 
    		if (tempMeasure == null) {
    			Log.i(MainActivity.LOG, "New time");
    			tempMeasure = new int[2];
    			tempMeasure[1] = 1;
    			dates.put(time-1, tempDateTime);
    		}
    		else {
    			tempMeasure[1] += 1;
    		}
    		tempMeasure[0] += tempVal; /* Total measures */
    		tempByTime.set(time-1, tempMeasure);
    	}
		
    	return tempList;
    }

    private ArrayList<int[]> reserveSpace(ArrayList<int[]> array, Integer maxElements) {
    	for (int i = 0; i < maxElements; i++)
    		array.add(null);

    	return array;
    }
}
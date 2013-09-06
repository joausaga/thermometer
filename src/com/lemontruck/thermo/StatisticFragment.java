package com.lemontruck.thermo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.lemontruck.thermo.exceptions.LocationException;
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
	private static Context context;
	private static int filter;
	private static int spinnerCurrentPos;
	private static List<Temperature> temperatures;
	private static ProgressDialog progressDialog;
	
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
		Resources res = context.getResources();
		
		filter = TODAY;
		spinnerCurrentPos = 0;
		addListenerSpinner(view);
        setCurrentLocation(view);  /* Update Location Label */
        progressDialog = new ProgressDialog(activity);
		progressDialog.setMessage(res.getString(R.string.loading_statistics_message));
        getStatistics(view);
        
        return view;
    }

    private void addListenerSpinner(final View staView) {
    	final Spinner filterSp = (Spinner) staView.findViewById(R.id.filter);
    	filterSp.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int pos, long id) 
        	{
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
            		progressDialog.show();
                    getStatistics(staView);
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
    
    private static View updateLayout(View view, HashMap<String,String> tempValues,
    								 HashMap<String,Object> tempAggregated) {
    	/* Update AVG Temperature */
    	TextView avgTemp = (TextView) view.findViewById(R.id.avg_temp);
    	avgTemp.setText(tempValues.get("avg")+"\u00B0");
    	/* Update MAX Temperature */
    	TextView maxTemp = (TextView) view.findViewById(R.id.max_temp);
    	maxTemp.setText(tempValues.get("max")+"\u00B0");
    	/* Update MIN Temperature */
    	TextView minTemp = (TextView) view.findViewById(R.id.min_temp);
    	minTemp.setText(tempValues.get("min")+"\u00B0");
    	view = populateTemperatureList(view,tempAggregated);
    	
    	return view;
    }
    
    private static View populateTemperatureList(View view, HashMap<String,Object> tempValues) {
    	TableLayout staTable = (TableLayout) view.findViewById(R.id.statistic_table);
    	String header = "";
    	Resources res = context.getResources();
    	String tail = "";
    	
    	SimpleDateFormat dateFormat = null;
    	
    	switch(filter) {
    		case TODAY: case SAMEDAYLASTYEAR:
    			dateFormat = new SimpleDateFormat("HH");
    			header = res.getString(R.string.table_title_hour);
    			tail = ":00";
    			break;
    		case THISWEEK: case SAMEWEEKLASTYEAR:
    			dateFormat = new SimpleDateFormat("EEEE");
    			header = res.getString(R.string.table_title_day);
    			break;
    		case THISMONTH: case SAMEMONTHLASTYEAR:
    			dateFormat = new SimpleDateFormat("dd");
    			header = res.getString(R.string.table_title_date);
    			break;
    		case THISYEAR: case THISSEMESTER:
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
				if (tempByTime.get(i)[1] == 0) tempByTime.get(i)[1] = 1; //An ad-hoc hack to solve (temporally) bad records in the DB  
				Integer avgTemp = tempByTime.get(i)[0] / tempByTime.get(i)[1];
				Date dateTemp = dates.get(i);
				staTable = addToStatisticTable(staTable, dateFormat.format(dateTemp), avgTemp.toString(), tail);
			}
		}
		
		return view;
    }
    
    private static TableLayout setTableHeaders(TableLayout table, String dateColName) {
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
    
    private static TableLayout addToStatisticTable(TableLayout table, String date, String avgTemp, String tail) {
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
    
    private void getStatistics(View view) { 
    	// Try to retrieve the statistics
		Intent intent = new Intent(context, StatisticProvider.class);
	    // Create a new Messenger for the communication back
		UpdateHandler handler = new UpdateHandler(context, view);
	    Messenger messenger = new Messenger(handler);
	    intent.putExtra("MESSENGER", messenger);
	    intent.putExtra("FILTER", filter);
	    context.startService(intent);
    }
    
    private static HashMap<String,String> getTemperatureValues() {
    	HashMap<String,String> tempValues = null;
    	
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

    	return tempValues;
    }
    
    private static class UpdateHandler extends Handler {
		private Context context;
		private View view;
	
		public UpdateHandler(Context context, View view) {
			this.context = context;
			this.view = view;
		}
		
		public void handleMessage(Message message) {
			if (message.arg1 == Activity.RESULT_OK) {
				ArrayList<Object> ret = new ArrayList<Object>();
				ret = (ArrayList<Object>)message.obj;
				
				temperatures = (List<Temperature>) ret.get(0);
				Spinner filterSp = (Spinner) view.findViewById(R.id.filter);
				if (!temperatures.isEmpty()) {
					HashMap<String,Object> tempAggregated = (HashMap<String,Object>) ret.get(1); 
					HashMap<String,String> tempValues = getTemperatureValues(); /* Get Temperature Statistics */
					updateLayout(view, tempValues, tempAggregated);
					spinnerCurrentPos = filterSp.getSelectedItemPosition();
            		progressDialog.dismiss();
				}
				else {
					progressDialog.dismiss();
					filterSp.setSelection(spinnerCurrentPos);
					Toast.makeText(context, R.string.statistic_exception, Toast.LENGTH_LONG).show();
				}
			} 
			else {
				Exception e = (Exception) message.obj;
				Log.e(MainActivity.LOG, "Could get statistics, cause: " + e.getMessage());
			}
		}
	}
}
package com.lemontruck.thermo.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;
import android.util.Log;

import com.lemontruck.thermo.MainActivity;
import com.lemontruck.thermo.StatisticFragment;
import com.lemontruck.thermo.Temperature;


public class TemperatureDataSource {
	  private final static String LOG = "com.lemontruck.thermo";  
	  
	  // Database fields
	  private SQLiteDatabase database;
	  private MySQLiteHelper dbHelper;
	  private String[] allColumns = { MySQLiteHelper.COLUMN_ID, 
			  						  MySQLiteHelper.COLUMN_TEMP,
			  						  MySQLiteHelper.COLUMN_DATETIME,
			  						  MySQLiteHelper.COLUMN_COUNTRY,
			  						  MySQLiteHelper.COLUMN_CITY };
	  private Time now = new Time();
	  
	  public TemperatureDataSource(Context context) {
		  dbHelper = new MySQLiteHelper(context);
	  }

	  public void open() throws SQLException {
		  database = dbHelper.getWritableDatabase();
	  }

	  public void close() {
		  dbHelper.close();
	  }
	  
	  public long saveTemperature(String temperature, 
			  					  String country, 
			  					  String city) 
	  {
		  ContentValues values = new ContentValues();
		  values.put(MySQLiteHelper.COLUMN_TEMP, temperature);
		  values.put(MySQLiteHelper.COLUMN_COUNTRY, country);
		  values.put(MySQLiteHelper.COLUMN_CITY, city);
		  Date currentDateTime = new Date();
		  DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		  values.put(MySQLiteHelper.COLUMN_DATETIME, dateFormat.format(currentDateTime));
		  long insertId = database.insert(MySQLiteHelper.TABLE_TEMP, null,values);
		  
		  return insertId;
	  }
	  
	  public void deleteTemperature(Temperature temp) {
		  long id = temp.getId();
		  Log.i(LOG, "Temperature deleted with id: " + id);
		  database.delete(MySQLiteHelper.TABLE_TEMP, MySQLiteHelper.COLUMN_ID + 
				  									 " = " + id, null);
	  }
	  
	  public List<Temperature> getAllTemperatures() {
		  List<Temperature> temperatures = new ArrayList<Temperature>();

		  Cursor cursor = database.query(MySQLiteHelper.TABLE_TEMP,
				  					     allColumns, null, null, null, null, null);

		  cursor.moveToFirst();
		  while (!cursor.isAfterLast()) {
			  Temperature temp = cursorToTemperature(cursor);
			  temperatures.add(temp);
			  cursor.moveToNext();
		  }
		  
		  cursor.close();
		  return temperatures;
	  }
	  
	  public List<Temperature> getTemperatures(String country, String city, int filter) {
		  List<Temperature> temperatures = new ArrayList<Temperature>();
		  Cursor cursor = null;
		  String startingTime = "";
		  String endingTime = "";
		  SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		  Calendar cal = Calendar.getInstance();
		  
		  if (filter != StatisticFragment.SAMEDAYLASTYEAR &&
			  filter != StatisticFragment.SAMEMONTHLASTYEAR &&
			  filter != StatisticFragment.SAMEWEEKLASTYEAR) 
		  {
			  endingTime = formatter.format(cal.getTime()); // NOW
			  if (filter == StatisticFragment.THISWEEK) {
				  Integer currentWeek =  cal.get(Calendar.WEEK_OF_YEAR); 
				  Integer currentYear =  cal.get(Calendar.YEAR);
				  cal.clear();
				  /* Point the calendar to the first day of the current week */
				  cal.set(Calendar.WEEK_OF_YEAR, currentWeek);
				  cal.set(Calendar.YEAR, currentYear);  
			  } 
			  if (filter == StatisticFragment.THISMONTH) {
				  /* Point the calendar to the first day of the current month */
				  cal.set(Calendar.DAY_OF_MONTH, 1);
			  }
			  if (filter == StatisticFragment.THISYEAR) {
				  /* Point the calendar to the first day of the current year */
				  cal.set(Calendar.DAY_OF_YEAR, 1);  
			  }
			  if (filter == StatisticFragment.THISSEMESTER) {
				  Integer numMonth = cal.get(Calendar.MONTH);;
				  /* Point the calendar to the first day of the current semester */
				  if (numMonth <= 6) {
					  cal.set(Calendar.DAY_OF_YEAR, 1);
				  }
				  else {
					  cal.set(Calendar.MONTH, Calendar.JULY);
					  cal.set(Calendar.DAY_OF_MONTH, 1);
				  }
			  }
		  } else {
			  Integer currentYear =  cal.get(Calendar.YEAR);
			  if (filter == StatisticFragment.SAMEMONTHLASTYEAR) {
				  /* LAST YEAR */
				  cal.set(Calendar.YEAR, currentYear-1);
				  Integer lastDayCurrentMonth = getLastDayMonth(cal);
				  cal.set(Calendar.DAY_OF_MONTH, lastDayCurrentMonth);
				  cal.set(Calendar.MINUTE, 59);
				  cal.set(Calendar.HOUR_OF_DAY, 23);
				  cal.set(Calendar.SECOND, 59);
				  endingTime = formatter.format(cal.getTime());
				  cal.set(Calendar.DAY_OF_MONTH, 1);
			  }
			  if (filter == StatisticFragment.SAMEWEEKLASTYEAR) {
				  Integer currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
				  /* Point the calendar to the last day and hour of the week */
				  cal.clear();
				  cal.set(Calendar.YEAR, currentYear-1);
				  cal.set(Calendar.WEEK_OF_YEAR, currentWeek+1);
				  cal.add(Calendar.DATE, -1);
				  cal.set(Calendar.MINUTE, 59);
				  cal.set(Calendar.HOUR_OF_DAY, 23);
				  cal.set(Calendar.SECOND, 59);
				  endingTime = formatter.format(cal.getTime());
				  /* Point the calendar to the first day of the week */
				  cal.clear();
				  cal.set(Calendar.YEAR, currentYear-1);
				  cal.set(Calendar.WEEK_OF_YEAR, currentWeek);
			  }
			  if (filter == StatisticFragment.SAMEDAYLASTYEAR) {
				  /* LAST YEAR */
				  cal.set(Calendar.YEAR, currentYear-1);
				  cal.set(Calendar.MINUTE, 59);
				  cal.set(Calendar.HOUR_OF_DAY, 23);
				  cal.set(Calendar.SECOND, 59);
				  endingTime = formatter.format(cal.getTime());
			  }
		  }
		  
		  /* Point the calendar to midnight */
		  cal.set(Calendar.MINUTE, 0);
		  cal.set(Calendar.HOUR_OF_DAY, 0);
		  cal.set(Calendar.SECOND, 0);
		  //formatter.applyPattern("yyyy/MM/dd HH:mm:ss");
		  startingTime = formatter.format(cal.getTime());
		  
		  Log.i(MainActivity.LOG, "Getting statistics from " + startingTime + " to " + endingTime);
		  
		  String[] args = {country,city,startingTime,endingTime};
		  String whereClause = MySQLiteHelper.COLUMN_COUNTRY + "=? AND " + MySQLiteHelper.COLUMN_CITY + "=? AND " +
 				  			   MySQLiteHelper.COLUMN_DATETIME + " BETWEEN ? AND ?";
		  String orderBy = MySQLiteHelper.COLUMN_DATETIME;
		  cursor = database.query(MySQLiteHelper.TABLE_TEMP,
			     				  allColumns, 
			     				  whereClause, 
			     				  args, null, null, orderBy);
		  
		  cursor.moveToFirst();
		  while (!cursor.isAfterLast()) {
			  Temperature temp = cursorToTemperature(cursor);
			  temperatures.add(temp);
			  cursor.moveToNext();
		  }
		  
		  cursor.close();

		  return temperatures;
	  }
	  
	  private Temperature cursorToTemperature(Cursor cursor) {
		  Temperature temp = new Temperature();
		  temp.setId(cursor.getLong(0));
		  temp.setTemperature(Integer.parseInt(cursor.getString(1)));
		  temp.setDatetime(cursor.getString(2));
		  temp.setCountry(cursor.getString(3));
		  temp.setCity(cursor.getString(4));
		  return temp;
	  }
	  
	  private Integer getLastDayMonth(Calendar cal) {
		  cal.add(Calendar.MONTH, 1);  
	      cal.set(Calendar.DAY_OF_MONTH, 1);  
	      cal.add(Calendar.DATE, -1);
	      return cal.get(Calendar.DATE);
	  }
}

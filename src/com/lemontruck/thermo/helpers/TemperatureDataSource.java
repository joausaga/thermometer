package com.lemontruck.thermo.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;
import android.util.Log;

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
		  String timeCondition = "";
		  Date date = new Date();
		  if (filter == StatisticFragment.TODAY) timeCondition = "datetime('now','start of day')";
		  if (filter == StatisticFragment.THISWEEK) {
			  SimpleDateFormat formatter = new SimpleDateFormat("u");
			  Integer numDay = Integer.parseInt(formatter.format(date));
			  timeCondition = "datetime('now','-" + numDay  + " days')";
		  }
		  if (filter == StatisticFragment.THISMONTH) timeCondition = "datetime('now','start of month')";
		  if (filter == StatisticFragment.THISYEAR) timeCondition = "datetime('now','start of year')";
		  if (filter == StatisticFragment.THISSEMESTER) {
			  SimpleDateFormat formatter = new SimpleDateFormat("MM");
			  Integer numMonth = Integer.parseInt(formatter.format(date));
			  if (numMonth <= 6) {
				  timeCondition = "datetime('now','-"+ numMonth +" months')";
			  }
			  else {
				  numMonth = numMonth - 6;
				  timeCondition = "datetime('now','-"+ numMonth +" months')";
			  }
		  }
		  if (filter == StatisticFragment.SAMEDAYLASTYEAR) timeCondition = "datetime('now','-365 days')";
		  
		  now.setToNow();
		  String[] args = {country,city,now.toString()};
		  String whereClause = MySQLiteHelper.COLUMN_COUNTRY + "=? AND " + MySQLiteHelper.COLUMN_CITY + "=? AND " +
 				  			   MySQLiteHelper.COLUMN_DATETIME + " BETWEEN " + timeCondition + " AND ?";
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
}

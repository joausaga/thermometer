package com.lemontruck.thermo.helpers;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;
import android.util.Log;

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
		  now.setToNow();
		  values.put(MySQLiteHelper.COLUMN_DATETIME, now.toString());
		  long insertId = database.insert(MySQLiteHelper.TABLE_TEMP, null,values);
		  
		  return insertId;
	  }
	  
	  public void deleteComment(Temperature temp) {
		  long id = temp.getId();
		  Log.i(LOG, "Temperature deleted with id: " + id);
		  database.delete(MySQLiteHelper.TABLE_TEMP, MySQLiteHelper.COLUMN_ID + 
				  									 " = " + id, null);
	  }
	  
	  public List<Temperature> getAllComments() {
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

package com.lemontruck.thermo.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
	public static final String TABLE_TEMP = "temperatures";
	
	/* Database schema */
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TEMP = "temperature";
	public static final String COLUMN_DATETIME = "datetime";
	public static final String COLUMN_COUNTRY = "country";
	public static final String COLUMN_CITY = "city";
	
	private static final String DATABASE_NAME = "temperatures.db";
	private static final int DATABASE_VERSION = 1;
	
	/* Database creation sql statement */
	private static final String DATABASE_CREATE = "create table " + 
												  TABLE_TEMP + "(" + 
												  COLUMN_ID + 
												  " integer primary key autoincrement, " + 
												  COLUMN_TEMP + " integer not null, " +
												  COLUMN_DATETIME + " text not null, " +
												  COLUMN_COUNTRY + " text not null, " +
												  COLUMN_CITY + " text not null);";

	  public MySQLiteHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	  }
	
	@Override
	  public void onCreate(SQLiteDatabase database) {
	    database.execSQL(DATABASE_CREATE);
	  }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + 
											  oldVersion + " to " + 
											  newVersion + 
											  ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEMP);
		onCreate(db);
  }

}

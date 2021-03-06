package com.lemontruck.thermo;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
 
public class ThermoDialog extends Activity {
	private final static String LOG = "com.lemontruck.thermo";
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showdialog();
    }
	
    private void showdialog()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title);
        
        builder.setItems(R.array.dialog_options, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		Context context = getApplicationContext();
        		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        		if (which == 0) { /* Force widget update */
    				//ThermoWidget.forceUpdate(context, false);
    				/*int[] ids = man.getAppWidgetIds(new ComponentName(context,ThermoWidget.class));
    			    Intent updateIntent = new Intent();
    			    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    			    updateIntent.putExtra(ThermoWidget.WIDGET_ID_KEY, ids);
    			    context.sendBroadcast(updateIntent);*/
    			}
    			if (which == 1) {  /* Visit the source web page */
    				Resources res = context.getResources();
    				String webInfoSource = res.getString(R.string.web_info_source);
    				Uri url = Uri.parse(webInfoSource);
    		        Intent intent = new Intent(Intent.ACTION_VIEW, url);
    		        startActivity(intent);
    			}
    			if (which == 2) {
    				int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context,ThermoWidget.class));
    				Intent intent = new Intent();
    			    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[0]); //TODO Configure just the first instance created
    			    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				intent.setComponent(new ComponentName("com.lemontruck.thermo","com.lemontruck.thermo.ConfigActivity"));
    				startActivity(intent);
    			}
    			finish();
            }
        });
    
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

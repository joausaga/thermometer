package com.lemontruck.thermo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.lemontruck.thermo.exceptions.ApiException;
import com.lemontruck.thermo.exceptions.ParseException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class WeatherInfoHelper{
	
	private final static String LOG = "com.lemontruck.thermo";
	
	/**
     * {@link StatusLine} HTTP status code when no server error has occurred.
     */
    private static final int HTTP_STATUS_OK = 200;
	
    /**
     * Shared buffer used by {@link #getUrlContent(String)} when reading results
     * from an API request.
     */
    private static byte[] sBuffer = new byte[512];
    
    /**
     * User-agent string to use when making requests. Should be filled using
     * {@link #prepareUserAgent(Context)} before making any other calls.
     */
    private static String sUserAgent = null;
    
    /**
     * Prepare the internal User-Agent string for use. This requires a
     * {@link Context} to pull the package name and version number for this
     * application.
     */
    public static void prepareUserAgent(Context context) {
        try {
            // Read package name and version number from manifest
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            sUserAgent = String.format(context.getString(R.string.user_agent),
                    				   info.packageName, info.versionName);
        } catch(NameNotFoundException e) {
            Log.e(LOG, "Couldn't find package information in PackageManager", e);
        }
    }
    
    /**
     * Read and return the weather information of the location passed as a parameter.
     * 
     * @param idLocation The id of HTML tag element that contains the current temperature of the 
     * location.
     * @param idLastUpdated The id of the HTML tag element that contains data about the last time
     * the temperature information was updated.
     * @return the current temperature at the location as well as the .
     * @throws ApiException If any connection or server error occurs.
     * @throws ParseException If there are problems parsing the response.
     */
    public static HashMap<String,String> getWeatherInfo(String sourceInfoURL, String idLocation, 
    													String idLastUpdated)
    throws ApiException, ParseException {
    	HashMap<String,String> currentTempInfo = new HashMap<String,String>();
    	
    	// Query the API for content
        String content = getUrlContent(sourceInfoURL);
        Document doc = Jsoup.parse(content);
        Element contentTemp = doc.getElementById(idLocation);
		Element contentLastUpdated = doc.getElementById(idLastUpdated);
		currentTempInfo.put("temp", contentTemp.text().split(",")[0]);
		currentTempInfo.put("last_updated", contentLastUpdated.text().split(" ")[1]);
		
    	return currentTempInfo;
    }
	
    /**
     * Pull the raw text content of the given URL. This call blocks until the
     * operation has completed, and is synchronized because it uses a shared
     * buffer {@link #sBuffer}.
     * 
     * @param url The exact URL to request.
     * @return The raw content returned by the server.
     * @throws ApiException If any connection or server error occurs.
     */
    protected static synchronized String getUrlContent(String url) throws ApiException {
        if (sUserAgent == null) {
            throw new ApiException("User-Agent string must be prepared");
        }
        
        // Create client and set our specific user-agent string
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", sUserAgent);

        try {
            HttpResponse response = client.execute(request);
            
            // Check if server response is valid
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HTTP_STATUS_OK) {
                throw new ApiException("Invalid response from server: " +
                        				status.toString());
            }
    
            // Pull content stream from response
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            
            // Read response into a buffered stream
            int readBytes = 0;
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            
            // Return result from buffered stream
            return new String(content.toByteArray());
        } catch (IOException e) {
            throw new ApiException("Problem communicating with API", e);
        }
    }
}

package com.example.cory1.privlocapp;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LocationQuery extends AsyncTask<String, Void, String[]>{

    private final String LOG_TAG = LocationQuery.class.getSimpleName();

    @Override
    protected String[] doInBackground(String... params) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String locationJsonStr = null;
        String lat = "37.4528876614";
        String lng = "-122.181977257";
        lat = params[0];
        lng = params[1];

        try {
            // Construct the URL for the query
            final String BASE_URL = "https://newprivlocdemo.appspot.com/within_location?";
            final String LAT_PARAM = "lat";
            final String LNG_PARAM = "lng";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(LAT_PARAM, lat)
                    .appendQueryParameter(LNG_PARAM, lng)
                    .build();

            URL url = new URL(builtUri.toString());
            //URL url = new URL("https://newprivlocdemo.appspot.com/within_location?lat=&lng=");

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //URL url = new URL("https://newprivlocdemo.appspot.com/within_location?lat=&lng=");

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            locationJsonStr = buffer.toString();
            //Log.v(LOG_TAG, "Location: " + locationJsonStr);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        try {
            return getLocationDataFromJson(locationJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    private String[] getLocationDataFromJson(String locationJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String LOC = "locations";
        Map<String, String> LOC_KEYS = new HashMap<String, String>();
        LOC_KEYS.put("LOC_KEY", "key");
        LOC_KEYS.put("LOC_ACT", "location_active");
        LOC_KEYS.put("DESC_NAME", "descriptive_name");
        LOC_KEYS.put("MAX_LNG", "max_longitude");
        LOC_KEYS.put("MIN_LNG", "min_longitude");
        LOC_KEYS.put("MAX_LAT", "max_latitude");
        LOC_KEYS.put("MIN_LAT", "min_latitude");
        LOC_KEYS.put("CAMERA_ACT", "camera_active");
        LOC_KEYS.put("CAMERA_MSG", "camera_message");
        LOC_KEYS.put("VIDEO_ACT", "video_active");
        LOC_KEYS.put("VIDEO_MSG", "video_message");

        //create the hash map of hash maps representing each location
        JSONObject locationJson = new JSONObject(locationJsonStr);
        if (locationJson.isNull(LOC)) {
            String[] nullResult = {"Not within a location"};
            return nullResult;
        } else {
            JSONObject locations = locationJson.getJSONObject(LOC);
            Iterator<?> keys = locations.keys();
            Map<String, Map<String, String>> allLocations = new HashMap<String, Map<String, String>>();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (locations.get(key) instanceof JSONObject) {
                    Map<String, String> descDict = new HashMap<String, String>();
                    for (Map.Entry<String, String> entry : LOC_KEYS.entrySet()) {
                        String value = ((JSONObject) locations.get(key))
                                .getString(LOC_KEYS.get(entry.getKey().toString()));
                        descDict.put(entry.getKey(), value);
                    }
                    allLocations.put(key, descDict);
                }
            }

            String[] resultStrs = makeLocationList(allLocations);
            return resultStrs;
        }
    }

    private String[] makeLocationList(Map<String, Map<String, String>> allLocations) {
        String[] resultStrs = new String[allLocations.size()];
        Iterator<?> hashKeys = allLocations.keySet().iterator();
        int i = 0;
        while (hashKeys.hasNext()) {
            String key = (String)hashKeys.next();
            StringBuilder thisLocation = new StringBuilder();
            thisLocation.append("Location Name: ");
            thisLocation.append(allLocations.get(key).get("DESC_NAME"));
            thisLocation.append("\n");
            thisLocation.append("Camera Active: ");
            thisLocation.append(allLocations.get(key).get("CAMERA_ACT"));
            resultStrs[i] = thisLocation.toString();
            thisLocation.append("\n");
            thisLocation.append("Camera Message: ");
            thisLocation.append(allLocations.get(key).get("CAMERA_MSG"));
            resultStrs[i] = thisLocation.toString();
            thisLocation.append("\n");
            thisLocation.append("Video Active: ");
            thisLocation.append(allLocations.get(key).get("VIDEO_ACT"));
            resultStrs[i] = thisLocation.toString();
            thisLocation.append("\n");
            thisLocation.append("Video Message: ");
            thisLocation.append(allLocations.get(key).get("VIDEO_MSG"));
            resultStrs[i] = thisLocation.toString();
            i++;
        }
        return resultStrs;
    }

    @Override
    protected void onPostExecute(String[] result) {
        Log.v(LOG_TAG, result[0]);
    }
}
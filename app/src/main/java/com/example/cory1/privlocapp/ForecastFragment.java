/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cory1.privlocapp;

import android.app.Fragment;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_my, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
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
            JSONObject locations = locationJson.getJSONObject(LOC);
            Iterator<?> keys = locations.keys();
            Map<String, Map<String, String>> allLocations = new HashMap<String, Map<String, String>>();
            while(keys.hasNext()) {
                String key = (String)keys.next();
                if(locations.get(key) instanceof JSONObject) {
                    Map<String, String> descDict = new HashMap<String, String>();
                    for (Map.Entry<String, String> entry : LOC_KEYS.entrySet()) {
                        String value = ((JSONObject) locations.get(key))
                                .getString(LOC_KEYS.get(entry.getKey().toString()));
                        descDict.put(entry.getKey(), value);
                    }
                    allLocations.put(key, descDict);
                }
            }
            String[] resultStrs = new String[allLocations.size()];
            Iterator<?> hashKeys = allLocations.keySet().iterator();
            int i = 0;
            while (hashKeys.hasNext()) {
                String key = (String)hashKeys.next();
                StringBuilder thisLocation = new StringBuilder();
                thisLocation.append(allLocations.get(key).get("DESC_NAME"));
                resultStrs[i] = thisLocation.toString();
                i++;
            }
            return resultStrs;
        }

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

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for (String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}
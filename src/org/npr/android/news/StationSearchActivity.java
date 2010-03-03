// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.npr.android.news;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.StationSearchEvent;
import org.npr.api.ApiConstants;

import java.util.HashMap;
import java.util.Map;

public class StationSearchActivity extends Activity implements
    OnClickListener, OnCheckedChangeListener {
  private static final String LOG_TAG = StationSearchActivity.class.getName();
  private EditText searchParam;
  private Button searchNow;
  private RadioGroup searchByGroup;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.station_search);

    searchParam = (EditText) findViewById(R.id.StationSearchParamText);
    searchParam.setEnabled(false);

    searchByGroup = (RadioGroup) findViewById(R.id.RadioGroup01);
    searchByGroup.setOnCheckedChangeListener(this);

    searchNow = (Button) findViewById(R.id.StationSearchNowButton);
    searchNow.setEnabled(false);
    searchNow.setOnClickListener(this);
    Tracker.instance(getApplication()).trackLink(new StationSearchEvent());
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.StationSearchNowButton:
        Map<String, String> params = new HashMap<String, String>();
        String query = null;
        switch (searchByGroup.getCheckedRadioButtonId()) {
          case R.id.RadioButton01:
            query = populateLocalStationParams(params);
            break;
          case R.id.RadioButton02:
            query = this.searchParam.getText().toString().trim();
            if (query.length() == 4) {
              // Assume call letters
              params.put(ApiConstants.PARAM_CALL_LETTERS, query);
            } else if (query.length() == 5) {
              // Assume zip code
              params.put(ApiConstants.PARAM_ZIP, query);
            } else {

            }
            break;
        }
        String url =
            ApiConstants.instance().createUrl(ApiConstants.STATIONS_PATH,
                params);

        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_QUERY_URL, url);
        result.putExtra(Constants.EXTRA_QUERY_TERM, query);
        setResult(Activity.RESULT_OK, result);
        finish();
        break;
    }
  }

  private String populateLocalStationParams(Map<String, String> params) {
    String query = null;
    Log.d(LOG_TAG, "finding local stations");
    LocationManager lm =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    Location location = null;

    // Order here matters. This will search the network provider first, which is
    // more likely to be up to date. Since we are not actively polling, this is
    // more resilient to changes (e.g. going on an airplane and touching down
    // and not firing up the GPS again before running this app).
    String[] providers =
        {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER};
    for (String provider : providers) {
      Location loc = lm.getLastKnownLocation(provider);
      if (loc != null) {
        location = loc;
        break;
      }
    }

    if (location != null) {
      double lat = location.getLatitude();
      double lon = location.getLongitude();

      params.put(ApiConstants.PARAM_LAT, new Double(lat).toString());
      params.put(ApiConstants.PARAM_LON, new Double(lon).toString());
      query = String.format("%f,%f", lat, lon);
    }
    
    return query;
  }

  @Override
  public void onCheckedChanged(RadioGroup group, int checkedId) {
    searchNow.setEnabled(true);
    if (checkedId == R.id.RadioButton02) {
      searchParam.setVisibility(View.VISIBLE);
      searchParam.setEnabled(true);
    } else {
      searchParam.setVisibility(View.GONE);
    }
  }
}

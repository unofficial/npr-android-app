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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.StationListMeasurement;
import org.npr.api.Station;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationListActivity extends PlayerActivity implements
    OnItemClickListener, OnClickListener {

  public static final String EXTRA_STATION_SEARCH_URL =
      "extra_station_search_url";
  private static Map<String, Station> stationCache =
      new HashMap<String, Station>();
  private String query;

  public static Station getStationFromCache(String stationId) {
    Station result = stationCache.get(stationId);
    if (result == null) {
      result = Station.StationFactory.downloadStation(stationId);
      stationCache.put(stationId, result);
    }
    return result;
  }

  public static void addAllToStationCache(List<Station> stations) {
    for (Station station : stations) {
      stationCache.put(station.getId(), station);
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    listAdapter.clear();
    stationCache.clear();
  }

  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      listAdapter.showData();
    }
  };
  private StationListAdapter listAdapter;
  private Thread listInitThread;

  private void initializeList() {
    listInitThread.start();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK) {
      final String url = data.getStringExtra(Constants.EXTRA_QUERY_URL);
      query = data.getStringExtra(Constants.EXTRA_QUERY_TERM);
      listInitThread = new Thread(new Runnable() {
        public void run() {
          listAdapter.initializeList(url);
          handler.sendEmptyMessage(0);
        }
      });
      initializeList();
      trackNow();
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.StationSearchButton01:
        Intent i = new Intent(this, StationSearchActivity.class);
        startActivityForResult(i, 0);
        break;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.station_list, container);
    final ListView lv = (ListView) findViewById(R.id.ListView01);
    final Button searchNow = (Button) findViewById(R.id.StationSearchButton01);
    searchNow.setOnClickListener(this);
    listAdapter =
        new StationListAdapter(this, R.layout.station_item,
            R.id.StationItemNameText);
    lv.setAdapter(listAdapter);
    lv.setOnItemClickListener(StationListActivity.this);

    Intent i = new Intent(this, StationSearchActivity.class);
    startActivityForResult(i, 0);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Station station = (Station) parent.getItemAtPosition(position);

    Intent i =
      new Intent(this, StationDetailsActivity.class).putExtra(
          Constants.EXTRA_STATION_ID, station.getId());

    startActivityWithoutAnimation(i);
  }

  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_main_subactivity_stations);
  }

  @Override
  public void trackNow() {
    if (listAdapter == null) {
      return;
    }
    StringBuilder pageName =
        new StringBuilder("Stations").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append("Search Results");
    Tracker.instance(getApplication()).trackPage(
        new StationListMeasurement(pageName.toString(), "Stations", query));
  }
}

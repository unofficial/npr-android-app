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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.StationDetailsMeasurement;
import org.npr.api.Station;
import org.npr.api.Station.AudioStream;
import org.npr.api.Station.Listenable;
import org.npr.api.Station.Podcast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class StationDetailsActivity extends BackAndForthActivity implements
    OnChildClickListener {
  private String stationId;
  private Station station;
  private ImageView imageView;
  private ImageView iconView;
  private Drawable imageDrawable;

  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0:
          if (imageDrawable != null) {
            iconView.setVisibility(View.GONE);
            imageView.setImageDrawable(imageDrawable);
            imageView.setVisibility(View.VISIBLE);
          }
          break;
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    stationId = getIntent().getStringExtra(Constants.EXTRA_STATION_ID);
    station = StationListActivity.getStationFromCache(stationId);

    if (station == null) {
      finish();
    }

    setContentView(R.layout.station_details);
    TextView miscText = (TextView) findViewById(R.id.StationMiscText);
    TextView taglineText = (TextView) findViewById(R.id.StationTaglineText);
    miscText.setText(new StringBuilder().append(station.getFrequency()).append(
        " ").append(station.getBand()).append(", ").append(
        station.getMarketCity()));
    taglineText.setText(station.getTagline());

    iconView = (ImageView) findViewById(R.id.StationDetailsIcon);
    imageView = (ImageView) findViewById(R.id.StationDetailsImage);
    final String image = station.getImage();
    if (image != null) {
      Thread imageInitThread = new Thread(new Runnable() {
        public void run() {
          imageDrawable = DownloadDrawable.createFromUrl(image);
          handler.sendEmptyMessage(0);
        }
      });
      imageInitThread.start();
    }
    constructList();
  }

  @SuppressWarnings("unchecked")
  private void constructList() {
    int[] topLevel = new int[] { R.string.msg_station_streams,
        R.string.msg_station_podcasts };
    List<AudioStream> streams = station.getAudioStreams();
    List<Podcast> podcasts = station.getPodcasts();

    int groupLayout = android.R.layout.simple_expandable_list_item_1;
    int childLayout = android.R.layout.simple_expandable_list_item_2;

    String[] groupFrom = new String[]{"title"};
    int[] groupTo = new int[]{ android.R.id.text1 };

    List<Map<String, String>> groupData =
        new ArrayList<Map<String, String>>();

    String[] childFrom = new String[]{"title", "num"};
    int[] childTo = groupTo;

    List<List<Map<String, String>>> childData =
        new ArrayList<List<Map<String, String>>>();
    List<Map<String, String>> children = new ArrayList<Map<String, String>>();

    // Streams
    if (streams.size() > 0) {
      Map<String, String> group1 = new HashMap<String, String>();
      group1.put(groupFrom[0], String.format("%s (%d)", getString(topLevel[0]),
          streams.size()));
      groupData.add(group1);
      for (Iterator<AudioStream> it = streams.iterator(); it.hasNext();) {
        AudioStream stream = it.next();
        int i = ((ListIterator) it).previousIndex();
        Map<String, String> curChildMap = new HashMap<String, String>();
        children.add(curChildMap);
        curChildMap.put(childFrom[0], stream.getTitle());
        curChildMap.put(childFrom[1], "" + i);
      }
      childData.add(children);
    }

    // Podcasts
    if (podcasts.size() > 0) {
      Map<String, String> group2 = new HashMap<String, String>();
      group2.put(groupFrom[0], String.format("%s (%d)", getString(topLevel[1]),
          podcasts.size()));
      groupData.add(group2);

      children = new ArrayList<Map<String, String>>();
      for (Iterator<Podcast> it = podcasts.iterator(); it.hasNext(); ) {
        Podcast podcast = it.next();
        int i = ((ListIterator) it).previousIndex();
        Map<String, String> curChildMap =
            new HashMap<String, String>();
        children.add(curChildMap);
        curChildMap.put(childFrom[0], podcast.getTitle());
        curChildMap.put(childFrom[1], "" + i);
      }
      childData.add(children);
    }

    SimpleExpandableListAdapter listAdapter = new SimpleExpandableListAdapter(
        this,
        groupData,
        groupLayout,
        groupFrom,
        groupTo,
        childData,
        childLayout,
        childFrom,
        childTo);
    ExpandableListView list =
        (ExpandableListView) findViewById(R.id.ExpandableListView01);
    list.setAdapter(listAdapter);
    list.setOnChildClickListener(this);
    // Expand first group if present and it has something to show
    if (listAdapter.getGroupCount() > 0) {
      if (listAdapter.getChildrenCount(0) > 0) {
        list.expandGroup(0);
      }
    }
  }

  @Override
  public CharSequence getMainTitle() {
    return station.getName();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean onChildClick(ExpandableListView parent, View v,
      int groupPosition, int childPosition, long id) {
    Map<String, String> map =
        (Map<String, String>) parent.getExpandableListAdapter().getChild(
            groupPosition, childPosition);
    int num = Integer.parseInt(map.get("num"));
    Listenable l;
    Intent i;
    switch(groupPosition) {
      case 0:
        l = station.getAudioStreams().get(num);
        i = new Intent(ListenActivity.class.getName()).putExtra(
                ListenActivity.EXTRA_CONTENT_URL, l.getUrl()).putExtra(
                ListenActivity.EXTRA_CONTENT_TITLE, l.getTitle()).putExtra(
                ListenActivity.EXTRA_PLAY_IMMEDIATELY, true).putExtra(
                ListenActivity.EXTRA_STREAM, true);
        sendBroadcast(i);
        break;
      case 1:
        l = station.getPodcasts().get(num);
        i = new Intent(this, PodcastActivity.class).putExtra(
                PodcastActivity.EXTRA_PODCAST_TITLE, l.getTitle()).putExtra(
                PodcastActivity.EXTRA_PODCAST_URL, l.getUrl());
        startActivity(i);
        break;
    }

    return true;
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("Stations").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append(getMainTitle());
    Tracker.instance(getApplication()).trackPage(
        new StationDetailsMeasurement(pageName.toString(), "Stations",
            stationId));
  }
}

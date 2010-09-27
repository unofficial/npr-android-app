// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.npr.android.news;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import org.npr.android.util.FileUtils;
import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.ActivityMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.Podcast;
import org.npr.api.Podcast.Item;
import org.npr.api.Podcast.PodcastFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends PlayerActivity implements OnItemClickListener {

  private static final String LOG_TAG = Main.class.getName();

  private class SubActivity {
    private final Intent startIntent;

    private SubActivity(Intent startIntent) {
      this.startIntent = startIntent;
    }

    @Override
    public String toString() {
      return Main.this.getString(startIntent.getIntExtra(
          Constants.EXTRA_SUBACTIVITY_ID, -1));
    }
  }

  private ListView listView;
  private String hourlyTitle;
  private String hourlyGuid;
  private String hourlyURL;

  private static final int MSG_ERROR_MESSAGE = 0;
  private static final int MSG_PLAY_HOURLY = 1;
  private static final int MSG_CANCEL_LOCATION_LISTENERS = 2;
  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case MSG_ERROR_MESSAGE:
        errorMessage();
        break;
      case MSG_PLAY_HOURLY:
        playHourly();
        break;
      case MSG_CANCEL_LOCATION_LISTENERS:
        cancelLocationListeners();
        break;
      }
    }
  };

  // This is public so that we can inspect if for testing
  public List<LocationListener> locationListeners =
      new ArrayList<LocationListener>();

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    lauchLocationListeners();
    initApiKey();

    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.main_inner, container);

    listView = (ListView) findViewById(R.id.MainListView);
    String grouping = null;
    String description = "Top Stories";
    String topicId = "1002";
    Map<String, String> params = new HashMap<String, String>();
    params.put("id", topicId);
    params.put("fields", ApiConstants.STORY_FIELDS);
    params.put("sort", "assigned");

    String newsUrl = ApiConstants.instance().createUrl(ApiConstants.STORY_PATH,
        params);
    final SubActivity[] activities = {
        new SubActivity(new Intent(this, NewsListActivity.class).putExtra(
            Constants.EXTRA_SUBACTIVITY_ID, R.string.msg_main_subactivity_news)
            .putExtra(Constants.EXTRA_QUERY_URL, newsUrl)
            .putExtra(Constants.EXTRA_DESCRIPTION, description)
            .putExtra(Constants.EXTRA_GROUPING, grouping)
            .putExtra(Constants.EXTRA_SIZE, 5)),
        new SubActivity(new Intent(this, Main.class).putExtra(
            Constants.EXTRA_SUBACTIVITY_ID,
            R.string.msg_main_subactivity_hourly)),
        new SubActivity(new Intent(this, NewsTopicActivity.class).putExtra(
            Constants.EXTRA_SUBACTIVITY_ID,
            R.string.msg_main_subactivity_topics)),
        new SubActivity(new Intent(this, NewsTopicActivity.class).putExtra(
            Constants.EXTRA_SUBACTIVITY_ID,
            R.string.msg_main_subactivity_programs)),
        new SubActivity(new Intent(this, StationListActivity.class).putExtra(
            Constants.EXTRA_SUBACTIVITY_ID,
            R.string.msg_main_subactivity_stations)),
        new SubActivity(new Intent(this, SearchActivity.class).putExtra(
            Constants.EXTRA_SUBACTIVITY_ID,
            R.string.msg_main_subactivity_search)) };
    listView.setAdapter(new MainListAdapter(activities));
    listView.setOnItemClickListener(this);
    trackNow();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    SubActivity s = (SubActivity) parent.getItemAtPosition(position);
    Intent i = s.startIntent;
    int activityID = i.getIntExtra(Constants.EXTRA_SUBACTIVITY_ID, -1);

    // Hourly newscast
    if (activityID == R.string.msg_main_subactivity_hourly) {
      // hourly newscast tracking
      StringBuilder pageName = new StringBuilder(
          getString(R.string.msg_main_subactivity_hourly));
      Tracker.instance(getApplication()).trackPage(
          new ActivityMeasurement(pageName.toString(), "Newscast"));

      // It takes a few seconds to download and parse so run in another thread
      Thread hourlyDownloader = new Thread(new Runnable() {
        public void run() {
          try {
            final String hourlyUrlCall =
                "http://www.npr.org/rss/podcast.php?id=500005";
            Podcast hourlyPod = PodcastFactory.downloadPodcast(hourlyUrlCall);
            List<Item> items = hourlyPod.getItems();

            // The hourly podcast has only one item
            Item info = items.get(0);

            // Set the member/instance variables
            hourlyTitle = info.getTitle();
            hourlyGuid = info.getGuid();
            hourlyURL = info.getUrl();

            // Success! let the activity know it can request playback
            handler.sendEmptyMessage(MSG_PLAY_HOURLY);

          } catch (Exception e) {
            Log.e(LOG_TAG, "error downloading hourly", e);
            handler.sendEmptyMessage(MSG_ERROR_MESSAGE);
          }
        }
      });

      hourlyDownloader.start();
    } else {
      startActivityWithoutAnimation(i);
    }
  }

  private class MainListAdapter extends ArrayAdapter<SubActivity> {
    public MainListAdapter(SubActivity[] activities) {
      super(Main.this, android.R.layout.simple_list_item_1, android.R.id.text1,
          activities);
    }
  }

  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_main_logo);
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("Home Screen");
    Tracker.instance(getApplication()).trackPage(
        new ActivityMeasurement(pageName.toString(), "Home"));
  }

  private void playHourly() {
    // Request to stream audio
    Intent i = new Intent(ListenView.class.getName())
        .putExtra(ListenView.EXTRA_CONTENT_URL, hourlyGuid)
        .putExtra(ListenView.EXTRA_CONTENT_TITLE, hourlyTitle)
        .putExtra(ListenView.EXTRA_ENQUEUE, false)
        .putExtra(ListenView.EXTRA_CONTENT_URL, hourlyURL)
        .putExtra(ListenView.EXTRA_PLAY_IMMEDIATELY, true);

    sendBroadcast(i);
  }

  private void errorMessage() {
    // Let the user know something was wrong, most likely a bad connection
    Toast.makeText(this,
        getResources().getString(R.string.msg_main_check_connection),
        Toast.LENGTH_SHORT).show();
  }

  private void initApiKey() {
    String key = "";
    try {
      key = FileUtils.readFile(this, R.raw.api_key).toString();
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    if (key.equals("")) {
      new AlertDialog.Builder(this).setMessage(R.string.msg_api_key_error)
          .setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              finish();
            }
          }).create().show();
      ApiConstants.createInstance(key.toString());
    } else {
      ApiConstants.createInstance(key.toString());
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Tracker.instance(getApplication()).finish();
    cancelLocationListeners();
  }

  /**
   * On start up, launch a location listener for each service. We need to do
   * this in order to ensure that getLastKnownLocation, used to find local
   * stations, will always find a value.
   */
  private void lauchLocationListeners() {
    LocationManager lm =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    List<String> providers = lm.getAllProviders();
    for (String provider : providers) {
      LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
          handler.sendEmptyMessage(MSG_CANCEL_LOCATION_LISTENERS);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

      };
      lm.requestLocationUpdates(provider, 60000, 0, listener);
      locationListeners.add(listener);
    }
  }

  /**
   * Remove all listeners.
   */
  private void cancelLocationListeners() {
    LocationManager lm =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    // Synchronized because there may be multiple listeners running and
    // we don't want them to both try to alter the listeners collection
    // at the same time.
    synchronized (locationListeners) {
      for (LocationListener listener : locationListeners) {
        lm.removeUpdates(listener);
        locationListeners.remove(listener);
      }
    }
  }

}

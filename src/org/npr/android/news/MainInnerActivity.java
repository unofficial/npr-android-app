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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.ActivityMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.Podcast;
import org.npr.api.Podcast.Item;
import org.npr.api.Podcast.PodcastFactory;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainInnerActivity extends BackAndForthActivity implements
    OnItemClickListener {
  
  private static final String LOG_TAG = MainInnerActivity.class.getName();
  
  private class SubActivity {
    private final Intent startIntent;
    private SubActivity(Intent startIntent) {
      this.startIntent = startIntent;
    }
    @Override
    public String toString() {
      return MainInnerActivity.this.getString(startIntent.getIntExtra(
          Constants.EXTRA_SUBACTIVITY_ID, -1));
    }
  }
  
  private ListView listView;
  private String hourlyTitle;
  private String hourlyGuid;
  private String hourlyURL;
  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0:
          errorMessage();
          break;
        case 1:
          playHourly();
          break;
      }
    }
  };

  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main_inner);

    listView = (ListView) findViewById(R.id.MainListView);
    String grouping = null;
    String description = "Top Stories";
    String topicId = "1002";
    Map<String, String> params = new HashMap<String, String>();
    params.put("id", topicId);
    params.put("fields", ApiConstants.STORY_FIELDS);
    params.put("sort", "assigned");

    String newsUrl =
        ApiConstants.instance().createUrl(ApiConstants.STORY_PATH, params);
    final SubActivity[] activities = {
        new SubActivity(new Intent(this, NewsListActivity.class).putExtra(
            Constants.EXTRA_SUBACTIVITY_ID,
            R.string.msg_main_subactivity_news).putExtra(
            Constants.EXTRA_QUERY_URL, newsUrl).putExtra(
            Constants.EXTRA_DESCRIPTION, description).putExtra(
            Constants.EXTRA_GROUPING, grouping).putExtra(Constants.EXTRA_SIZE,
            5)),
        new SubActivity(new Intent(this, MainInnerActivity.class).putExtra(
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
            R.string.msg_main_subactivity_search))
        };
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
    if ( activityID == R.string.msg_main_subactivity_hourly) {
      // Request to show the progress indicator
      Intent showProgress = new Intent(Constants.BROADCAST_PROGRESS)
          .putExtra(Constants.EXTRA_SHOW_PROGRESS, true);
      sendBroadcast(showProgress);
      
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
            handler.sendEmptyMessage(1);
            
          } catch(Exception e) {
              Log.e(LOG_TAG, "error downloading hourly", e);
              handler.sendEmptyMessage(0);
          }
        }
      });
      
      hourlyDownloader.start();
    }
    else {
      i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
      ((Main) getParent()).goForward(i, true);
    }
  }

  private class MainListAdapter extends ArrayAdapter<SubActivity> {
    public MainListAdapter(SubActivity[] activities) {
      super(MainInnerActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1,
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
    Intent i = new Intent(ListenActivity.class.getName())
        .putExtra(ListenActivity.EXTRA_CONTENT_URL, hourlyGuid)
        .putExtra(ListenActivity.EXTRA_CONTENT_TITLE, hourlyTitle)
        .putExtra(ListenActivity.EXTRA_ENQUEUE, true)
        .putExtra(ListenActivity.EXTRA_CONTENT_URL, hourlyURL)
        .putExtra(ListenActivity.EXTRA_PLAY_IMMEDIATELY, true);
      
    sendBroadcast(i);
        
    // Stop showing the progress indicator
    Intent stopProgress = new Intent(Constants.BROADCAST_PROGRESS)
        .putExtra(Constants.EXTRA_SHOW_PROGRESS, false);
      
    sendBroadcast(stopProgress);
  }
  
  private void errorMessage() {
    // We need to stop the progress indicator 
    // Let the user know nothing is being processed
    Intent showProgress = new Intent(Constants.BROADCAST_PROGRESS)
        .putExtra(Constants.EXTRA_SHOW_PROGRESS, false);
    sendBroadcast(showProgress);
    
    // Let the user know something was wrong, most likely a bad connection
    Toast.makeText(this, 
        getResources().getString(R.string.msg_main_check_connection), 
        Toast.LENGTH_SHORT).show();
  }
  
}

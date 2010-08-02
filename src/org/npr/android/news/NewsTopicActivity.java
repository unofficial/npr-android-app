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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.ActivityMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.Program;
import org.npr.api.StoryGrouping;
import org.npr.api.Topic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsTopicActivity extends BackAndForthActivity implements
    OnItemClickListener {

  public static enum TopicType {
    TOPICS(R.string.msg_main_subactivity_topics),
    PROGRAMS(R.string.msg_main_subactivity_programs),
    ;
    private int title;

    private TopicType(int title) {
      this.title = title;
    }
    
  }

  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      Intent i =
        new Intent(Constants.BROADCAST_PROGRESS).putExtra(
            Constants.EXTRA_SHOW_PROGRESS, false);
      NewsTopicActivity.this.sendBroadcast(i);
      switch (msg.what) {
        case 0:
          listView.setAdapter(listAdapter);
          break;
      }
    }
  };

  private ListAdapter listAdapter;
  private ListView listView;
  private Thread listInitThread;
  private TopicType topic = null;

  @SuppressWarnings("unchecked")
  private int constructList() {
    int type = getIntent().getIntExtra(Constants.EXTRA_SUBACTIVITY_ID, -1);
    List<? extends StoryGrouping> groupings = null;
    switch (topic) {
      case TOPICS:
        groupings =
            Topic.factory.downloadStoryGroupings(30);
        listAdapter = new NewsTopicAdapter<Topic>((List<Topic>) groupings); 
        break;
      case PROGRAMS:
        groupings =
            Program.factory.downloadStoryGroupings(30);
        listAdapter = new NewsTopicAdapter<Program>((List<Program>) groupings); 
        break;
    }
    int message = 0;

    if (groupings == null) {
      message = 1;
    }
    return message;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    int type = getIntent().getIntExtra(Constants.EXTRA_SUBACTIVITY_ID, -1);
    switch (type) {
      case R.string.msg_main_subactivity_topics:
        topic = TopicType.TOPICS;
        break;
      case R.string.msg_main_subactivity_programs:
        topic = TopicType.PROGRAMS;
        break;
    }
    super.onCreate(savedInstanceState);
    setContentView(R.layout.news_topics);
    listView = (ListView) findViewById(R.id.ListView01);
    listView.setOnItemClickListener(this);

    initializeList();
  }

  private void initializeList() {
    Intent i =
        new Intent(Constants.BROADCAST_PROGRESS).putExtra(
            Constants.EXTRA_SHOW_PROGRESS, true);
    this.sendBroadcast(i);
    listInitThread = new Thread(new Runnable() {
      public void run() {
        int result = NewsTopicActivity.this.constructList();
        handler.sendEmptyMessage(result);
      }
    });
    listInitThread.start();
  }

  @Override
  public CharSequence getMainTitle() {
    return getString(topic.title);
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("News");
    Tracker.instance(getApplication()).trackPage(
        new ActivityMeasurement(pageName.toString(), "News"));
  }

  @Override
  public boolean isRefreshable(){
    return true;
  }

  @Override
  public void refresh(){
    initializeList();
  }
  
  private class NewsTopicAdapter<T extends StoryGrouping> extends
      ArrayAdapter<T> {
    public NewsTopicAdapter(List<T> groupings) {
      super(NewsTopicActivity.this, android.R.layout.simple_list_item_1,
          android.R.id.text1, groupings);
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    int type = getIntent().getIntExtra(Constants.EXTRA_SUBACTIVITY_ID, -1);
    StoryGrouping item = (StoryGrouping) parent.getItemAtPosition(position);

    String grouping = getString(type);
    String description = item.getTitle();
    String topicId = item.getId();
    Map<String, String> params = new HashMap<String, String>();
    params.put(ApiConstants.PARAM_ID, topicId);
    params.put(ApiConstants.PARAM_FIELDS, ApiConstants.STORY_FIELDS);
    params.put(ApiConstants.PARAM_SORT, "assigned");      

    String url =
        ApiConstants.instance().createUrl(ApiConstants.STORY_PATH, params);

    Intent i = new Intent(this, NewsListActivity.class);
    i.putExtra(Constants.EXTRA_QUERY_URL, url);
    i.putExtra(Constants.EXTRA_DESCRIPTION, description);
    i.putExtra(Constants.EXTRA_GROUPING, grouping);
    i.putExtra(Constants.EXTRA_SIZE, 10);
    ((Main) getParent()).goForward(i, true);
  }
}

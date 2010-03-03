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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.StoryListMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.Story;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsListActivity extends BackAndForthActivity implements
    OnItemClickListener {

  private String apiUrl;
  private String description;
  private String grouping;
  private String topicId;
  private int initialSize;

  protected NewsListAdapter listAdapter;

  private static Map<String, Story> storyCache = new HashMap<String, Story>();

  public static Story getStoryFromCache(String storyId) {
    Story result = storyCache.get(storyId);
    if (result == null) {
      result = Story.StoryFactory.downloadStory(storyId);
      storyCache.put(storyId, result);
    }
    return result;
  }

  public static void addAllToStoryCache(List<Story> stories) {
    for (Story story : stories) {
      storyCache.put(story.getId(), story);
    }
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    storyCache.clear();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    apiUrl = getIntent().getStringExtra(Constants.EXTRA_QUERY_URL);
    setContentView(R.layout.news);
    ListView listView = (ListView) findViewById(R.id.ListView01);
    listView.setOnItemClickListener(this);
    listAdapter =
      new NewsListAdapter(NewsListActivity.this);
    listView.setAdapter(listAdapter);

    grouping = getIntent().getStringExtra(Constants.EXTRA_GROUPING);
    description = getIntent().getStringExtra(Constants.EXTRA_DESCRIPTION);

    topicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);
    initialSize = getIntent().getIntExtra(Constants.EXTRA_SIZE, 0);
    addStories();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Story s = (Story) parent.getAdapter().getItem(position);
    if (s == null) {
      addStories();
    } else {
      Intent i = new Intent(this, NewsStoryActivity.class);
      i.putExtra(Constants.EXTRA_STORY_ID, s.getId());
      i.putExtra(Constants.EXTRA_DESCRIPTION, description);
      ((Main) getParent()).goForward(i, true);
    }
  }

  private void addStories() {
    Map<String, String> params = new HashMap<String, String>();
    params.put("startNum", "" + listAdapter.getCount());
    params.put("numResults", "" + initialSize);
    listAdapter.addMoreStories(ApiConstants.instance().addParams(apiUrl,
        params), initialSize);    
  }

  @Override
  public CharSequence getMainTitle() {
    return description;
  }

  @Override
  public void trackNow() {
    StringBuilder pageName =
        new StringBuilder("News").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append(grouping).append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append(description);
    Tracker.instance(getApplication()).trackPage(
        new StoryListMeasurement(pageName.toString(), "News", topicId));
  }

  @Override
  public boolean isRefreshable(){
    return true;
  }

  @Override
  public void refresh(){
    addStories();
  }
}

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

import android.os.Bundle;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.SearchResultsMeasurement;

public class SearchResultsActivity extends NewsListActivity {
  private String query;
  private String description;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    query = getIntent().getStringExtra(Constants.EXTRA_QUERY_TERM);
    description = getIntent().getStringExtra(Constants.EXTRA_DESCRIPTION);
  }
  
  @Override
  public CharSequence getMainTitle() {
    return description;
  }

  @Override
  public void trackNow() {
    if (listAdapter == null) {
      // Hacky, but works.
      Thread trackerThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) { }
          trackNow();
        }
      });
      trackerThread.start();
      return;
    }
    
    StringBuilder pageName =
        new StringBuilder("Search").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append("Results");
    int resultsCount = listAdapter.isEmpty() ? 0 : listAdapter.getCount();
    Tracker.instance(getApplication()).trackPage(
        new SearchResultsMeasurement(pageName.toString(),
        "Search", query, resultsCount));
  }

}

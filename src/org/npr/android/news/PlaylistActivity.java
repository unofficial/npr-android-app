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
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import org.npr.android.util.PlaylistProvider;
import org.npr.android.util.Tracker;
import org.npr.android.util.PlaylistProvider.Items;
import org.npr.android.util.Tracker.ActivityMeasurement;

public class PlaylistActivity extends Activity implements OnClickListener,
    OnCheckedChangeListener, OnItemClickListener, Trackable {
  private static final String LOG_TAG = PlaylistActivity.class.getName();
  private boolean filterUnread = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.playlist);
    Button clearButton = (Button) findViewById(R.id.PlaylistClear);
    RadioButton unreadButton = (RadioButton) findViewById(R.id.RadioButton01);
    RadioButton readButton = (RadioButton) findViewById(R.id.RadioButton02);
    clearButton.setOnClickListener(this);

    unreadButton.setChecked(filterUnread);
    readButton.setChecked(!filterUnread);

    unreadButton.setOnCheckedChangeListener(this);
    readButton.setOnCheckedChangeListener(this);
    
    refreshList();
    trackNow();
  }

  
  private void refreshList() {
    String[] cols = new String[] {Items.NAME};
    Cursor cursor =
        managedQuery(PlaylistProvider.CONTENT_URI, null,
            Items.IS_READ + " = ?", new String[] {filterUnread ? "0" : "1"},
            Items.PLAY_ORDER);
    Log.d(LOG_TAG, "" + cursor.getCount());
    startManagingCursor(cursor);

    ListAdapter adapter =
        new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1,
            cursor, cols, new int[] {android.R.id.text1});

    ListView listView = (ListView) findViewById(R.id.ListView01);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(this);
  }

  @Override
  public void onClick(View arg0) {
    switch(arg0.getId()) {
      case R.id.PlaylistClear:
        getContentResolver().delete(PlaylistProvider.CONTENT_URI,
            Items.IS_READ + " = ?", new String[] {filterUnread ? "0" : "1"});
        refreshList();
        break;
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (!isChecked) {
      return;
    }
    switch(buttonView.getId()) {
      case R.id.RadioButton01:
        filterUnread = isChecked;
        refreshList();
        break;
      case R.id.RadioButton02:
        filterUnread = !isChecked;
        refreshList();
        break;
    }
    trackNow();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Cursor c = (Cursor) parent.getItemAtPosition(position);
    if (c != null) {
      c.moveToPosition(position);
      long playlistId = c.getLong(c.getColumnIndex(Items._ID));
      Log.d(LOG_TAG, "clicked on position " + position + ", id " + playlistId);
      Intent i = new Intent(ListenActivity.class.getName()).putExtra(
          ListenActivity.EXTRA_CONTENT_ID, playlistId).putExtra(
          ListenActivity.EXTRA_PLAY_IMMEDIATELY, true);
      sendBroadcast(i);
    }
    finish();
  }


  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("Playlist");
    String description = filterUnread ? "Active" : "Listened To";
    pageName.append(Tracker.PAGE_NAME_SEPARATOR).append(description);
    Tracker.instance(getApplication()).trackPage(
        new ActivityMeasurement(pageName.toString(), "Playlist"));
  }
}

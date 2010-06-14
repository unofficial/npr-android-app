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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.LinkEvent;
import org.npr.android.util.Tracker.PodcastLaterEvent;
import org.npr.android.util.Tracker.PodcastNowEvent;
import org.npr.api.Podcast;
import org.npr.api.Podcast.Item;
import org.npr.api.Podcast.PodcastFactory;

public class PodcastActivity extends Activity implements OnItemClickListener,
    OnClickListener {
  public static final String EXTRA_PODCAST_URL = "extra_podcast_url";
  public static final String EXTRA_PODCAST_TITLE = "extra_podcast_title";
  private Podcast podcast;
  private ProgressBar progressBar;
  private ListView listView;
  private TextView miscText;
  private Item lastItem;

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      if (podcast != null) {
        ArrayAdapter<Item> adapter =
            new ArrayAdapter<Item>(PodcastActivity.this,
                android.R.layout.simple_list_item_2, android.R.id.text1,
                podcast.getItems()) {

              @Override
              public View getView(int position, View convertView,
                  ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                Item i = getItem(position);
                TextView tv = (TextView) v.findViewById(android.R.id.text2);
                tv.setText(i.getPubDate());
                return v;
              }
        };
        
        progressBar.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        if (podcast.getSummary() != null) {
          miscText.setText(podcast.getSummary());
          miscText.setVisibility(View.VISIBLE);
        }
      } else {
        final AlertDialog.Builder builder = new AlertDialog.Builder(PodcastActivity.this);
        builder.setTitle(R.string.msg_error);
        builder.setMessage(R.string.msg_parse_error);
        builder.setNeutralButton(R.string.msg_ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            PodcastActivity.this.finish();            
          }
        });
        builder.create().show();
      }
    }
  };
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.podcast);
    
    final String url = getIntent().getStringExtra(EXTRA_PODCAST_URL);
    String title = getIntent().getStringExtra(EXTRA_PODCAST_TITLE);
    TextView titleText = (TextView) findViewById(R.id.PodcastTitle);
    titleText.setText(title);
    progressBar = (ProgressBar) findViewById(R.id.ProgressBar01);
    progressBar.setVisibility(View.VISIBLE);
    miscText = (TextView) findViewById(R.id.PodcastSummary);
    miscText.setVisibility(View.GONE);
    listView = (ListView) findViewById(R.id.ListView01);
    listView.setOnItemClickListener(this);
    
    new Thread(new Runnable(){
      @Override
      public void run() {
        podcast = PodcastFactory.downloadPodcast(url);
        handler.sendEmptyMessage(0);
      }
    }).start();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Item i = (Item) parent.getAdapter().getItem(position);
    showDialog(i);
  }
  
  private void showDialog(Item i) {
    lastItem = i;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.msg_podcast_dialog_title);
    builder.setMessage(i.toString());
    builder.setNegativeButton(R.string.msg_podcast_cancel, this);
    builder.setNeutralButton(R.string.msg_podcast_enqueue, this);
    builder.setPositiveButton(R.string.msg_podcast_listen_now, this);
    Dialog d = builder.create();
    d.show();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    Log.w("Podcast", "got Click");
    boolean play = !(which == Dialog.BUTTON_NEGATIVE);
    boolean playNow = play && (which == Dialog.BUTTON_POSITIVE);
    if (play) {
      Intent i =
        new Intent(ListenActivity.class.getName()).putExtra(
            ListenActivity.EXTRA_CONTENT_URL, lastItem.getUrl()).putExtra(
            ListenActivity.EXTRA_CONTENT_TITLE, lastItem.getTitle());
      i.putExtra(ListenActivity.EXTRA_ENQUEUE, true);
      i.putExtra(Constants.EXTRA_STORY_ID, (String) null);
      LinkEvent e;
      if (playNow) {
        i.putExtra(ListenActivity.EXTRA_PLAY_IMMEDIATELY, true);
        e =
            new PodcastNowEvent(lastItem.getTitle(), lastItem.getTitle(),
                lastItem.getUrl());
      } else {
        e =
            new PodcastLaterEvent(podcast.getTitle(), lastItem.getTitle(),
                lastItem.getUrl());
      }
      sendBroadcast(i);
      Log.w("Podcast", "broadcast sent");
      Tracker.instance(getApplication()).trackLink(e);    
    }
  }
}

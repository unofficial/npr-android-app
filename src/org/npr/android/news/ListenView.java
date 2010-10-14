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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

public class ListenView extends FrameLayout implements OnClickListener,
    OnSeekBarChangeListener, OnDrawerOpenListener, OnDrawerCloseListener {

  private static final String LOG_TAG = ListenView.class.toString();
  public static final String EXTRA_CONTENT_URL = "extra_content_url";
  public static final String EXTRA_CONTENT_TITLE = "extra_content_title";
  public static final String EXTRA_CONTENT_ID = "extra_content_id";
  public static final String EXTRA_ENQUEUE = "extra_enqueue";
  public static final String EXTRA_PLAY_IMMEDIATELY = "extra_play_immediately";
  public static final String EXTRA_STREAM = "extra_stream";

  private ImageButton streamButton;
  private ImageButton playButton;
  private SeekBar progressBar;
  private TextView infoText;
  private TextView lengthText;
  private SlidingDrawer drawer;

  private BroadcastReceiver receiver = new ListenBroadcastReceiver();

  private ServiceConnection conn;
  private PlaybackService player;
  
  public ListenView(Context context) {
    super(context);
  }

  private void init() {
    ViewGroup.inflate(getContext(), R.layout.listen, this);

    drawer = (SlidingDrawer) findViewById(R.id.drawer);
    drawer.setOnDrawerOpenListener(this);
    drawer.setOnDrawerCloseListener(this);
    playButton = (ImageButton) findViewById(R.id.StreamPlayButton);
    playButton.setEnabled(false);
    playButton.setOnClickListener(this);
    streamButton = (ImageButton) findViewById(R.id.StreamShareButton);
    streamButton.setOnClickListener(this);
    streamButton.setEnabled(false);

    Button playlistButton = (Button) findViewById(R.id.StreamPlaylistButton);
    playlistButton.setOnClickListener(this);

    progressBar = (SeekBar) findViewById(R.id.StreamProgressBar);
    progressBar.setMax(100);
    progressBar.setOnSeekBarChangeListener(this);
    progressBar.setEnabled(false);

    infoText = (TextView) findViewById(R.id.StreamTextView);

    lengthText = (TextView) findViewById(R.id.StreamLengthText);
    lengthText.setText("");
  }

  public void reattach() {
    getContext().registerReceiver(receiver,
        new IntentFilter(this.getClass().getName()));

    Intent serviceIntent = new Intent(getContext(), PlaybackService.class);
    conn = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        player = ((PlaybackService.ListenBinder) service).getService();
        onBindComplete((PlaybackService.ListenBinder) service);
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        Log.w(LOG_TAG, "DISCONNECT");
        player = null;
      }

    };

    getContext().getApplicationContext().bindService(serviceIntent, conn,
        Context.BIND_AUTO_CREATE);
  }

  public void detach() {
    getContext().unregisterReceiver(receiver);
    getContext().getApplicationContext().unbindService(conn);
  }
  
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    init();
  }

  private void onBindComplete(PlaybackService.ListenBinder binder) {
  }

  private void togglePlay() {
    if (player.isPlaying()) {
      player.pause();
    } else {
      player.play();
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
    case R.id.StreamPlayButton:
      togglePlay();
      break;
    case R.id.StreamPlaylistButton:
        getContext().startActivity(
            new Intent(getContext(), PlaylistActivity.class));
      break;
    case R.id.StreamShareButton:
      Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
//      shareIntent.putExtra(Intent.EXTRA_SUBJECT, current.title);
//      shareIntent.putExtra(Intent.EXTRA_TEXT, String.format("%s: %s",
//          current.title, current.url));
      shareIntent.setType("text/plain");
      getContext().startActivity(Intent.createChooser(shareIntent,
          getContext().getString(R.string.msg_share_story)));
      break;
    }
  }

  class ListenBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.w(LOG_TAG, "broadcast received");
    }
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress,
      boolean fromUser) {
    int possibleProgress = progress > seekBar.getSecondaryProgress() ? seekBar
        .getSecondaryProgress() : progress;
    if (fromUser) {
      // Only seek to position if we've downloaded the content.
      int msec = player.getDuration() * possibleProgress / seekBar.getMax();
      player.seekTo(msec);
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onDrawerOpened() {
    ImageView arrow = (ImageView) findViewById(R.id.DrawerArrowImage);
    arrow.setImageDrawable(getResources().getDrawable(R.drawable.arrow_down));
  }

  @Override
  public void onDrawerClosed() {
    ImageView arrow = (ImageView) findViewById(R.id.DrawerArrowImage);
    arrow.setImageDrawable(getResources().getDrawable(R.drawable.arrow_up));
  }
}

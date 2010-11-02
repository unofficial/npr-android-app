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

import org.npr.android.util.PlaylistEntry;

import java.io.IOException;

public class ListenView extends FrameLayout implements OnClickListener,
    OnSeekBarChangeListener, OnDrawerOpenListener, OnDrawerCloseListener {

  private static final String LOG_TAG = ListenView.class.getName();

  private ImageButton streamButton;
  private ImageButton playButton;
  private SeekBar progressBar;
  private TextView infoText;
  private TextView lengthText;
  private SlidingDrawer drawer;
  private boolean playButtonisPause = false;

  private BroadcastReceiver changeReceiver = new PlaybackChangeReceiver();
  private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiver();
  private BroadcastReceiver closeReceiver = new PlaybackCloseReceiver();

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
    progressBar.setOnSeekBarChangeListener(this);
    progressBar.setEnabled(false);

    infoText = (TextView) findViewById(R.id.StreamTextView);

    lengthText = (TextView) findViewById(R.id.StreamLengthText);
    lengthText.setText("");

    attachToPlaybackService();
  }

  public void attachToPlaybackService() {
    Intent serviceIntent = new Intent(getContext(), PlaybackService.class);
    conn = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        player = ((PlaybackService.ListenBinder) service).getService();
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        Log.w(LOG_TAG, "DISCONNECT");
        player = null;
      }
    };

    // Explicitly start the service. Don't use BIND_AUTO_CREATE, since it
    // causes an implicit service stop when the last binder is removed.
    getContext().getApplicationContext().startService(serviceIntent);
    getContext().getApplicationContext().bindService(serviceIntent, conn, 0);

    getContext().registerReceiver(changeReceiver,
        new IntentFilter(PlaybackService.SERVICE_CHANGE_NAME));
    getContext().registerReceiver(updateReceiver,
        new IntentFilter(PlaybackService.SERVICE_UPDATE_NAME));
    getContext().registerReceiver(closeReceiver,
        new IntentFilter(PlaybackService.SERVICE_CLOSE_NAME));
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    init();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    Log.d(LOG_TAG, "detached from window");
    getContext().unregisterReceiver(changeReceiver);
    getContext().unregisterReceiver(updateReceiver);
    getContext().unregisterReceiver(closeReceiver);
    getContext().getApplicationContext().unbindService(conn);
  }

  private void togglePlay() {
    if (player.isPlaying()) {
      player.pause();
      playButton.setImageResource(android.R.drawable.ic_media_play);
      playButtonisPause = false;
    } else {
      player.play();
      playButton.setImageResource(android.R.drawable.ic_media_pause);
      playButtonisPause = true;
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

  protected void listen(PlaylistEntry entry) {
    if (player != null) {
      try {
        player.setCurrent(entry);
        player.listen(entry.url, entry.isStream);
      } catch (IllegalArgumentException e) {
        Log.e(LOG_TAG, "", e);
      } catch (IllegalStateException e) {
        Log.e(LOG_TAG, "", e);
      } catch (IOException e) {
        Log.e(LOG_TAG, "", e);
      }
    }
  }

  private class PlaybackChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String title = intent.getStringExtra(PlaybackService.EXTRA_TITLE);
      infoText.setText(title);
    }
  }

  private class PlaybackUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      int duration = intent.getIntExtra(PlaybackService.EXTRA_DURATION, 1);
      int position = intent.getIntExtra(PlaybackService.EXTRA_POSITION, 0);
      int downloaded = intent.getIntExtra(PlaybackService.EXTRA_DOWNLOADED, 1);
      if (!playButtonisPause && player != null && player.isPlaying()) {
        playButton.setImageResource(android.R.drawable.ic_media_pause);
        playButtonisPause = true;
      }
      playButton.setEnabled(true);
      progressBar.setEnabled(true);
      progressBar.setMax(duration);
      progressBar.setProgress(position);
      progressBar.setSecondaryProgress(downloaded);
    }
  }
  
  private class PlaybackCloseReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      playButton.setEnabled(false);
      playButton.setImageResource(android.R.drawable.ic_media_play);
      progressBar.setEnabled(false);
      progressBar.setProgress(0);
      progressBar.setSecondaryProgress(0);
      infoText.setText(null);
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

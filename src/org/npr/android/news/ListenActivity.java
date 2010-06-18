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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

import org.npr.android.util.M3uParser;
import org.npr.android.util.PlaylistParser;
import org.npr.android.util.PlaylistProvider;
import org.npr.android.util.PlsParser;
import org.npr.android.util.PlaylistProvider.Items;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ListenActivity extends Activity implements OnClickListener,
    OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,
    OnInfoListener, OnSeekBarChangeListener, OnPreparedListener,
    OnDrawerOpenListener, OnDrawerCloseListener {

  private static final String LOG_TAG = ListenActivity.class.toString();
  public static final String EXTRA_CONTENT_URL = "extra_content_url";
  public static final String EXTRA_CONTENT_TITLE = "extra_content_title";
  public static final String EXTRA_CONTENT_ID = "extra_content_id";
  public static final String EXTRA_ENQUEUE = "extra_enqueue";
  public static final String EXTRA_PLAY_IMMEDIATELY = "extra_play_immediately";
  public static final String EXTRA_STREAM = "extra_stream";

  private PlaylistEntry current = null;
  private ImageButton streamButton;
  private ImageButton playButton;
  private SeekBar progressBar;
  private TextView infoText;
  private TextView lengthText;
  private SlidingDrawer drawer;
  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case 0:
        setPlayButton();
        break;
      case 1:
        updateProgress();
        break;
      case 2:
        enableProgress(true);
        break;
      case 3:
        enableProgress(false);
        break;
      }
    }
  };

  private Thread updateProgressThread;
  private BroadcastReceiver receiver = new ListenBroadcastReceiver();

  private ServiceConnection conn;
  private PlaybackService player;
  
  private boolean isPausedInCall = false;
  private TelephonyManager telephonyManager = null;
  private PhoneStateListener listener = null;

  // amount of time to rewind playback when resuming after call 
  private final static int RESUME_REWIND_TIME = 3000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.listen);
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
    
    telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    // Create a PhoneStateListener to watch for offhook and idle events
    listener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
        switch (state) {
        case TelephonyManager.CALL_STATE_OFFHOOK:
          // phone going offhook, pause the player
          if (player != null && player.isPlaying()) {
            player.pause();
            isPausedInCall = true;
            setPlayButton();
          }
          break;
        case TelephonyManager.CALL_STATE_IDLE:
          // phone idle.  rewind a couple of seconds and start playing
          if (isPausedInCall && player != null) {
            int resumePosition = player.getPosition() - RESUME_REWIND_TIME;
            if (resumePosition < 0) {
              resumePosition = 0;
            }
            player.seekTo(resumePosition);
            player.play();
            setPlayButton();
          }
          break;
        }
      }
    };

    // Register the listener with the telephony manager
    telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

    registerReceiver(receiver, new IntentFilter(this.getClass().getName()));

    Intent serviceIntent = new Intent(this, PlaybackService.class);
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
    if (!PlaybackService.isRunning) {
      getApplicationContext().startService(serviceIntent);
    }
    getApplicationContext().bindService(serviceIntent, conn, 1);
  }

  private void onBindComplete(PlaybackService.ListenBinder binder) {
    binder.setListener(this);
    if (player.isPlaying()) {
      current = player.getCurrentEntry();
      setPlayButton();
      play();
      startUpdateThread();
    }
  }

  private void play() {
    resetUI();
    new Thread(new Runnable() {
      public void run() {
        startListening();
      }
    }).start();
    Log.d(LOG_TAG, "started playing");
  }

  private void resetUI() {
    Log.d(LOG_TAG, "resetUI()");
    infoText.setText(current.title);
    infoText.setTypeface(infoText.getTypeface(), Typeface.NORMAL);

    progressBar.setProgress(0);
    progressBar.setSecondaryProgress(0);
    streamButton.setEnabled(true);
  }

  private void enableProgress(boolean enabled) {
    progressBar.setEnabled(enabled);
  }

  private void togglePlay() {
    // cancel pause while in a phone call if user manually starts playing
    if (isPausedInCall) {
      isPausedInCall = false;
    } 
    if (player.isPlaying()) {
      player.pause();
    } else {
      player.play();
    }
    setPlayButton();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
    case R.id.StreamPlayButton:
      togglePlay();
      break;
    case R.id.StreamPlaylistButton:
      startActivity(new Intent(this, PlaylistActivity.class));
      break;
    case R.id.StreamShareButton:
      Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
      shareIntent.putExtra(Intent.EXTRA_SUBJECT, current.title);
      shareIntent.putExtra(Intent.EXTRA_TEXT, String.format("%s: %s",
          current.title, current.url));
      shareIntent.setType("text/plain");
      startActivity(Intent.createChooser(shareIntent,
          getString(R.string.msg_share_story)));
      break;
    }
  }

  private void setPlayButton() {
    playButton.setEnabled(true);
    if (player.isPlaying()) {
      playButton.setImageResource(android.R.drawable.ic_media_pause);
    } else {
      playButton.setImageResource(android.R.drawable.ic_media_play);
    }
  }

  public void updateProgress() {
    try {
      if (player.isPlaying()) {
        int progress = 100 * player.getPosition() / player.getDuration();
        progressBar.setProgress(progress);
        updatePlayTime();
      }
    } catch (IllegalStateException e) {
      Log.e(LOG_TAG, "update progress", e);
    }
  }

  private void listen(final String url, boolean stream)
      throws IllegalArgumentException, IllegalStateException, IOException {
    Log.d(LOG_TAG, "listening to " + url);

    if (updateProgressThread != null && updateProgressThread.isAlive()) {
      updateProgressThread.interrupt();
      try {
        updateProgressThread.join();
      } catch (InterruptedException e) {
        Log.e(LOG_TAG, "error in updateProgressthread", e);
      }
    }
    player.stop();
    player.listen(url, stream);
    handler.sendEmptyMessage(stream ? 3 : 2);
  }

  private void startListening() {
    String url = current.url;
    try {
      if (url == null || url.equals("")) {
        Log.d(LOG_TAG, "no url");
        // Do nothing.
      } else if (isPlaylist(url)) {
        downloadPlaylistAndPlay();
      } else {
        listen(url, current.isStream);
      }
    } catch (IllegalArgumentException e) {
      Log.e(LOG_TAG, "", e);
      e.printStackTrace();
    } catch (IllegalStateException e) {
      Log.e(LOG_TAG, "", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "", e);
    }
    Log.d(LOG_TAG, "playing commenced");
  }

  private boolean isPlaylist(String url) {
    return url.indexOf("m3u") > -1 || url.indexOf("pls") > -1;
  }

  private void downloadPlaylistAndPlay() throws MalformedURLException,
      IOException {
    String url = current.url;
    Log.d(LOG_TAG, "downloading " + url);
    URLConnection cn = new URL(url).openConnection();
    cn.connect();
    InputStream stream = cn.getInputStream();
    if (stream == null) {
      Log
          .e(getClass().getName(),
              "Unable to create InputStream for url: + url");
    }

    File downloadingMediaFile = new File(getCacheDir(), "playlist_data");
    FileOutputStream out = new FileOutputStream(downloadingMediaFile);
    byte buf[] = new byte[16384];
    int totalBytesRead = 0, incrementalBytesRead = 0;
    int numread;
    while ((numread = stream.read(buf)) > 0) {
      out.write(buf, 0, numread);
      totalBytesRead += numread;
      incrementalBytesRead += numread;
    }

    stream.close();
    out.close();
    PlaylistParser parser;
    if (url.indexOf("m3u") > -1) {
      parser = new M3uParser(downloadingMediaFile);
    } else if (url.indexOf("pls") > -1) {
      parser = new PlsParser(downloadingMediaFile);
    } else {
      return;
    }
    String mp3Url = parser.getNextUrl();
    listen(mp3Url, current.isStream);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (telephonyManager != null && listener != null) {
      telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
    }
    unregisterReceiver(receiver);
    getApplicationContext().unbindService(conn);
  }

  class ListenBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.w(LOG_TAG, "broadcast received");
      String url = intent.getStringExtra(EXTRA_CONTENT_URL);
      String title = intent.getStringExtra(EXTRA_CONTENT_TITLE);
      long id = intent.getLongExtra(EXTRA_CONTENT_ID, -1);
      boolean enqueue = intent.getBooleanExtra(EXTRA_ENQUEUE, false);
      boolean playImmediately = intent.getBooleanExtra(EXTRA_PLAY_IMMEDIATELY,
          false);
      boolean stream = intent.getBooleanExtra(EXTRA_STREAM, false);
      String storyID = intent.getStringExtra(Constants.EXTRA_STORY_ID);
      Log.d(LOG_TAG, "Received play request: " + url);
      Log.d(LOG_TAG, "  entitled: " + title);
      Log.d(LOG_TAG, "  enqueue: " + enqueue);
      Log.d(LOG_TAG, "  play now: " + playImmediately);
      Log.d(LOG_TAG, "  stream: " + stream);

      PlaylistEntry entry;
      if (id != -1) {
        entry = retrievePlaylistEntryById(id);
        if (entry == null) {
          return;
        }
      } else {
        entry = new PlaylistEntry(id, url, title, stream, -1, storyID);
      }

      if (enqueue) {
        addPlaylistItem(entry);
        Toast.makeText(getApplicationContext(),
            R.string.msg_item_added_to_playlist, Toast.LENGTH_SHORT).show();
      }
      if (playImmediately) {
        current = entry;
        PlaybackService.setCurrent(current);
        play();
        drawer.animateOpen();
      }
    }
  }

  private static String msecToTime(int msec) {
    int sec = (msec / 1000) % 60;
    int min = (msec / 1000 / 60) % 60;
    int hour = msec / 1000 / 60 / 60;
    StringBuilder output = new StringBuilder();
    if (hour > 0) {
      output.append(hour).append(":");
      output.append(String.format("%02d", min)).append(":");
    } else {
      output.append(String.format("%d", min)).append(":");
    }
    output.append(String.format("%02d", sec));
    return output.toString();
  }

  public void updatePlayTime() {
    if (player.isPlaying()) {
      String current = msecToTime(player.getCurrentPosition());
      String total = msecToTime(player.getDuration());
      lengthText.setText(current + " / " + total);
    }
  }

  @Override
  public void onBufferingUpdate(MediaPlayer mp, int percent) {
    if (percent > 20 && percent % 5 != 0) {
      // Throttle events, since we get too many of them at first.
      return;
    }
    progressBar.setSecondaryProgress(percent);
    updatePlayTime();
  }

  @Override
  public void onCompletion(MediaPlayer mp) {

  }

  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    new AlertDialog.Builder(this).setMessage(
        "Received error: " + what + ", " + extra).setCancelable(true).show();
    setPlayButton();
    return false;
  }

  @Override
  public boolean onInfo(MediaPlayer mp, int what, int extra) {
    return false;
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    int possibleProgress = progress > seekBar.getSecondaryProgress() ? seekBar
        .getSecondaryProgress() : progress;
    if (fromUser) {
      // Only seek to position if we've downloaded the content.
      int msec = player.getDuration() * possibleProgress / seekBar.getMax();
      player.seekTo(msec);
    }
    updatePlayTime();
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    current = player.getCurrentEntry();
    resetUI();
    Log.d(LOG_TAG, "prepared and started");
    startUpdateThread();
    drawer.animateClose();
  }

  private void startUpdateThread() {
    handler.sendEmptyMessage(0);
    updateProgressThread = new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        while (true) {
          handler.sendEmptyMessage(1);
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    });
    updateProgressThread.start();
  }

  public static class PlaylistEntry {
    long id;
    final String url;
    final String title;
    final boolean isStream;
    int order;
    final String storyID;

    public PlaylistEntry(long id, String url, String title, boolean isStream,
        int order) {
      this(id, url, title, isStream, order, null);
    }

    public PlaylistEntry(long id, String url, String title, boolean isStream,
        int order, String storyID) {
      this.id = id;
      this.url = url;
      this.title = title;
      this.isStream = isStream;
      this.order = order;
      this.storyID = storyID;
    }
  }

  private void addPlaylistItem(PlaylistEntry entry) {
    ContentValues values = new ContentValues();
    values.put(Items.NAME, entry.title);
    values.put(Items.URL, entry.url);
    values.put(Items.IS_READ, false);
    values.put(Items.PLAY_ORDER, PlaylistProvider.getMax(this) + 1);
    values.put(Items.STORY_ID, entry.storyID);
    Log.d(LOG_TAG, "Adding playlist item to db");
    Uri insert = getContentResolver().insert(PlaylistProvider.CONTENT_URI,
        values);
    entry.id = ContentUris.parseId(insert);
  }

  private PlaylistEntry retrievePlaylistEntryById(long id) {
    Uri query = ContentUris.withAppendedId(PlaylistProvider.CONTENT_URI, id);
    Cursor cursor = getContentResolver().query(query, null, null, null,
        PlaylistProvider.Items.PLAY_ORDER);
    startManagingCursor(cursor);
    return getFromCursor(cursor);
  }

  private PlaylistEntry getFromCursor(Cursor c) {
    String title = null, url = null, storyID = null;
    long id;
    int order;
    if (c.moveToFirst()) {
      id = c.getInt(c.getColumnIndex(PlaylistProvider.Items._ID));
      title = c.getString(c.getColumnIndex(PlaylistProvider.Items.NAME));
      url = c.getString(c.getColumnIndex(PlaylistProvider.Items.URL));
      order = c.getInt(c.getColumnIndex(PlaylistProvider.Items.PLAY_ORDER));
      storyID = c.getString(c.getColumnIndex(PlaylistProvider.Items.STORY_ID));
      c.close();
      return new PlaylistEntry(id, url, title, false, order, storyID);
    }
    c.close();
    return null;
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

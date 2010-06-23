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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.npr.android.util.M3uParser;
import org.npr.android.util.PlaylistParser;
import org.npr.android.util.PlaylistProvider;
import org.npr.android.util.PlsParser;
import org.npr.android.util.PlaylistProvider.Items;
import org.npr.android.news.ListenActivity.PlaylistEntry;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PlaybackService extends Service implements OnPreparedListener,
    OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,
    OnInfoListener, OnSeekBarChangeListener {

  private static final String LOG_TAG = PlaybackService.class.toString();
  public static final String EXTRA_CONTENT_URL = "extra_content_url";
  public static final String EXTRA_CONTENT_TITLE = "extra_content_title";
  public static final String EXTRA_CONTENT_ID = "extra_content_id";
  public static final String EXTRA_ENQUEUE = "extra_enqueue";
  public static final String EXTRA_PLAY_IMMEDIATELY = "extra_play_immediately";
  public static final String EXTRA_STREAM = "extra_stream";
  public static final String EXTRA_STORY_ID = "extra_story_id";

  private static MediaPlayer mediaPlayer;
  public static boolean isRunning = false;
  private boolean isPlaying = false;
  private StreamProxy proxy;
  private NotificationManager notificationManager;
  private static final int NOTIFICATION_ID = 1;
  private int bindCount = 0;
  private ListenActivity parent = null;
  private static PlaylistEntry current = null;

  @Override
  public void onCreate() {
    mediaPlayer = new MediaPlayer();
    mediaPlayer.setOnBufferingUpdateListener(this);
    mediaPlayer.setOnCompletionListener(this);
    mediaPlayer.setOnErrorListener(this);
    mediaPlayer.setOnInfoListener(this);
    mediaPlayer.setOnPreparedListener(this);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    Log.w(LOG_TAG, "Playback service created");
    isRunning = true;
  }

  @Override
  public IBinder onBind(Intent arg0) {
    bindCount++;
    return new ListenBinder();
  }

  @Override
  public boolean onUnbind(Intent arg0) {
    bindCount--;
    parent = null;
    Log.w(LOG_TAG, "Unbinding PlaybackService");
    if (!mediaPlayer.isPlaying() && bindCount == 0)
      stopSelf();
    return false;
  }

  public Boolean isPlaying() {
    return isPlaying;
  }

  public PlaylistEntry getCurrentEntry() {
    PlaylistEntry c = current;
    return c;
  }

  public static void setCurrent(PlaylistEntry c) {
    current = c;
  }

  public int getPosition() {
    return mediaPlayer.getCurrentPosition();
  }

  public int getDuration() {
    return mediaPlayer.getDuration();
  }

  public int getCurrentPosition() {
    return mediaPlayer.getCurrentPosition();
  }

  public void seekTo(int pos) {
    mediaPlayer.seekTo(pos);
  }

  public void play() {
    mediaPlayer.start();
    markAsRead(current.id);
    isPlaying = true;
    int icon = R.drawable.stat_notify_musicplayer;
    CharSequence contentText = current.title;
    long when = System.currentTimeMillis();
    Notification notification = new Notification(icon, contentText, when);
    notification.flags = Notification.FLAG_NO_CLEAR
        | Notification.FLAG_ONGOING_EVENT;
    Context c = getApplicationContext();
    CharSequence title = getString(R.string.app_name);
    Intent notificationIntent = new Intent(this, Main.class);
    notificationIntent.setAction(Intent.ACTION_VIEW);
    notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (current.storyID != null) {
      notificationIntent.putExtra(Constants.EXTRA_STORY_ID, current.storyID);
    }
    PendingIntent contentIntent = PendingIntent.getActivity(c, 0,
        notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    notification.setLatestEventInfo(c, title, contentText, contentIntent);
    notificationManager.notify(NOTIFICATION_ID, notification);
  }

  public void pause() {
    mediaPlayer.pause();
    isPlaying = false;
    notificationManager.cancel(NOTIFICATION_ID);
  }

  public void stop() {
    mediaPlayer.stop();
    isPlaying = false;
    notificationManager.cancel(NOTIFICATION_ID);
  }

  public void listen(final String url, boolean stream)
      throws IllegalArgumentException, IllegalStateException, IOException {
    Log.d(LOG_TAG, "listening to " + url);

    String playUrl = url;

    // From 2.2 on (SDK ver 8), the local mediaplayer can handle Shoutcast
    // streams natively. Let's detect that, and not proxy.
    Log.w(LOG_TAG, Build.VERSION.SDK);
    int sdkVersion = 0;
    try {
      sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    } catch (NumberFormatException e) { }

    if (stream && sdkVersion < 8) {
      if (proxy == null) {
        proxy = new StreamProxy();
        proxy.init();
        proxy.start();
      }
      String proxyUrl = String.format("http://127.0.0.1:%d/%s",
          proxy.getPort(), url);
      playUrl = proxyUrl;
    }
    mediaPlayer.reset();
    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    mediaPlayer.setDataSource(playUrl);
    Log.d(LOG_TAG, "Preparing: " + playUrl);
    try {
      mediaPlayer.prepare();
      isPlaying = true;
    } catch (Exception e) {
      Log.e("", e.getMessage(), e);
    }
    Log.d(LOG_TAG, "Waiting for prepare");
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    play();
    if (parent != null) {
      parent.onPrepared(mp);
    }
  }

  @Override
  public void onDestroy() {
    parent = null;
    super.onDestroy();
    if (proxy != null) {
      proxy.stop();
    }
    mediaPlayer.stop();
    mediaPlayer.release();
    isRunning = false;
  }

  public class ListenBinder extends Binder {

    public PlaybackService getService() {
      return PlaybackService.this;
    }

    public void setListener(ListenActivity l) {
      parent = l;
    }

  }

  @Override
  public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
    if (parent != null) {
      parent.onBufferingUpdate(arg0, arg1);
    }
  }

  @Override
  public void onCompletion(MediaPlayer mp) {
    Log.w(LOG_TAG, "onComplete()");
    isPlaying = false;
    notificationManager.cancel(NOTIFICATION_ID);
    if (parent != null) {
      parent.onCompletion(mp);
    }
    playNext();
    if (bindCount == 0 && !isPlaying) {
      stopSelf();
    }
  }

  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    if (parent != null) {
      return parent.onError(mp, what, extra);
    }
    return false;
  }

  @Override
  public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
    if (parent != null) {
      return parent.onInfo(arg0, arg1, arg2);
    }
    return false;
  }

  @Override
  public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
    if (parent != null) {
      parent.onProgressChanged(arg0, arg1, arg2);
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    if (parent != null) {
      parent.onStartTrackingTouch(seekBar);
    }
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    if (parent != null) {
      parent.onStopTrackingTouch(seekBar);
    }
  }

  private void playNext() {
    Log.w(LOG_TAG, "Playing next track");
    if (current != null) {
      PlaylistEntry entry = getNextPlaylistItem(current.order);
      if (entry != null) {
        current = entry;
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
    }
    isPlaying = false;
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
      Log.e(LOG_TAG, "Unable to create InputStream for url: + url");
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

  private PlaylistEntry retrievePlaylistItem(int current, boolean next) {
    String selection = PlaylistProvider.Items.IS_READ + " = ?";
    String[] selectionArgs = new String[1];
    selectionArgs[0] = "0";
    String sort = PlaylistProvider.Items.PLAY_ORDER + (next ? " asc" : " desc");
    return retrievePlaylistItem(selection, selectionArgs, sort);
  }

  private PlaylistEntry getNextPlaylistItem(int current) {
    return retrievePlaylistItem(current, true);
  }

  private PlaylistEntry retrievePlaylistItem(String selection,
      String[] selectionArgs, String sort) {
    Cursor cursor = getContentResolver().query(PlaylistProvider.CONTENT_URI,
        null, selection, selectionArgs, sort);
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
  
  private void markAsRead(long id) {
    Uri update = ContentUris.withAppendedId(PlaylistProvider.CONTENT_URI, id);
    ContentValues values = new ContentValues();
    values.put(Items.IS_READ, true);
    @SuppressWarnings("unused")
    int result = getContentResolver().update(update, values, null, null);
  }

}

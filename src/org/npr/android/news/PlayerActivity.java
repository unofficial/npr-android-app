// Copyright 2010 Google Inc.
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
import android.app.ActivityGroup;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import org.npr.android.util.PlaylistEntry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author mfrederick@google.com (Michael Frederick)
 *
 * A base class for all Activities that want to display the default layout,
 * including the ListenView. 
 */
public abstract class PlayerActivity extends ActivityGroup implements
    Trackable, Refreshable {
  private TextView titleText;
  public abstract CharSequence getMainTitle();
  private static final String LOG_TAG = PlayerActivity.class.getName();
  private ListenView listenView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Override the normal volume controls so that the user can alter the volume
    // when a stream is not playing.
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    setContentView(R.layout.main);
    titleText = (TextView) findViewById(R.id.LogoNavText);
    titleText.setText(getMainTitle());

    listenView = new ListenView(this);
    ((ViewGroup) findViewById(R.id.MediaPlayer)).addView(listenView,
        new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,
            LayoutParams.WRAP_CONTENT));
  }

  @Override
  public boolean isRefreshable() {
    return false;
  }

  @Override
  public void refresh() {
  }

  @Override
  public void trackNow() {
  }

  @Override
  public void finish() {
    super.finish();
    noAnimation();
  }

  protected void startActivityWithoutAnimation(Intent i) {
    startActivity(i);
    noAnimation();
  }

  /**
   * Prevents the default animation on the pending transition. Only works on
   * SDK version 5 and up, but may be safely called from any version.
   */
  protected void noAnimation() {
    try {
      Method overridePendingTransition =
          Activity.class.getMethod("overridePendingTransition", new Class[] {
              int.class, int.class});
      overridePendingTransition.invoke(this, 0, 0);
    } catch (SecurityException e) {
      Log.w(LOG_TAG, "", e);
    } catch (NoSuchMethodException e) {
      // Don't log an error here; we anticipate an error on SDK < 5
    } catch (IllegalArgumentException e) {
      Log.w(LOG_TAG, "", e);
    } catch (IllegalAccessException e) {
      Log.w(LOG_TAG, "", e);
    } catch (InvocationTargetException e) {
      Log.w(LOG_TAG, "", e);
    }
  }

  private enum MenuId {
    ABOUT,
    REFRESH
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, MenuId.ABOUT.ordinal(), Menu.NONE,
      R.string.msg_main_menu_about)
      .setIcon(android.R.drawable.ic_menu_help).setAlphabeticShortcut('a');
    if (this.isRefreshable()) {
      menu.add(Menu.NONE, MenuId.REFRESH.ordinal(), Menu.NONE,
        R.string.msg_refresh).setAlphabeticShortcut('r');
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == MenuId.ABOUT.ordinal()) {
      startActivity(new Intent(this, AboutActivity.class));
      return true;
    } else if (item.getItemId() == MenuId.REFRESH.ordinal()) {
      this.refresh();
    }
    return super.onOptionsItemSelected(item);
  }

  protected void listen(PlaylistEntry entry) {
    listenView.listen(entry);
  }
}

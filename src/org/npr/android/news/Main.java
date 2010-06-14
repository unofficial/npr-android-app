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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.npr.android.util.Eula;
import org.npr.android.util.FileUtils;
import org.npr.android.util.Tracker;
import org.npr.api.ApiConstants;

public class Main extends BackAndForthActivityGroup implements
    OnClickListener {
  public static final String EXTRA_TITLE = "extra_title";

  private enum MenuId {
    EDIT,
    CLOSE,
    ABOUT
  }
  private static final String LOG_TAG = Main.class.getName();
  private TextView logoNav;
  private ProgressBar progressBar;
  private BroadcastReceiver progressReceiver;
  private BroadcastReceiver logoReceiver;
  public ImageButton refreshButton;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // First time the app is launched, it will show this. It will never show it
    // again once the user accepts the license.
    Eula.showEula(this);
    initApiKey();

    // Override the normal volume controls so that the user can alter the volume
    // when a stream is not playing.
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    setContentView(R.layout.main);

    flipper = (ViewFlipper) findViewById(R.id.MainFlipper);

    logoNav = (TextView) findViewById(R.id.LogoNavText);

//    ImageButton logoButton = (ImageButton) findViewById(R.id.LogoButton);
//    logoButton.setOnClickListener(this);

    progressBar = (ProgressBar) findViewById(R.id.ProgressBar01);
    refreshButton = (ImageButton) findViewById(R.id.RefreshButton);
    refreshButton.setOnClickListener(this);

    progressReceiver = new ProgressBroadcastReceiver();
    registerReceiver(progressReceiver,
        new IntentFilter(Constants.BROADCAST_PROGRESS));

    Intent i = new Intent(this, ListenActivity.class);
    // Ensure that only one ListenActivity can be launched. Otherwise, we may
    // get overlapping media players.
    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    Window w =
      getLocalActivityManager().startActivity(ListenActivity.class.getName(),
          i);
    View v = w.getDecorView();
    ((ViewGroup) findViewById(R.id.MediaPlayer)).addView(v,
        new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,
            LayoutParams.WRAP_CONTENT));

    // A little hacky, but otherwise we get an annoying black line where the
    // seam of the drawer's edge is.
    ((FrameLayout)((ViewGroup) v).getChildAt(0)).setForeground(null);

    logoReceiver = new LogoBroadcastReceiver();
    registerReceiver(logoReceiver, new IntentFilter(this
        .getClass().getName()));
    
    if (!checkIntentForNews(getIntent())) {
      init(new Intent(this, MainInnerActivity.class));
    }
  }
  
  @Override
  public void onNewIntent(Intent newIntent) {
    super.onNewIntent(newIntent);
    checkIntentForNews(newIntent);
  }
  
  public boolean checkIntentForNews(Intent intent) {
    if (intent.getAction().equals(Intent.ACTION_VIEW)) {
      Bundle extras = intent.getExtras();
      if (extras == null) {
        return false;
      }
      if (extras.containsKey(Constants.EXTRA_STORY_ID)) {
        Log.d(LOG_TAG, "story ID is not null");
        intent.setClass(this, NewsStoryActivity.class);
        intent.putExtra(Constants.EXTRA_DESCRIPTION, R.string.msg_main_subactivity_nowplaying);
        
        goForward(intent, false);
        return true;
      }
    }
    return false;
  }

  @Override
  public void onSuperDestroy() {
    unregisterReceiver(progressReceiver);
    unregisterReceiver(logoReceiver);
  }

  class ProgressBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      boolean showProgress =
          intent.getBooleanExtra(Constants.EXTRA_SHOW_PROGRESS, false);
      initRefreshAndProgress(showProgress);
    }
  }

  private void initRefreshAndProgress(boolean showProgress) {
    boolean showRefresh = !showProgress && getCurrentChild().isRefreshable();
    refreshButton.setVisibility(showRefresh ? View.VISIBLE : View.GONE);
    progressBar.setVisibility(showProgress ? View.VISIBLE : showRefresh ?
        View.GONE : View.INVISIBLE);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, MenuId.CLOSE.ordinal(), Menu.NONE, "Close")
        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
        .setAlphabeticShortcut('c');
    menu.add(Menu.NONE, MenuId.ABOUT.ordinal(), Menu.NONE,
        R.string.msg_main_menu_about).setIcon(android.R.drawable.ic_menu_help)
        .setAlphabeticShortcut('e');
    return (super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == MenuId.CLOSE.ordinal()) {
      finish();
      return true;
    } else if (item.getItemId() == MenuId.ABOUT.ordinal()) {
      startActivity(new Intent(this, AboutActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
//      case R.id.LogoButton:
//        goTo(0);
//        break;
      case R.id.RefreshButton:
        refresh();
        break;
    }
  }

  private void refresh() {
    Refreshable r = getCurrentChild();
    if (r.isRefreshable()) {
      r.refresh();
    }
  }

  class LogoBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String title = intent.getStringExtra(EXTRA_TITLE);
      logoNav.setText(title);
    }
  }

  private void initApiKey() {
    String key = "";
    try {
      key = FileUtils.readFile(this, R.raw.api_key).toString();
    } catch (Exception e) {
      Log.e(LOG_TAG, "", e);
    }
    if (key.equals("")) {
      new AlertDialog.Builder(this).setMessage(R.string.msg_api_key_error)
          .setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              finish();
            }
          }).create().show();
    } else {
      ApiConstants.createInstance(key.toString());
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Tracker.instance(getApplication()).finish();
  }

  @Override
  public void onChildChanged() {
    initRefreshAndProgress(false);
  }
}

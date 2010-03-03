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

import android.app.ActivityGroup;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class BackAndForthActivityGroup extends ActivityGroup {
  private static final String LOG_TAG =
      BackAndForthActivityGroup.class.getName();
  protected ViewFlipper flipper;
  private int currentLevel = -1;
  private Map<String, String> activityIds = new HashMap<String, String>();
  private List<BackAndForthActivity> activityStack =
      new ArrayList<BackAndForthActivity>();

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      Log.d(LOG_TAG, "received back key; child is "
          + flipper.getDisplayedChild());
      if (flipper.getDisplayedChild() > 0) {
        goBack();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  public void init(Intent intent) {
    currentLevel = 0;
    Window w = getLocalActivityManager().startActivity("main", intent);
    activityStack.add((BackAndForthActivity) getLocalActivityManager()
        .getActivity("main"));
    activityStack.get(currentLevel).showTitle();

    View v = w.getDecorView();
    if (v.getParent() != null) {
      ((ViewGroup) v.getParent()).removeView(v);
    }
    flipper.addView(v);
  }

  public void goForward(Intent intent, boolean destroy) {
    String className = intent.getComponent().getClassName();
    currentLevel++;
    if (destroy) {
      String id = activityIds.get(className);
      getLocalActivityManager().destroyActivity(id, true);
      activityIds.remove(className);
    }

    String id = className;
    if (destroy) {
      // Since our other activity is being destroyed asynchronously, we need to
      // generate a new id for the activity manager.
      id += new Random().toString();
    }

    activityIds.put(className, id);
    Window w = getLocalActivityManager().startActivity(id, intent);

    activityStack = activityStack.subList(0, currentLevel);
    activityStack.add((BackAndForthActivity) getLocalActivityManager()
        .getActivity(id));
    onChildChanged();
    activityStack.get(currentLevel).showTitle();
    activityStack.get(currentLevel).trackNow();

    int numChildren = flipper.getChildCount();
    if (numChildren >= currentLevel) {
      // Remove all views past the current one.
      for (int i = numChildren - 1; i >= currentLevel; i--) {
        flipper.removeViewAt(i);
      }
    }

    View v = w.getDecorView();
    if (v.getParent() != null) {
      ((ViewGroup) v.getParent()).removeView(v);
    }
    flipper.addView(v);

    flipper.setOutAnimation(this, android.R.anim.slide_out_right);
    flipper.setInAnimation(this, android.R.anim.slide_in_left);
    flipper.showNext();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    onSuperDestroy();
  }

  public abstract void onSuperDestroy();

  public abstract void onChildChanged();

  public void goBack() {
    goTo(currentLevel - 1);
  }

  public void goTo(int newLevel) {
    if (flipper.getDisplayedChild() == newLevel) {
      return;
    }
    currentLevel = newLevel;

    flipper.setOutAnimation(this, android.R.anim.slide_out_right);
    flipper.setDisplayedChild(currentLevel);
    activityStack.get(currentLevel).showTitle();
    activityStack.get(currentLevel).trackNow();
    onChildChanged();
  }

  protected BackAndForthActivity getCurrentChild() {
    return activityStack.get(currentLevel);
  }
}

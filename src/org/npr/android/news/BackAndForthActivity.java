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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

public abstract class BackAndForthActivity extends Activity implements
    Trackable, Refreshable {
  private static final String LOG_TAG = BackAndForthActivity.class.getName();

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    Activity parent = getParent();
    if (keyCode == KeyEvent.KEYCODE_BACK && parent != null) {
      Log.d(LOG_TAG, "child received key event");
      return parent.onKeyDown(keyCode, event);
    } else {
      return super.onKeyDown(keyCode, event);
    }
  }

  public void showTitle() {
    setTopTitle(getMainTitle());
  }

  public void setTopTitle(CharSequence text) {
    Intent i =
        new Intent(Main.class.getName()).putExtra(Main.EXTRA_TITLE, text);
    this.sendBroadcast(i);
  }

  public abstract CharSequence getMainTitle();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public boolean isRefreshable(){
    return false;
  }

  @Override
  public void refresh(){
  }
}

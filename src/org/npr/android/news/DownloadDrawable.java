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

import android.graphics.drawable.Drawable;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;

public class DownloadDrawable {
  private static final String LOG_TAG = DownloadDrawable.class.getName();

  public static Drawable createFromUrl(String url) {
    InputStream data = null;
    Log.d(LOG_TAG, "Starting download");
    HttpClient http = new DefaultHttpClient();
    HttpGet method = new HttpGet(url);

    HttpResponse response = null;
    try {
      response = http.execute(method);
      data = response.getEntity().getContent();
    } catch (ClientProtocolException e) {
      Log.e(LOG_TAG, "error downloading", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "error downloading", e);
    } catch (IllegalStateException e) {
      Log.e(LOG_TAG, "error downloading", e);
    }
    Log.d(LOG_TAG, "Download complete");
    return Drawable.createFromStream(data, url);
  }
}

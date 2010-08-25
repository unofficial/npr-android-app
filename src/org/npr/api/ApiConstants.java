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

package org.npr.api;

import android.net.Uri;
import android.util.Log;

import java.util.Map;
import java.util.Map.Entry;

public class ApiConstants {
  private static final String LOG_TAG = ApiConstants.class.getName();
  // Main URL
  public static final String URL = "http://api.npr.org";

  // Various endpoints of the api
  public static final String STATIONS_PATH = "stations";
  public static final String STORY_PATH = "query";
  public static final String LIST_PATH = "list";

  // Station api params
  public static final String PARAM_LAT = "lat";
  public static final String PARAM_LON = "lon";
  public static final String PARAM_ZIP = "zip";
  public static final String PARAM_CALL_LETTERS = "callLetters";
  public static final String PARAM_CITY = "city";
  public static final String PARAM_STATE = "state";

  // General params
  public static final String PARAM_ID = "id";
  public static final String PARAM_API_KEY = "apiKey";
  public static final String PARAM_SC = "sc";
  public static final String PARAM_SC_VALUE = "18";
  public static final String PARAM_FIELDS = "fields";
  public static final String PARAM_SORT = "sort";
  public static final String PARAM_DATE = "date";

  public static final String STORY_FIELDS = "title,miniTeaser,teaser,storyDate,byline,text,audio,textWithHtml,image,organization,parent";
  private final String apiKey;
  private static ApiConstants instance;

  public String createUrl(String path, Map<String, String> params) {
    String uri = String.format("%s/%s?", URL, path);
    params.put(PARAM_API_KEY, this.apiKey);
    params.put(PARAM_SC, PARAM_SC_VALUE);
    return addParams(uri, params);
  }

  public String addParams(String url, Map<String, String> params) {
    StringBuilder uri = new StringBuilder(url);
    for (Entry<String, String> param : params.entrySet()) {
      uri.append("&").append(Uri.encode(param.getKey())).append("=").append(
          Uri.encode(param.getValue()));
    }
    Log.d(LOG_TAG, uri.toString());
    return uri.toString();
  }
  
  private ApiConstants(String apiKey) {
    // Force construction through static methods
    this.apiKey = apiKey;
  }

  public static void createInstance(String apiKey) {
    if (instance == null) {
      instance = new ApiConstants(apiKey);
    }
  }

  public static ApiConstants instance() {
    return instance;
  }
}

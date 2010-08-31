// Copyright 2010 Google Inc. All Rights Reserved.
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

package org.npr.android.util;

public class PlaylistEntry {
  public long id;
  public final String url;
  public final String title;
  public final boolean isStream;
  public int order;
  public final String storyID;

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


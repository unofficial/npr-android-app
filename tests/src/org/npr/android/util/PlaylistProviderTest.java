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

package org.npr.android.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

import org.npr.android.util.PlaylistProvider;
import org.npr.android.util.PlaylistProvider.Items;
import org.npr.android.util.PlaylistProvider.PlaylistHelper;

import java.io.File;

public class PlaylistProviderTest extends AndroidTestCase {
  private PlaylistProvider provider;
  PlaylistHelper mockHelper;
  private SQLiteDatabase db;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setupDb();
  }

  private void setupDb() {
    db = SQLiteDatabase.create(null);
    Context context = new MockContext() {
      @Override
      public SQLiteDatabase openOrCreateDatabase(String file, int mode, 
          SQLiteDatabase.CursorFactory factory) {
        return db;
      }

      @Override
      public File getDatabasePath(String name) {
        return null;
      }

      @Override
      public int checkUriPermission(Uri uri, String readPermission,
          String writePermission, int pid, int uid, int modeFlags) {
        return PackageManager.PERMISSION_GRANTED;
      }
    };
    provider = new PlaylistProvider();
    provider.attachInfo(context, null);
    MockContentResolver resolver = new MockContentResolver();
    resolver.addProvider("org.npr.app.Playlist", provider);
    this.setContext(new IsolatedContext(resolver, context));
    mockHelper = new PlaylistHelper(context) {
      @Override
      public synchronized SQLiteDatabase getReadableDatabase() {
        return db;
      }
    };
    provider.setHelper(mockHelper);
  }

  private void insertRecords() {
    // Initializes the DB.
    mockHelper.getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put(Items.NAME, "A");
    values.put(Items.URL, "http://a");
    values.put(Items.IS_READ, false);
    values.put(Items.PLAY_ORDER, 0);

    db.insertOrThrow(PlaylistProvider.TABLE_NAME, Items.NAME, values);

    values.clear();
    values.put(Items.NAME, "B");
    values.put(Items.URL, "http://b");
    values.put(Items.IS_READ, false);
    values.put(Items.PLAY_ORDER, 1);

    db.insertOrThrow(PlaylistProvider.TABLE_NAME, Items.NAME, values);
    
    values.clear();
    values.put(Items.NAME, "C");
    values.put(Items.URL, "http://c");
    values.put(Items.IS_READ, false);
    values.put(Items.PLAY_ORDER, 2);

    db.insertOrThrow(PlaylistProvider.TABLE_NAME, Items.NAME, values);

    assertEquals(3, DatabaseUtils.queryNumEntries(db,
        PlaylistProvider.TABLE_NAME));
  }
  
  public void testGetMaxEmpty() {
    mockHelper.getWritableDatabase();

    assertEquals(-1, PlaylistProvider.getMax(getContext(), mockHelper));
  }

  public void testGetMax() {
    insertRecords();

    assertEquals(2, PlaylistProvider.getMax(getContext(), mockHelper));
  }
  
  public void testInsert() {
    ContentValues values = new ContentValues();
    values.put(Items.NAME, "D");
    values.put(Items.URL, "http://d");
    values.put(Items.IS_READ, false);
    values.put(Items.PLAY_ORDER, 2);
    getContext().getContentResolver().insert(PlaylistProvider.CONTENT_URI,
        values);
    
    assertEquals(1, DatabaseUtils.queryNumEntries(db,
        PlaylistProvider.TABLE_NAME));
  }
  
  public void testInsertMultiple() {
    insertRecords();
    
    ContentValues values = new ContentValues();
    values.put(Items.NAME, "D");
    values.put(Items.URL, "http://d");
    values.put(Items.IS_READ, false);
    values.put(Items.PLAY_ORDER, 2);
    getContext().getContentResolver().insert(PlaylistProvider.CONTENT_URI,
        values);
    
    assertEquals(4, DatabaseUtils.queryNumEntries(db,
        PlaylistProvider.TABLE_NAME));
  }
}

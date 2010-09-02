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

import java.util.Arrays;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class PlaylistProvider extends ContentProvider {
  public static final Uri CONTENT_URI = Uri
      .parse("content://org.npr.android.util.Playlist");
  private static final String DATABASE_NAME = "playlist.db";
  protected static final int DATABASE_VERSION = 2;
  protected static final String TABLE_NAME = "items";
  private static final String LOG_TAG = PlaylistProvider.class.getName();
  private PlaylistHelper helper;

  /**
   * For testing purposes, allows to override the existing helper so we don't
   * touch the actual filesystem.
   * 
   * @param helper
   */
  protected void setHelper(PlaylistHelper helper) {
    this.helper = helper;
  }

  public static int getMax(Context context) {
    PlaylistHelper temp = new PlaylistHelper(context);
    int result = getMax(context, temp);
    temp.close();
    return result;
  }

  /**
   * Useful for testing.
   * 
   * @param context
   * @param helper
   * @return The max play order, or -1 if there are no entries in the playlist
   */
  protected static int getMax(Context context, PlaylistHelper helper) {
    SQLiteDatabase db = helper.getReadableDatabase();
    
    if (DatabaseUtils.queryNumEntries(db, TABLE_NAME) == 0) {
      return -1;
    }

    return (int) DatabaseUtils.longForQuery(db, "select max(play_order) from "
        + TABLE_NAME, null);
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    SQLiteDatabase db = helper.getWritableDatabase();
    long id = ContentUris.parseId(uri);
    String realSelection = getSelectionFromId(uri, selection);
    int result = db.delete(TABLE_NAME, realSelection, selectionArgs);
    return result;
  }

  @Override
  public String getType(Uri arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    SQLiteDatabase db = helper.getWritableDatabase();
    long id = db.insert(TABLE_NAME, Items.NAME, values);
    return ContentUris.withAppendedId(uri, id);
  }

  @Override
  public boolean onCreate() {
    helper = new PlaylistHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteDatabase db = helper.getWritableDatabase();
    String realSelection = getSelectionFromId(uri, selection);

    Cursor result = db.query(TABLE_NAME, projection, realSelection,
        selectionArgs, null /* no group by */, null /* no having */, sortOrder);
    Log.d(LOG_TAG, uri.toString() + ";" + realSelection + ";"
        + Arrays.toString(selectionArgs));
    return result;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    SQLiteDatabase db = helper.getWritableDatabase();
    String realSelection = getSelectionFromId(uri, selection);
    Log.d(LOG_TAG, "update where " + realSelection);
    int result = db.update(TABLE_NAME, values, realSelection, selectionArgs);
    return result;
  }

  private String getSelectionFromId(Uri uri, String selection) {
    long id = ContentUris.parseId(uri);
    String realSelection = selection == null ? "" : selection + " and ";
    if (id != -1) {
      realSelection += Items._ID + " = " + id;
      return realSelection;
    }
    return selection;
  }

  public static class Items implements BaseColumns {
    public static final String NAME = "name";
    public static final String URL = "url";
    public static final String PLAY_ORDER = "play_order";
    public static final String IS_READ = "is_read";
    public static final String STORY_ID = "story_id";
    public static final String[] COLUMNS = { NAME, URL, PLAY_ORDER, IS_READ,
        STORY_ID };
    public static final String[] ALL_COLUMNS = { BaseColumns._ID, NAME, URL,
        PLAY_ORDER, IS_READ, STORY_ID };

    // This class cannot be instantiated
    private Items() {
    }
  }

  protected static class PlaylistHelper extends SQLiteOpenHelper {
    PlaylistHelper(Context context) {
      super(context, DATABASE_NAME, null /* no cursor factory */,
          DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Items._ID
          + " INTEGER PRIMARY KEY," + Items.NAME + " TEXT," + Items.URL
          + " VARCHAR," + Items.IS_READ + " BOOLEAN," + Items.PLAY_ORDER
          + " INTEGER," + Items.STORY_ID + " TEXT" + ");");
    }

    @SuppressWarnings("unused")
    private void dropTable(SQLiteDatabase db) {
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(PlaylistHelper.class.getName(), "Upgrading database from version "
          + oldVersion + " to " + newVersion);
      if (newVersion <= 3) {
        try {
          // TODO: This is kind of a hack, and it would be better to check for
          // the existence of the column first.
          db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN "
              + Items.STORY_ID + " TEXT DEFAULT NULL;");
        } catch (SQLException error) {
          Log.e(LOG_TAG, error.toString());
        }
      }
    }
  }
}

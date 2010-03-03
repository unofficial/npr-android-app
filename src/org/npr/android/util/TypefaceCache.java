package org.npr.android.util;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;

public class TypefaceCache {
  private static HashMap<String, Typeface> map =
      new HashMap<String, Typeface>();

  public static Typeface getTypeface(String file, Context context) {
    Typeface result = map.get(file);
    if (result == null) {
      result = Typeface.createFromAsset(context.getAssets(), file);
      map.put(file, result);
    }
    return result;
  }
}

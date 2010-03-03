package org.npr.android.util;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtils {
  public static CharSequence readFile(Activity activity, int id) {
    BufferedReader in = null;
    try {
      in =
          new BufferedReader(new InputStreamReader(activity.getResources()
              .openRawResource(id)));
      String line;
      StringBuilder buffer = new StringBuilder();
      while ((line = in.readLine()) != null) {
        buffer.append(line).append('\n');
      }
      // Chomp the last newline
      buffer.deleteCharAt(buffer.length() - 1);
      return buffer;
    } catch (IOException e) {
      return "";
    } finally {
      closeStream(in);
    }
  }

  /**
   * Closes the specified stream.
   * 
   * @param stream The stream to close.
   */
  private static void closeStream(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // Ignore
      }
    }
  }
}

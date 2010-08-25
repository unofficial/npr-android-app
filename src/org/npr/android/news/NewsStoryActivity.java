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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.npr.android.util.Tracker;
import org.npr.android.util.TypefaceCache;
import org.npr.android.util.Tracker.LinkEvent;
import org.npr.android.util.Tracker.PlayLaterEvent;
import org.npr.android.util.Tracker.PlayNowEvent;
import org.npr.android.util.Tracker.StoryDetailsMeasurement;
import org.npr.api.Story;
import org.npr.api.Story.Audio;
import org.npr.api.Story.Byline;
import org.npr.api.Story.Parent;
import org.npr.api.Story.TextWithHtml;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

public class NewsStoryActivity extends BackAndForthActivity implements
    OnClickListener {
  private static String LOG_TAG = NewsStoryActivity.class.getName();
  private String description;
  private Story story;
  private String storyId;
  private String title;
  private String topicId;
  private String orgId;
  private ImageView icon;
  private Drawable iconDrawable;
  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 0:
          icon.setImageDrawable(iconDrawable);
          icon.setVisibility(View.VISIBLE);
          break;
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    storyId = getIntent().getStringExtra(Constants.EXTRA_STORY_ID);
    story = NewsListActivity.getStoryFromCache(storyId);
    description = getIntent().getStringExtra(Constants.EXTRA_DESCRIPTION);

    if (story == null) {
      return;
    }
    Intent i =
        new Intent(Constants.BROADCAST_PROGRESS).putExtra(
            Constants.EXTRA_SHOW_PROGRESS, true);
    sendBroadcast(i);
    setContentView(R.layout.news_story);

    orgId =
        story.getOrganizations().size() > 0 ? story.getOrganizations().get(0)
            .getId() : null;

    for (Parent p : story.getParentTopics()) {
      if (p.isPrimary()) {
        topicId = p.getId();
        break;
      }
    }
    icon = (ImageView) findViewById(R.id.NewsStoryIcon);
    TextView title = (TextView) findViewById(R.id.NewsStoryTitleText);
    TextView dateline = (TextView) findViewById(R.id.NewsStoryDateline);
    Button listenNow =
        (Button) findViewById(R.id.NewsStoryListenNowButton);
    Button enqueue =
      (Button) findViewById(R.id.NewsStoryListenEnqueueButton);
    ImageButton share = (ImageButton) findViewById(R.id.NewsStoryShareButton);
    WebView textView = (WebView) findViewById(R.id.NewsStoryWebView);
    textView.setBackgroundColor(0);

    title.setText(story.getTitle());
    title.setTypeface(TypefaceCache.getTypeface("fonts/Georgia.ttf",
        this));
    this.title = story.getTitle();

    // Sample date from api: Tue, 09 Jun 2009 15:20:00 -0400
    SimpleDateFormat longDateFormat =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    DateFormat shortDateFormat =
        DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
    StringBuilder datelineText = new StringBuilder();
    try {
      datelineText.append(shortDateFormat.format(longDateFormat.parse(story
          .getStoryDate())));
    } catch (ParseException e) {
      Log.e(LOG_TAG, "date format", e);
    }

    Iterator<Byline> bylines = story.getBylines().iterator();
    if (bylines.hasNext()) {
      datelineText.append(" - by ");
    }
    while (bylines.hasNext()) {
      Byline b = bylines.next();
      datelineText.append(b.getName());
      if (bylines.hasNext()) {
        datelineText.append(", ");
      }
    }
    if (datelineText.length() == 0) {
      dateline.setVisibility(View.GONE);      
    }
    dateline.setText(datelineText.toString());

    TextWithHtml text = story.getTextWithHtml();
    String textHtml;
    if (text != null) {
      StringBuilder sb = new StringBuilder();
      for (String paragraph : text.getParagraphs()) {
        sb.append("<p>").append(paragraph).append("</p>");
      }
      textHtml = String.format(HTML_FORMAT, sb.toString());
      // WebView can't load external images, so we need to strip them or it
      // may not render.
      textHtml = textHtml.replaceAll("<img .*/>", "");
    } else {
      // Only show the teaser if there is no full-text.
      textHtml =
          String.format(HTML_FORMAT, "<p class='teaser'>" + story.getTeaser()
              + "</p>");
    }
    textView.loadDataWithBaseURL(null, textHtml, "text/html", "utf-8", null);

    if (story.getImages().size() > 0) {
      final String url = story.getImages().get(0).getSrc();
      Thread imageInitThread = new Thread(new Runnable() {
        public void run() {
          iconDrawable = DownloadDrawable.createFromUrl(url);
//          if (iconDrawable.getBounds().height() > 0) {
            handler.sendEmptyMessage(0);
//          }
        }
      });
      imageInitThread.start();
    }

    listenNow.setOnClickListener(this);
    enqueue.setOnClickListener(this);
    share.setOnClickListener(this);
    boolean isListenable = getPlayableUrl(getPlayable()) != null;
    listenNow.setVisibility(isListenable ? View.VISIBLE : View.GONE);
    listenNow.setEnabled(isListenable);
    enqueue.setVisibility(isListenable ? View.VISIBLE : View.GONE);
    enqueue.setEnabled(isListenable);

    i = new Intent(Constants.BROADCAST_PROGRESS).putExtra(
          Constants.EXTRA_SHOW_PROGRESS, false);
    sendBroadcast(i);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.NewsStoryListenNowButton:
        playStory(true);
        break;
      case R.id.NewsStoryListenEnqueueButton:
        playStory(false);
        break;
      case R.id.NewsStoryShareButton:
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, story.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(
            "%s: http://www.npr.org/%s", story.getTitle(), story.getId()));
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent,
            getString(R.string.msg_share_story)));
        break;
    }
  }

  private void playStory(boolean playNow) {
    Audio a = getPlayable();
    Intent i =
      new Intent(ListenActivity.class.getName()).putExtra(
            ListenActivity.EXTRA_CONTENT_URL, getPlayableUrl(a)).putExtra(
            ListenActivity.EXTRA_CONTENT_TITLE, story.getTitle()).putExtra(
            ListenActivity.EXTRA_ENQUEUE, true).putExtra(Constants.EXTRA_STORY_ID, storyId);
    LinkEvent e;
    if (playNow) {
      i.putExtra(ListenActivity.EXTRA_PLAY_IMMEDIATELY, true);
      e = new PlayNowEvent(storyId, story.getTitle(), a.getId());
    } else {
      e = new PlayLaterEvent(storyId, story.getTitle(), a.getId());
    }
    sendBroadcast(i);
    Tracker.instance(getApplication()).trackLink(e);
  }
  
  private Audio getPlayable() {
    for (Audio a : story.getAudios()) {
      if (a.getType().equals("primary")) {
        return a;
      }
    }
    return null;
  }

  private String getPlayableUrl(Audio a) {
    String url = null;
    if (a != null) {
      for (Audio.Format f : a.getFormats()) {
        if ((url = f.getMp3()) != null) {
          return url;
        }
      }
    }
    return url;
  }

  // WebView is default black text.
  public static final String HTML_FORMAT =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">" +
      "<html><head><title></title>" +
      "<style type=\"text/css\">" +
      "body {color:#000; margin:0}" +
      "a {color:blue}" +
      ".teaser {font-size: 10pt}" +
      "</style>" +
      "</head>" +
      "<body>" +
      "%s" +
      "</body></html>";

  @Override
  public CharSequence getMainTitle() {
    return description;
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder(storyId).append("-");
    pageName.append(title);
    Tracker.instance(getApplication()).trackPage(
        new StoryDetailsMeasurement(pageName.toString(), "News", orgId, topicId,
            storyId));
  }
}

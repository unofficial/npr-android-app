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

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.client.ClientProtocolException;
import org.npr.android.util.TypefaceCache;
import org.npr.api.Client;
import org.npr.api.Story;
import org.npr.api.Story.Audio;
import org.npr.api.Story.StoryFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;


public class NewsListAdapter extends ArrayAdapter<Story> {
  private static final String LOG_TAG = NewsListAdapter.class.getName();
  private LayoutInflater inflater;
  private static Typeface headlineTypeface = null;

  public NewsListAdapter(Context context) {
    super(context, R.layout.news_item);
    inflater = LayoutInflater.from(getContext());
    if (headlineTypeface == null) {
      headlineTypeface =
          TypefaceCache.getTypeface("fonts/Georgia.ttf", context);
    }
  }

  private List<Story> moreStories;
  private boolean endReached = false;
  
  private Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      Intent i =
        new Intent(Constants.BROADCAST_PROGRESS).putExtra(
            Constants.EXTRA_SHOW_PROGRESS, false);
      NewsListAdapter.this.getContext().sendBroadcast(i);
      if (moreStories != null) {
        remove(null);
        for (Story s : moreStories) {
          if (getPosition(s) < 0) {
            add(s);
          }
        }
        if (!endReached) {
          add(null);          
        }
      }
    }
  };

  private boolean isPlayable(Story story) {
    for (Audio a : story.getAudios()) {
      if (a.getType().equals("primary")) {
        for (Audio.Format f : a.getFormats()) {
          if (f.getMp3() != null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.news_item, parent, false);
    }

    Story story = getItem(position);

    ImageView image = (ImageView) convertView.findViewById(R.id.NewsItemImage);
    TextView teaser = (TextView) convertView.findViewById(R.id.NewsItemTeaserText);
    TextView name = (TextView) convertView.findViewById(R.id.NewsItemNameText);      

    if (story != null) {
      image.setImageDrawable(getContext().getResources().getDrawable(
          isPlayable(story) ? R.drawable.icon_listen_main : R.drawable.bullet));
      image.setVisibility(View.VISIBLE);
      name.setTypeface(headlineTypeface, Typeface.NORMAL);
      name.setText(story.toString());
      String teaserText = story.getMiniTeaser();
      if (teaserText == null) {
        teaserText = story.getTeaser();
      }
      if (teaserText != null && teaserText.length() > 0) {
        // Disable for now.
//        teaser.setText(story.getTeaser());
//        teaser.setVisibility(View.VISIBLE);
      } else {
        teaser.setVisibility(View.INVISIBLE);
      }
      teaser.setVisibility(View.GONE);
    } else {
      // null marker means it's the end of the list.
      image.setVisibility(View.INVISIBLE);
      teaser.setVisibility(View.INVISIBLE);
      name.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
      name.setText(R.string.msg_load_more);
    }
    return convertView;
  }

  public void addMoreStories(final String url, final int count) {
    Intent i =
      new Intent(Constants.BROADCAST_PROGRESS).putExtra(
          Constants.EXTRA_SHOW_PROGRESS, true);
    this.getContext().sendBroadcast(i);
    new Thread(new Runnable() {
      @Override
      public void run() {
        // TODO Auto-generated method stub
        getMoreStories(url, count);
        handler.sendEmptyMessage(0);
      }
    }).start();
  }
  
  private void getMoreStories(String url, int count) {
    Node stories = null;
    try {
      stories = new Client(url).execute();
    } catch (ClientProtocolException e) {
      Log.e(LOG_TAG, "", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "", e);
    } catch (SAXException e) {
      Log.e(LOG_TAG, "", e);
    } catch (ParserConfigurationException e) {
      Log.e(LOG_TAG, "", e);
    }

    Log.d(LOG_TAG, "stories: " + stories.getNodeName());

    moreStories = StoryFactory.parseStories(stories);
    if (moreStories != null) {
      if (moreStories.size() < count) {
        endReached = true;
      }
      NewsListActivity.addAllToStoryCache(moreStories);
    }
  }
}

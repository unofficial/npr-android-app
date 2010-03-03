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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import org.apache.http.client.ClientProtocolException;
import org.npr.api.Client;
import org.npr.api.Station;
import org.npr.api.Station.StationFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class StationListAdapter extends ArrayAdapter<Station> {
  private static final String LOG_TAG = StationListAdapter.class.getName();
  private List<Station> data;
  public StationListAdapter(Context context, int resource,
      int textViewResource) {
    super(context, resource, textViewResource);
  }

  public void initializeList(String url) {
    // If we fail, then the list will be empty.
    data = new LinkedList<Station>();

    Node stations = null;
    try {
      stations = new Client(url).execute();
    } catch (ClientProtocolException e) {
      Log.e(LOG_TAG, "", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "", e);
    } catch (SAXException e) {
      Log.e(LOG_TAG, "", e);
    } catch (ParserConfigurationException e) {
      Log.e(LOG_TAG, "", e);
    }

    if (stations == null) {
      return;
    }

    Log.d(LOG_TAG, "stations: " + stations.getNodeName());

    data = StationFactory.parseStations(stations);
    StationListActivity.addAllToStationCache(data);
  }

  public void showData() {
    this.clear();
    for (Station station : data) {
      this.add(station);
      Log.d(LOG_TAG, "adding station: " + station);
    }
    notifyDataSetChanged();
    Log.d(LOG_TAG, "initialized station list");
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View line = super.getView(position, convertView, parent);
    ImageView image =
        (ImageView) line.findViewById(R.id.StationItemPlayableImage);
    ImageView placeholder =
      (ImageView) line.findViewById(R.id.StationItemPlaceholderImage);
    Station station = getItem(position);
    boolean hasAudio =
        station.getPodcasts().size() + station.getAudioStreams().size() > 0;
    image.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
    placeholder.setVisibility(hasAudio ? View.GONE : View.VISIBLE);
    return line;
  }
}

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

import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

public class Station {
  public static final String LOG_TAG = Station.class.getName();
  private final String id;
  private final String name;
  private final String marketCity;
  private final String frequency;
  private final String band;
  private final List<AudioStream> audioStreams;
  private final List<Podcast> podcasts;
  private final String tagline;
  private final String image;

  public static class Listenable {
    private final String url;
    private final String title;
    
    public Listenable(String url, String title) {
      this.url = url;
      this.title = title;
    }

    public String getUrl() {
      return url;
    }

    public String getTitle() {
      return title;
    }
  }
  
  public static class AudioStream extends Listenable {
    public AudioStream(String url, String title) {
      super(url, title);
    }
  }
  
  public static class Podcast extends Listenable {
    public Podcast(String url, String title) {
      super(url, title);
    }
  }

  private Station(String id, String name, String city, String frequency,
      String band, List<AudioStream> audioStreams, List<Podcast> podcasts,
      String tagline, String image) {
    this.id = id;
    this.name = name;
    this.marketCity = city;
    this.frequency = frequency;
    this.band = band;
    this.audioStreams = audioStreams;
    this.podcasts = podcasts;
    this.tagline = tagline;
    this.image = image;
  }

  public String getTagline() {
    return tagline;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getMarketCity() {
    return marketCity;
  }

  public String getFrequency() {
    return frequency;
  }

  public String getBand() {
    return band;
  }

  public List<AudioStream> getAudioStreams() {
    return audioStreams;
  }
  
  public List<Podcast> getPodcasts() {
    return podcasts;
  }
  
  public String getImage() {
    return image;
  }
  
  @Override
  public String toString() {
    return new StringBuilder()
        .append(name).append(" - ")
        .append(frequency).append(" ")
        .append(band).append(", ")
        .append(marketCity)
        .toString();
  }

  public static class StationBuilder {
    private final String id;
    private String name;
    private String marketCity;
    private String frequency;
    private String band;
    private List<AudioStream> audioStreams;
    private List<Podcast> podcasts;
    private String tagline;
    private String image;

    public StationBuilder(String id) {
      this.id = id;
    }

    public StationBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public StationBuilder withMarketCity(String city) {
      this.marketCity = city;
      return this;
    }

    public StationBuilder withFrequency(String frequency) {
      this.frequency = frequency;
      return this;
    }

    public StationBuilder withBand(String band) {
      this.band = band;
      return this;
    }

    public StationBuilder withAudioStreams(List<AudioStream> audioStreams) {
      this.audioStreams = audioStreams;
      return this;
    }
    
    public StationBuilder withPodcasts(List<Podcast> podcasts) {
      this.podcasts = podcasts;
      return this;
    }
    
    public StationBuilder withTagline(String tagline) {
      this.tagline = tagline;
      return this;
    }

    public StationBuilder withImage(String image) {
      this.image = image;
      return this;
    }

    public Station build() {
      return new Station(id, name, marketCity, frequency, band, audioStreams,
          podcasts, tagline, image);
    }
  }

  public static class StationFactory {
    public static List<Station> parseStations(Node rootNode) {
      List<Station> result = new ArrayList<Station>();
      NodeList stationList = rootNode.getChildNodes();
      for (Node stationNode : new IterableNodeList(stationList)) {
        Station station = createStation(stationNode);
        if (station != null) {
          result.add(station);
        }
      }
      return result;
    }

    private static Station createStation(Node node) {
      if (!node.getNodeName().equals("station") ||
          !node.hasChildNodes()) {
        return null;
      }
      StationBuilder sb = new StationBuilder(node.getAttributes().getNamedItem(
              "id").getNodeValue());
      List<AudioStream> streams = new LinkedList<AudioStream>();
      List<Podcast> podcasts = new LinkedList<Podcast>();
      for (Node n : new IterableNodeList(node.getChildNodes())) {
        String nodeName = n.getNodeName();
        Node nodeChild = n.getChildNodes().item(0);
        if (nodeName.equals("name")) {
          sb.withName(nodeChild.getNodeValue());
        } else if (nodeName.equals("band") && nodeChild != null) {
          sb.withBand(nodeChild.getNodeValue());
        } else if (nodeName.equals("frequency") && nodeChild != null) {
          sb.withFrequency(nodeChild.getNodeValue());
        } else if (nodeName.equals("marketCity") && nodeChild != null) {
          sb.withMarketCity(nodeChild.getNodeValue());
        } else if (nodeName.equals("tagline") && nodeChild != null) {
          sb.withTagline(nodeChild.getNodeValue());
        } else if (nodeName.equals("image") && nodeChild != null) {
          sb.withImage(nodeChild.getNodeValue());
        } else if (nodeName.equals("url")) {
          Attr typeIdAttr = (Attr) n.getAttributes().getNamedItem("typeId");
          Attr typeAttr = (Attr) n.getAttributes().getNamedItem("type");
          Attr titleAttr = (Attr) n.getAttributes().getNamedItem("title");
          if (typeIdAttr != null && typeAttr != null) {
            String typeId = typeIdAttr.getValue();
            String type = typeAttr.getValue();
            String url = nodeChild.getNodeValue();
            String title = titleAttr.getValue();
            if (typeId.equals("10") && type.equals("Audio MP3 Stream")
                && nodeChild != null) {
              streams.add(new AudioStream(url, title));
            }
            if (typeId.equals("9") && type.equals("Podcast")
                && nodeChild != null) {
              podcasts.add(new Podcast(url, title));
            }
          }
        }
      }
      sb.withAudioStreams(streams);
      sb.withPodcasts(podcasts);
      return sb.build();
    }

    public static Station downloadStation(String stationId) {
      Log.d(LOG_TAG, "downloading station: " + stationId);
      Map<String, String> params = new HashMap<String, String>();
      params.put(ApiConstants.PARAM_ID, stationId);
      String url =
          ApiConstants.instance().createUrl(ApiConstants.STATIONS_PATH, params);

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
        return null;
      }
      Log.d(LOG_TAG, "node " + stations.getNodeName() + " "
          + stations.getChildNodes().getLength());
      List<Station> result = parseStations(stations);
      return result.size() > 0 ? result.get(0) : null;
    }
  }
}
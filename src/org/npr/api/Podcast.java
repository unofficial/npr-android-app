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
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class Podcast {
  private final String title;
  private final String summary;
  private final String link;
  private final List<Item> items;

  public Podcast(String title, String summary, String link, List<Item> items) {
    this.title = title;
    this.summary = summary;
    this.link = link;
    this.items = items;
  }

  public static class Item {
    private final String title;
    private final String pubDate;
    private final String guid;
    private final String url;
    public Item(String title, String pubDate, String guid, String url) {
      this.title = title;
      this.pubDate = pubDate;
      this.guid = guid;
      this.url = url;
    }

    public String getTitle() {
      return title;
    }
    public String getPubDate() {
      return pubDate;
    }
    public String getGuid() {
      return guid;
    }
    public String getUrl() {
      return url;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public String getLink() {
    return link;
  }

  public List<Item> getItems() {
    return items;
  }

  public static class PodcastBuilder {
    private String title;
    private String summary;
    private String link;
    private List<Item> items = new LinkedList<Item>();

    public PodcastBuilder withTitle(String title) {
      this.title = title;
      return this;
    }

    public PodcastBuilder withSummary(String summary) {
      this.summary = summary;
      return this;
    }

    public PodcastBuilder withLink(String link) {
      this.title = link;
      return this;
    }

    public PodcastBuilder withItem(Item item) {
      this.items.add(item);
      return this;
    }

    public Podcast build() {
      return new Podcast(title, summary, link, items);
    }
  }

  public static class PodcastFactory {
    private static final String LOG_TAG = PodcastFactory.class.getName();

    private static Podcast parsePodcast(Node rootNode) {
      if (!rootNode.getNodeName().equals("rss") ||
          !rootNode.hasChildNodes()) {
        return null;
      }
      PodcastBuilder pb = new PodcastBuilder();
      // Presumes one channel
      Node channel = null;
      for (Node n : new IterableNodeList(rootNode.getChildNodes())) {
        String nodeName = n.getNodeName();
        if (nodeName.equals("channel")) {
          channel = n;
          break;
        }
      }
      if (channel == null) {
        return null;
      }
      for (Node n : new IterableNodeList(channel.getChildNodes())) {
        String nodeName = n.getNodeName();
        Node nodeChild = n.getChildNodes().item(0);
        if (nodeName.equals("title")) {
          pb.withTitle(nodeChild.getNodeValue());
        } else if (nodeName.equals("link") && nodeChild != null) {
          pb.withLink(nodeChild.getNodeValue());
        } else if (nodeName.equals("itunes:summary") && nodeChild != null) {
          pb.withSummary(nodeChild.getNodeValue());
        } else if (nodeName.equals("item")) {
          Item item = createItem(n);
          if (item != null) {
            pb.withItem(item);
          }
        }
      }
      return pb.build();
    }

    private static Item createItem(Node node) {
      if (!node.getNodeName().equals("item") ||
          !node.hasChildNodes()) {
        return null;
      }
      String title = null, guid = null, url = null, pubDate = null;
      for (Node n : new IterableNodeList(node.getChildNodes())) {
        String nodeName = n.getNodeName();
        Node nodeChild = n.getChildNodes().item(0);
        if (nodeName.equals("title")) {
          title = nodeChild.getNodeValue();
        } else if (nodeName.equals("guid") && nodeChild != null) {
          guid = nodeChild.getNodeValue();
        } else if (nodeName.equals("pubDate") && nodeChild != null) {
          pubDate = nodeChild.getNodeValue();
        } else if (nodeName.equals("enclosure")) {
          Attr urlAttr = (Attr) n.getAttributes().getNamedItem("url");
          if (urlAttr != null) {
            url = urlAttr.getNodeValue();
          }
        }
        if (title != null && pubDate != null && guid != null && url != null) {
          // There may be multiple enclosures. Use the first one.
          // TODO(mfrederick): Support multiple enclosures.
          break;
        }
      }
      return new Item(title, pubDate, guid, url);
    }

    public static Podcast downloadPodcast(String url) {
      Log.d(LOG_TAG, "downloading podcast: " + url);

      Node podcasts = null;
      try {
        podcasts = new Client(url).execute();
      } catch (ClientProtocolException e) {
        Log.e(LOG_TAG, "", e);
      } catch (IOException e) {
        Log.e(LOG_TAG, "", e);
      } catch (SAXException e) {
        Log.e(LOG_TAG, "", e);
      } catch (ParserConfigurationException e) {
        Log.e(LOG_TAG, "", e);
      }

      if (podcasts == null) {
        return null;
      }
      Log.d(LOG_TAG, "node " + podcasts.getNodeName() + " "
          + podcasts.getChildNodes().getLength());
      Podcast result = parsePodcast(podcasts);
      return result;
    }
  }
}

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

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class Client {
  private static final String LOG_TAG = Client.class.getName();
  private final String url;

  public Client(String url) {
    this.url = url;
  }

  public Node execute() throws ClientProtocolException, IOException,
      SAXException, ParserConfigurationException {
    InputStream data = download();

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setValidating(false);
    dbf.setNamespaceAware(false);

    if (data != null) {
      Document document = dbf.newDocumentBuilder().parse(data);
      Log.d(LOG_TAG, "DOM parsed");
      return document.getDocumentElement();
    }
    return null;
  }

  public void sax(ContentHandler handler) {
    try {
      XMLReader xr =
          SAXParserFactory.newInstance().newSAXParser().getXMLReader();
      xr.setContentHandler(handler);
      xr.parse(new InputSource(download()));
    } catch (SAXException e) {
      Log.e(LOG_TAG, "error creating parser", e);
    } catch (ParserConfigurationException e) {
      Log.e(LOG_TAG, "error creating parser", e);
    } catch (FactoryConfigurationError e) {
      Log.e(LOG_TAG, "error creating parser", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "error parsing", e);
    }
  }

  private InputStream download() {
    InputStream data = null;
    Log.d(LOG_TAG, "Starting download: " + url);
    HttpClient http = new DefaultHttpClient();
    HttpGet method = new HttpGet(url);

    HttpResponse response = null;
    try {
      response = http.execute(method);
      data = response.getEntity().getContent();
    } catch (ClientProtocolException e) {
      Log.e(LOG_TAG, "error downloading", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "error downloading", e);
    } catch (IllegalStateException e) {
      Log.e(LOG_TAG, "error downloading", e);
    }
    Log.d(LOG_TAG, "Download complete");
    return data;
  }
}
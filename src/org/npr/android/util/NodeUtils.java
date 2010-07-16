// Copyright 2010 Google Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.npr.api.IterableNodeList;
import org.w3c.dom.Node;

import android.util.Log;

public class NodeUtils {
  public static final String LOG_TAG = NodeUtils.class.getName();
  private static boolean getTextContentAvailable = true;
  private static Method getTextContentMethod = null;
 
  private final static int MAX_RECURSION_DEPTH = 10;

  /**
   * Node#getTextContent() is only available in Froyo and later.
   * Call getTextContent if it exists else use local implementation
   * @param node
   * @return text content of this node and its descendant
   */
  public static String getTextContent(Node node) {
    if (getTextContentAvailable && getTextContentMethod == null) {
      try {
        getTextContentMethod = Node.class.getMethod(
            "getTextContent", (Class[]) null );
      } catch (NoSuchMethodException nsme) {
        // failure, must be older device
        getTextContentAvailable = false;
      }
    }

    if (getTextContentAvailable) {
      try {
        String value = (String) getTextContentMethod.invoke(node);
        return value;
      } catch (IllegalArgumentException e1) {
        getTextContentAvailable = false;
      } catch (IllegalAccessException e1) {
        getTextContentAvailable = false;
      } catch (InvocationTargetException e1) {
        getTextContentAvailable = false;
      }
    }

    // getTextContent doesn't exist.
     return getTextContentImpl(node, 0);
  }
  
  /**
   * implementation based on Javadoc description of getTextContent
   * @param node
   * @return text content of this node and its descendant
   */
  private static String getTextContentImpl(Node node, int recursionDepth) {
    // should never happen but don't allow too much recursion
    if (recursionDepth > MAX_RECURSION_DEPTH) {
      Log.d(LOG_TAG, "too much recursion!");
      return "";
    }
    
    switch (node.getNodeType()) {
    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
    case Node.COMMENT_NODE:
    case Node.PROCESSING_INSTRUCTION_NODE:
      return node.getNodeValue();

    case Node.ELEMENT_NODE:
    case Node.ATTRIBUTE_NODE:
    case Node.ENTITY_NODE:
    case Node.ENTITY_REFERENCE_NODE:
    case Node.DOCUMENT_FRAGMENT_NODE:
      StringBuffer value = new StringBuffer();
      for (Node childNode: new IterableNodeList(node.getChildNodes())) {
        int childNodeType = childNode.getNodeType();
        if (childNodeType != Node.COMMENT_NODE 
            && childNodeType != Node.PROCESSING_INSTRUCTION_NODE) {
          value.append(getTextContentImpl(childNode, recursionDepth + 1));
        }
      }
      return value.toString();
    }

    Log.d(LOG_TAG, "unexpected node type: " + node.getNodeType());
    return null;
  }
}
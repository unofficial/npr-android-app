package org.npr.android.util;

import android.app.Application;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Tracker {
  private static Tracker tracker;
  private static final String LOG_TAG = Tracker.class.getName();
  private static final boolean OMNITURE_PRESENT;
  private static final String OMNITURE_APP_MEASUREMENT =
      "com.omniture.android.AppMeasurement";
  @SuppressWarnings("unchecked")
  private static Class MEASUREMENT_CLASS;

  private ExecutorService executorService;
  private Object omnitureMeasurement;
  private final Application application;

  public static final String PAGE_NAME_SEPARATOR = ": ";
  public static Tracker instance(Application application) {
    if (tracker == null) {
      tracker = new Tracker(application);
    }
    tracker.begin();
    return tracker;
  }

  static {
    try {
      MEASUREMENT_CLASS = Class.forName(OMNITURE_APP_MEASUREMENT);
    } catch (ClassNotFoundException e) {
      MEASUREMENT_CLASS = null;
    }
    OMNITURE_PRESENT = MEASUREMENT_CLASS != null;
  }

  public Tracker(Application application) {
    this.application = application;
  }

  /**
   * Must be called after instantiation, or when main Activity is restarted.
   */
  private void begin() {
    executorService = Executors.newSingleThreadExecutor();
  }

  public void finish() {
    try {
      // Attempt to let the tracker do its job before we die.
      executorService.shutdown();
      // Give it one second
      executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "awaiting tracker completion", e);
    }
  }

  private Object createMeasurement() throws IllegalAccessException,
      InstantiationException, SecurityException, NoSuchMethodException,
      IllegalArgumentException, InvocationTargetException {
    Object result = null;
    if (omnitureMeasurement != null) {
      result = omnitureMeasurement;

      Method clearVars = MEASUREMENT_CLASS.getMethod("clearVars");
      clearVars.invoke(result);
    } else {
      Class[] typeList = { Application.class };
      Constructor constructor = MEASUREMENT_CLASS.getConstructor(typeList);
      Object[] argList = { application };
      result = omnitureMeasurement = constructor.newInstance(argList);
    }
    return result;
  }

  private Object populateDefaultFields(Object s) throws SecurityException,
      NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

    // Specify the Report Suite ID(s) to track here
    Field account = MEASUREMENT_CLASS.getField("account");
    account.set(s, "nprnprandroiddev");

    Field currencyCode = MEASUREMENT_CLASS.getField("currencyCode");
    currencyCode.set(s, "USD");

    Field linkLeaveQueryString =
        MEASUREMENT_CLASS.getField("linkLeaveQueryString");
    linkLeaveQueryString.setBoolean(s, true);

    /*
     * WARNING: Changing any of the below variables will cause drastic changes
     * to how your visitor data is collected. Changes should only be made when
     * instructed to do so by your account manager.
     */
    Field dc = MEASUREMENT_CLASS.getField("dc");
    dc.set(s, "122");

    /* Configure debugging here */
    Field debugTracking = MEASUREMENT_CLASS.getField("debugTracking");
    debugTracking.setBoolean(s, false);

    return s;
  }

  public void trackPage(ActivityMeasurement activityMeasurement) {
    // Any Omniture failures (including not being present) will result in all
    // future calls to be no-ops.
    if (!OMNITURE_PRESENT) {
      return;
    }

    final ActivityMeasurement internalMapping = activityMeasurement;
    
    // Ensures that only one invocation will happen at a time, since Omniture is
    // not thread-safe, and should not put much burden on the user experience.
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        invokeTrack(internalMapping);
      }
    });
  }

  private void invokeTrack(ActivityMeasurement internalMapping) {
    try {
      Object s = createMeasurement();
      populateDefaultFields(s);

      for (Entry<String, String> value : internalMapping.values.entrySet()) {
        Field f = MEASUREMENT_CLASS.getField(value.getKey());
        f.set(s, value.getValue());
      }
      synchronized (this) {
        /*
         * NOTE: App Measurement for Android is not thread-safe. You can use it
         * in a multi-threaded application, but you cannot access it from
         * multiple threads.
         */
        Method track = MEASUREMENT_CLASS.getMethod("track", new Class[] {});
        track.invoke(s);
      }

    } catch (SecurityException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (IllegalArgumentException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (IllegalAccessException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (InstantiationException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (NoSuchMethodException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (InvocationTargetException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (NoSuchFieldException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    }
  }

  private void invokeTrackLink(LinkEvent event) {
    try {
      Object s = createMeasurement();

      for (Entry<String, String> value : event.values.entrySet()) {
        Field f = MEASUREMENT_CLASS.getField(value.getKey());
        f.set(s, value.getValue());
      }
      synchronized (this) {
        /*
         * NOTE: App Measurement for Android is not thread-safe. You can use it
         * in a multi-threaded application, but you cannot access it from
         * multiple threads.
         */
        Method trackLink =
            MEASUREMENT_CLASS.getMethod("trackLink", new Class[] {String.class,
                String.class, String.class});
        trackLink.invoke(s, null, "o", event.linkName);
      }

    } catch (SecurityException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (IllegalArgumentException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (IllegalAccessException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (InstantiationException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (NoSuchMethodException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (InvocationTargetException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    } catch (NoSuchFieldException e) {
      Log.e(LOG_TAG, "error obtaining tracker", e);
    }
  }
  
  public void trackLink(LinkEvent event) {
    if (!OMNITURE_PRESENT) {
      return;
    }

    final LinkEvent e = event;
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        invokeTrackLink(e);
      }
    });
  }

  public static class ActivityMeasurement {
    public ActivityMeasurement(String pageName, String channel, String orgId) {
      this.pageName = pageName;
      this.channel = channel;
      this.orgId = orgId;

      values = new HashMap<String, String>();
      values.put(pageNameVars[0], pageName);
      values.put(pageNameVars[1], pageName);
      values.put(pageNameVars[2], pageName);

      values.put(contentTypeVars[0], contentType);
      values.put(contentTypeVars[1], contentType);

      values.put(versionNumberVars[0], versionNumber);
      values.put(versionNumberVars[1], versionNumber);

      values.put(channelVars[0], channel);

      values.put(eventsVars[0], events);

      values.put(orgIdVars[0], orgId);
      values.put(orgIdVars[1], orgId);
    }

    public ActivityMeasurement(String pageName, String channel) {
      // For all other pages, this should be set to "1" (which represents NPR).
      this(pageName, channel, "1");
    }

    protected Map<String, String> values;
    // pageName

    // Title of the current page, according to the table at
    // http://spreadsheets.google.com/pub?key=tvTr3NSj4Cb6kgG5kUQ7DQw&output=html

    // prop3, eVar3
    // Title of current page (same as pageName)
    @SuppressWarnings("unused")
    private String pageName;
    private static String[] pageNameVars =
        new String[] {"pageName", "prop3", "eVar3"};

    // prop5, eVar5
    // Content Type - Always set to constant "Android"
    private final String contentType = "Android";
    private static String[] contentTypeVars = new String[] {"prop5", "eVar6"};

    // prop44, eVar44
    // Version number of Android application
    private final String versionNumber = "";
    private static String[] versionNumberVars =
        new String[] {"prop44", "eVar44"};

    // channel
    // Section within application. Examples: Home, News, Stations, Search,
    // Playlist
    @SuppressWarnings("unused")
    private String channel = "";
    private static String[] channelVars = new String[] {"channel"};

    // events
    // For all standard page views, should contain "event2" as value.
    private final String events = "event2";
    protected static String[] eventsVars = new String[] {"events"};

    // prop20, eVar20
    protected String orgId;
    protected static String[] orgIdVars = new String[] {"prop20", "eVar20"};
  }

  public static class StoryDetailsMeasurement extends ActivityMeasurement {
    public StoryDetailsMeasurement(String pageName, String channel,
        String orgId, String topicId, String storyId) {
      super(pageName, channel, orgId);
      this.storyId = storyId;
      this.topicId = topicId;

      values.put(storyIdVars[0], storyId);
      values.put(storyIdVars[1], storyId);

      values.put(topicIdVars[0], topicId);
      values.put(topicIdVars[1], topicId);
    }

    // prop4, eVar4
    // Story ID -- leave blank for all but news article detail pages. When on an
    // article detail page, put the story id that you have from the API call.
    @SuppressWarnings("unused")
    private String storyId;
    private static String[] storyIdVars = new String[] {"prop4", "eVar4"};

    // prop7, eVar7
    // On a story page, this should contain the primary topic ID of the story.
    @SuppressWarnings("unused")
    private String topicId;
    private static String[] topicIdVars = new String[] {"prop7", "eVar7"};

    // prop20, eVar20
    // When on a story page, this should contain the orgId for the story.
  }

  public static class StoryListMeasurement extends ActivityMeasurement {
    public StoryListMeasurement(String pageName, String channel, String topicId) {
      super(pageName, channel);
      this.topicId = topicId;

      values.put(topicIdVars[0], topicId);
      values.put(topicIdVars[1], topicId);
    }

    // prop7, eVar7
    // When on a list of stories within a topic, this should contain the
    // topicId.
    @SuppressWarnings("unused")
    private String topicId;
    private static String[] topicIdVars = new String[] {"prop7", "eVar7"};
  }

  public static class StationDetailsMeasurement extends ActivityMeasurement {

    public StationDetailsMeasurement(String pageName, String channel,
        String orgId) {
      super(pageName, channel, orgId);
    }
    // prop20, eVar20
    // When on a station-specific page, this should contain the orgId of the
    // station.

    // prop21, eVar21
    // When on program-specific pages and story pages, the ID of the program.
  }

  public static class SearchResultsMeasurement extends ActivityMeasurement {

    public SearchResultsMeasurement(String pageName, String channel, String query,
        int resultsCount) {
      super(pageName, channel);
      this.query = query;
      this.resultsCount = resultsCount;
      values.put(queryVars[0], query);
      values.put(queryVars[1], query);
      
      values.put(resultsCountVars[0], "" + resultsCount);
      values.put(resultsCountVars[1], "" + resultsCount);

      // a) event1 should be passed along with the standard track() call (in
      // addition to event2)
      String events = values.get(eventsVars[0]);
      events += ",event1";
      values.put(eventsVars[0], events);
    }
    // When presenting the Search Results screen:
    //
    // b) the search query string used, should be set in prop1 and eVar1
    @SuppressWarnings("unused")
    private String query;
    private static String[] queryVars = new String[] {"prop1", "eVar1"};
    // c) the number of results returned should be set in prop2 and eVar2
    @SuppressWarnings("unused")
    private int resultsCount;
    private static String[] resultsCountVars = new String[] {"prop2", "eVar2"};
    // d) the search date range should be set in prop12 and eVar12 (format TBD)

  }
  
  public static class StationListMeasurement extends ActivityMeasurement {

    public StationListMeasurement(String pageName, String channel,
        String query) {
      super(pageName, channel);
      this.query = query;
      values.put(queryVars[0], query);
      values.put(queryVars[1], query);

      // event27 should be fired (in addition to event2).
      String events = values.get(eventsVars[0]);
      events += ",event27";
      values.put(eventsVars[0], events);
    }
    // When presenting the list of stations, the query used to select the
    // station (whether zip code, call letters, or GPS coordinates) should be
    // passed to prop43/eVar43
    @SuppressWarnings("unused")
    private String query;
    private static String[] queryVars = new String[] {"prop43", "eVar43"};
  }

  
  public static class LinkEvent {
    public LinkEvent(String linkName, String event) {
      this.linkName = linkName;
      values = new HashMap<String, String>();

      values.put(linkNameVars[0], linkName);
      
      values.put(eventsVars[0], event);
    }

    protected final String linkName;
    private static String[] linkNameVars = new String[] {"linkName"};
    protected Map<String, String> values;
    protected static String[] eventsVars = new String[] {"events"};

  }
  
  public static class PlayNowEvent extends LinkEvent {
    protected PlayNowEvent(String storyId, String mediaTitle, String mediaId,
        String event) {
      super(storyId + "-" + mediaTitle, event);
      
      values.put(linkNameVars[0], storyId + "-" + mediaTitle);

      values.put(mediaVars[0], storyId + "-" + mediaId);
      values.put(mediaVars[1], storyId + "-" + mediaId);

      values.put(randVars[0], "1");
      values.put(randVars[1], "1");
    }
    
    public PlayNowEvent(String storyId, String mediaTitle, String mediaId) {
      this(storyId, mediaTitle, mediaId, "event6");
    }
    // Link name and eVar18= "storyID-mediaTitle"
    private static String[] linkNameVars = new String[] {"eVar18"};

    // prop36/eVar36 set to "storyID-mediaID"
    private static String[] mediaVars = new String[] {"prop36", "eVar36"};

    // prop25/eVar25 set to "1"
    private static String[] randVars = new String[] {"prop25", "eVar25"};
  }
  
  public static class PlayLaterEvent extends PlayNowEvent {

    public PlayLaterEvent(String storyId, String mediaTitle, String mediaId) {
      super(storyId, mediaTitle, mediaId, "event23");
    }
  }

  public static class PodcastNowEvent extends LinkEvent {
    protected PodcastNowEvent(String title, String episode, String url,
        String event) {
      super(String.format("Podcast: %s: %s", title, episode), event);
      
      values.put(linkNameVars[0], linkName);

      values.put(podcastVars[0], "Podcast");
      values.put(podcastVars[1], "Podcast");
      
      values.put(uriVars[0], url);
      values.put(uriVars[1], url);

      values.put(randVars[0], "10");
      values.put(randVars[1], "10");
    }
    
    public PodcastNowEvent(String title, String episode, String url) {
      this(title, episode, url, "event6");
    }

    // Link name and eVar18 = "Podcast: Podcast Title: Episode Title"
    private static String[] linkNameVars = new String[] {"eVar18"};

    // prop36/eVar36 set to "Podcast"
    private static String[] podcastVars = new String[] {"prop36", "eVar36"};

    // prop27/eVar27 set to Podcast URI
    private static String[] uriVars = new String[] {"prop27", "eVar27"};
    
    //  prop25/eVar25 set to "10"
    private static String[] randVars = new String[] {"prop25", "eVar25"};
  }
  
  public static class PodcastLaterEvent extends PlayNowEvent {

    public PodcastLaterEvent(String storyId, String mediaTitle, String mediaId) {
      super(storyId, mediaTitle, mediaId, "event25");
    }
  }
  
  public static class StationSearchEvent extends LinkEvent {

    public StationSearchEvent() {
      super("Select Station Dialog", "event15");
    }
    // Dialog for GPS/Zip Code/Call Letters search activated
    // event15
    // Link name = "Select Station Dialog"
    // No additional props/eVars
  }

  public static class StationStreamEvent extends LinkEvent {

    public StationStreamEvent(String stationName, String streamName) {
      super(String.format("Station Stream: %s: %s", stationName, streamName),
          "event26");
      values.put(linkNameVars[0], linkName);

      values.put(streamVars[0], "Station Stream");
      values.put(streamVars[1], "Station Stream");

      values.put(randVars[0], "20");
      values.put(randVars[1], "20");
    }

    private static String[] linkNameVars = new String[] {"eVar18"};
    // Station stream launched
    // event26
    // Link name and eVar18 = "Station Stream: Station Name: Stream Name"

    // prop36/eVar36 set to "Station Stream"
    private static String[] streamVars = new String[] {"prop36", "eVar36"};

    // prop25/eVar25 set to "20"
    private static String[] randVars = new String[] {"prop25", "eVar25"};

  }
}

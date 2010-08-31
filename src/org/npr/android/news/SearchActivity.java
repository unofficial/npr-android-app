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

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import org.npr.android.util.Tracker;
import org.npr.android.util.Tracker.ActivityMeasurement;
import org.npr.api.ApiConstants;
import org.npr.api.Story;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchActivity extends PlayerActivity implements
    OnClickListener {
  private EditText searchText;
  private Calendar startDate;
  private Calendar endDate;
  private Button startDateButton;
  private Button endDateButton;
  private SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewGroup container = (ViewGroup) findViewById(R.id.Content);
    ViewGroup.inflate(this, R.layout.search, container);

    searchText = (EditText) findViewById(R.id.EditText01);

    Date start = new Date();
    endDate = new GregorianCalendar();
    startDate = (Calendar) endDate.clone();
    startDate.add(Calendar.DATE, -7);

    Button searchButton = (Button) findViewById(R.id.SearchButton);
    startDateButton = (Button) findViewById(R.id.StartDateButton);
    endDateButton = (Button) findViewById(R.id.EndDateButton);
    startDateButton.setText(dateFormat.format(startDate.getTime()));
    endDateButton.setText(dateFormat.format(endDate.getTime()));
    searchButton.setOnClickListener(this);
    startDateButton.setOnClickListener(this);
    endDateButton.setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.SearchButton:
        String text = searchText.getText().toString();
        String start = getDate(startDate);
        String end = getDate(endDate);
        Map<String, String> params = new HashMap<String, String>();
        List<Story> storyList = new ArrayList<Story>();
        params.put("searchTerm", text);
        params.put("startDate", start);
        params.put("endDate", end);

        params.put("fields", ApiConstants.STORY_FIELDS);
        params.put("sort", "assigned");
        String url =
            ApiConstants.instance().createUrl(ApiConstants.STORY_PATH, params);

        String description =
            getString(R.string.msg_search_term) + Tracker.PAGE_NAME_SEPARATOR
                + text;
        Intent i = new Intent(this, SearchResultsActivity.class);
        i.putExtra(Constants.EXTRA_QUERY_URL, url);
        i.putExtra(Constants.EXTRA_DESCRIPTION, description);
        i.putExtra(Constants.EXTRA_QUERY_TERM, text);
        i.putExtra(Constants.EXTRA_SIZE, 10);

        startActivityWithoutAnimation(i);
        break;
      case R.id.StartDateButton:
        OnDateSetListener callback = new OnDateSetListener() {
          @Override
          public void onDateSet(DatePicker view, int year, int monthOfYear,
              int dayOfMonth) {
            startDate.set(Calendar.YEAR, year);
            startDate.set(Calendar.MONTH, monthOfYear);
            startDate.set(Calendar.DATE, dayOfMonth);
            startDateButton.setText(dateFormat.format(startDate.getTime()));
          }
        };
        new DatePickerDialog(this, callback, startDate.get(Calendar.YEAR),
            startDate.get(Calendar.MONTH), startDate.get(Calendar.DATE)).show();
        break;
      case R.id.EndDateButton:
        OnDateSetListener callbackEnd = new OnDateSetListener() {
          @Override
          public void onDateSet(DatePicker view, int year, int monthOfYear,
              int dayOfMonth) {
            endDate.set(Calendar.YEAR, year);
            endDate.set(Calendar.MONTH, monthOfYear);
            endDate.set(Calendar.DATE, dayOfMonth);
            endDateButton.setText(dateFormat.format(endDate.getTime()));
          }
        };
        new DatePickerDialog(this, callbackEnd, endDate.get(Calendar.YEAR),
            endDate.get(Calendar.MONTH), endDate.get(Calendar.DATE)).show();
        break;
    }
  }

  private String getDate(Calendar cal) {
    StringBuilder sb = new StringBuilder();
    sb.append(cal.get(Calendar.YEAR)).append("-");
    // Months are 0-based in Java, 1-based in NPR api.
    sb.append(cal.get(Calendar.MONTH) + 1).append("-");
    sb.append(cal.get(Calendar.DATE));
    return sb.toString();
  }

  @Override
  public CharSequence getMainTitle() {
    return getString(R.string.msg_main_subactivity_search);
  }

  @Override
  public void trackNow() {
    StringBuilder pageName = new StringBuilder("Search").append(Tracker.PAGE_NAME_SEPARATOR);
    pageName.append("Search Form");
    Tracker.instance(getApplication()).trackPage(
        new ActivityMeasurement(pageName.toString(), "Search"));
  }
}

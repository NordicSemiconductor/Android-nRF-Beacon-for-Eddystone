// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sample.libproximitybeacon;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;

public class ProximityBeaconImpl implements ProximityBeacon {
  private static final String ENDPOINT = "https://proximitybeacon.googleapis.com/v1beta1/";
  public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

  private static final int GET = 0;
  private static final int PUT = 1;
  private static final int POST = 2;
  private static final int DELETE = 3;
  private final Activity mActivity;
  private Account account;
  private final OkHttpClient httpClient;
  private Project mProject;
  private String mToken;
  public static final String ACCESS_TOKEN_INFO = "ACCESS_TOKEN_INFO";
  public static final String ACCESS_TOKEN = "ACCESS_TOKEN";

  public ProximityBeaconImpl(Activity mActivity, Account account) {
    this.mActivity = mActivity;
    this.account = account;
    this.httpClient = new OkHttpClient();
  }

  public ProximityBeaconImpl(Activity mActivity, Project project) {
    this.mActivity = mActivity;
    this.httpClient = new OkHttpClient();
    this.mProject = project;
  }

  private void getToken(){
    mToken = mActivity.getSharedPreferences(ACCESS_TOKEN_INFO, Context.MODE_PRIVATE).getString(ACCESS_TOKEN, "");
  }

  @Override
  public void getForObserved(Callback callback, JSONObject requestBody, String apiKey) {
    // The authorization step here isn't strictly necessary. The API key is enough.
    new AuthTask("beaconinfo:getforobserved?key=" + apiKey, POST, requestBody.toString(), callback).execute();
  }

  @Override
  public void activateBeacon(Callback callback, String beaconName) {
    new AuthTask(beaconName + ":activate", POST, "", callback).execute();
  }

  @Override
  public void deactivateBeacon(Callback callback, String beaconName) {
    new AuthTask(beaconName + ":deactivate", POST, "", callback).execute();
  }

  @Override
  public void decommissionBeacon(Callback callback, String beaconName) {
    new AuthTask(beaconName + ":decommission", POST, "", callback).execute();
  }

  @Override
  public void getBeacon(Callback callback, String beaconName) {
    new AuthTask(beaconName, callback).execute();
  }

  @Override
  public void listBeacons(Callback callback, String query) {
    new AuthTask("beacons" + "?q=" + query, callback).execute();
  }

  @Override
  public void registerBeacon(Callback callback, JSONObject requestBody) {
    new AuthTask("beacons:register?projectId=" + mProject.getProjectId(), POST, requestBody.toString(), callback).execute();
  }

  @Override
  public void updateBeacon(Callback callback, String beaconName, JSONObject requestBody) {
    new AuthTask(beaconName, PUT, requestBody.toString(), callback).execute();
  }

  @Override
  public void batchDeleteAttachments(Callback callback, String beaconName) {
    new AuthTask(beaconName + "/attachments:batchDelete?projectId=" + mProject.getProjectId(), POST, "", callback).execute();
  }

  @Override
  public void createAttachment(Callback callback, String beaconName, JSONObject requestBody) {
    new AuthTask(beaconName + "/attachments?projectId=" + mProject.getProjectId(), POST, requestBody.toString(), callback).execute();
  }

  @Override
  public void deleteAttachment(Callback callback, String attachmentName) {
    new AuthTask(attachmentName, DELETE, "", callback).execute();
  }

  @Override
  public void listAttachments(Callback callback, String beaconName) {
    new AuthTask(beaconName + "/attachments?namespacedType=*/*", callback).execute();
  }

  @Override
  public void listDiagnostics(Callback callback, String beaconName) {
    new AuthTask(beaconName + "/diagnostics", callback).execute();
  }

  @Override
  public void listNamespaces(Callback callback) {
    new AuthTask("namespaces?projectId=" + mProject.getProjectId(), callback).execute();
  }

  @Override
  public void getEphemeralIdRegistrationParams(Callback callback) {
    new AuthTask("eidparams", callback).execute();
  }

  public void refreshProject(final Project project) {
    mProject = project;
  }

  private class AuthTask extends AsyncTask<Void, Void, Void> {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    private final String urlPart;
    private final int method;
    private final String json;
    private final Callback callback;

    AuthTask(String urlPart, Callback callback) {
      this(urlPart, GET, "", callback);
    }

    AuthTask(String urlPart, int method, String json, Callback callback) {
      this.urlPart = urlPart;
      this.method = method;
      this.json = json;
      this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
      //final String token = GoogleAuthUtil.getToken(mActivity, account, SCOPE);
      getToken();
      Request.Builder requestBuilder = new Request.Builder()
              .header(AUTHORIZATION, BEARER + mToken)
              .url(ENDPOINT + urlPart);
      switch (method) {
        case PUT:
          requestBuilder.put(RequestBody.create(MEDIA_TYPE_JSON, json));
          break;
        case POST:
          requestBuilder.post(RequestBody.create(MEDIA_TYPE_JSON, json));
          break;
        case DELETE:
          requestBuilder.delete(RequestBody.create(MEDIA_TYPE_JSON, json));
          break;
        default: break;
      }
      Request request = requestBuilder.build();
      httpClient.newCall(request).enqueue(new HttpCallback(callback));
      return null;
    }

  }
}

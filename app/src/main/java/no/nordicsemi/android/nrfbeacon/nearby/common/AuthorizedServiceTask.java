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

package no.nordicsemi.android.nrfbeacon.nearby.common;

import android.accounts.Account;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;

import no.nordicsemi.android.nrfbeacon.nearby.util.Utils;

/**
 * NOP async task that allows us to check if a new user has authorized the app
 * to access their account.
 */
public class AuthorizedServiceTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = AuthorizedServiceTask.class.getSimpleName();
    //static final String authScope = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_PRX_API = 1003;
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER = 1004;
    private static final String AUTH_PROXIMITY_API = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";
    private static final String AUTH_SCOPE_URL_SHORTENER = "oauth2:https://www.googleapis.com/auth/urlshortener";

    private final Activity activity;
    private final Account account;
    private String authScope;
    private int requestType;

    public AuthorizedServiceTask(Activity activity, Account accountName, final String authScope, final int requestType) {
        this.activity = activity;
        this.account = accountName;
        this.authScope = authScope;
        this.requestType = requestType;
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.i(TAG, "checking authorization for " + account.name);
        try {
            String token = GoogleAuthUtil.getToken(activity, account, authScope);
                switch (requestType){
                    case Utils.GET_TOKEN:
                        break;
                    case Utils.CLEAR_TOKEN:
                        GoogleAuthUtil.clearToken(activity, token);
                        break;
                }

        } catch (UserRecoverableAuthException e) {
            // GooglePlayServices.apk is either old, disabled, or not present
            // so we need to show the user some UI in the activity to recover.
            handleAuthException(activity, e);
        } catch (GoogleAuthException e) {
            // Some other type of unrecoverable exception has occurred.
            // Report and log the error as appropriate for your app.
            Log.w(TAG, "GoogleAuthException: " + e.getMessage());
        } catch (IOException e) {
            // The fetchToken() method handles Google-specific exceptions,
            // so this indicates something went wrong at a higher level.
            // TIP: Check for network connectivity before starting the AsyncTask.
        }
        return null;
    }

    private void handleAuthException(final Activity activity, final Exception e) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException) e).getConnectionStatusCode();
                    Dialog dialog;
                    if(authScope.equals(AUTH_PROXIMITY_API)) {
                        dialog = GooglePlayServicesUtil.getErrorDialog(
                                statusCode, activity, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_PRX_API);
                    } else {
                        dialog = GooglePlayServicesUtil.getErrorDialog(
                                statusCode, activity, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER);
                    }
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    if(authScope.equals(AUTH_PROXIMITY_API)) {
                        activity.startActivityForResult(
                                intent, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_PRX_API);
                    } else {
                        activity.startActivityForResult(
                                intent, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER);
                    }
                }
            }
        });
    }

}

package no.nordicsemi.android.nrfbeacon.nearby.util;

import android.accounts.Account;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import no.nordicsemi.android.nrfbeacon.nearby.R;

/**
 * Created by rora on 07.04.2016.
 */
public class AuthTaskUrlShortener extends AsyncTask <Void, Void, Void> {
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = "BEACON";
    private final OkHttpClient mOkHttpClient;
    private final Callback mCallBack;
    private final String longUrl;
    private final String BASE_URL = "https://www.googleapis.com/urlshortener/v1/url";//?key=AIzaSyDlSUOly5_hQkzeyxOr5Ff5c8AuNWaUcZM";
    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/urlshortener";
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER = 1004;
    private static final String AUTH_PROXIMITY_API = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";
    private static final String AUTH_SCOPE_URL_SHORTENER = "oauth2:https://www.googleapis.com/auth/urlshortener";
    private Activity mActivity;
    private Account mAccount;

    public AuthTaskUrlShortener(final Callback mCallBack, final String longUrl, Activity context, Account account){
        this.mOkHttpClient = new OkHttpClient();
        this.mCallBack = mCallBack;
        this.longUrl = longUrl;
        this.mActivity = context;
        this.mAccount = account;
    }

    @Override
    protected Void doInBackground(Void... params) {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("longUrl", longUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String token;
        try {
            token = GoogleAuthUtil.getToken(mActivity, mAccount, SCOPE);
            Request.Builder requestBuilder = new Request.Builder()
                    .header(AUTHORIZATION, BEARER + token)
                    .url(BASE_URL)
                    .post(RequestBody.create(MEDIA_TYPE_JSON, jsonObject.toString()));

            Request request = requestBuilder.build();
            mOkHttpClient.newCall(request).enqueue(new HttpCallback(mCallBack));
        } catch (UserRecoverableAuthException e) {
            // GooglePlayServices.apk is either old, disabled, or not present
            // so we need to show the user some UI in the activity to recover.
            handleAuthException(mActivity, e);
            Log.e(TAG, "UserRecoverableAuthException", e);
        } catch (GoogleAuthException e) {
            // Some other type of unrecoverable exception has occurred.
            // Report and log the error as appropriate for your app.
            Log.e(TAG, "GoogleAuthException", e);
        } catch (IOException e) {
            // The fetchToken() method handles Google-specific exceptions,
            // so this indicates something went wrong at a higher level.
            // TIP: Check for network connectivity before starting the AsyncTask.
            Log.e(TAG, "IOException", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
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
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                            statusCode, activity, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    activity.startActivityForResult(
                            intent, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER);

                }
            }
        });
    }
}

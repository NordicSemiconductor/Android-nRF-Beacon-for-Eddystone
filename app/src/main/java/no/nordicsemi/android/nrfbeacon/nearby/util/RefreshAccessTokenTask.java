package no.nordicsemi.android.nrfbeacon.nearby.util;

import android.os.AsyncTask;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

/**
 * Created by rora on 07.04.2016.
 */
public class RefreshAccessTokenTask extends AsyncTask <Void, Void, Void> {
    private final OkHttpClient mOkHttpClient;
    private final Callback mCallBack;
    private final String mRefreshToken;
    private String mClientId;
    private String mCode;

    public RefreshAccessTokenTask(final Callback mCallBack, final String mRefreshToken, String clientId, String code){
        this.mOkHttpClient = new OkHttpClient();
        this.mCallBack = mCallBack;
        this.mRefreshToken = mRefreshToken;
        this.mClientId = clientId;
        this.mCode = code;
    }

    @Override
    protected Void doInBackground(Void... params) {

        RequestBody requestBody = new FormEncodingBuilder()
                .add("refresh_token", mRefreshToken)
                .add("client_id", mClientId)
                .add("client_secret", "ADD_YOUR_CLIENT_SECRET")
                .add("grant_type", "refresh_token")
                .build();
        final Request request = new Request.Builder()
                .url(Utils.ACCESS_TOKEN_URL)
                .post(requestBody)
                .build();
        mOkHttpClient.newCall(request).enqueue(new HttpCallback(mCallBack));
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}

package no.nordicsemi.android.nrfbeacon.nearby.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import no.nordicsemi.android.nrfbeacon.nearby.R;

/**
 * Created by rora on 14.07.2016.
 */
public class ListProjectsTask extends AsyncTask<Void, Void, Void> {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    private final String url;
    private final Callback callback;
    private final Context mContext;
    private final OkHttpClient httpClient;
    private final String mToken;
    private ProgressDialog mProgressDialog;

    public ListProjectsTask(Context context, String url, Callback callback, String token) {
        this.mContext = context;
        this.url = url;
        this.callback = callback;
        httpClient = new OkHttpClient();
        this.mToken = token;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        showProgressDialog();
    }

    @Override
    protected Void doInBackground(Void... params) {
        final String token = mToken;

        Request.Builder requestBuilder = new Request.Builder()
                .header(AUTHORIZATION, BEARER + token)
                .url(url);

        Request request = requestBuilder.build();
        httpClient.newCall(request).enqueue(new HttpCallback(callback));
        return null;
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        hideProgressDialog();
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(mContext.getString(R.string.please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setTitle(mContext.getString(R.string.title_listing_projects));
            mProgressDialog.setMessage(mContext.getString(R.string.message_listing_projects));
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
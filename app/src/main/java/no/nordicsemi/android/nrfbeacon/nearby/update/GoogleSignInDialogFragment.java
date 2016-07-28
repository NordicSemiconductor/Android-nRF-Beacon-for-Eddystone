/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrfbeacon.nearby.update;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.sample.libproximitybeacon.Project;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.common.ProjectsAdapter;
import no.nordicsemi.android.nrfbeacon.nearby.util.RequestAccessTokenTask;
import no.nordicsemi.android.nrfbeacon.nearby.util.Utils;

/**
 * Created by rora on 16.03.2016.
 */
public class GoogleSignInDialogFragment extends DialogFragment{

    private SignInButton mSignInButton;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInOptions mGso;

    private ArrayList<Project> mProjectList;
    private ProjectsAdapter mAdapterProjectsList;
    private ProgressDialog mProgressDialog;
    private String mServerAuthCode;

    private String message;

    public interface OnSignInCompletion{
        void onSignInCompleteHandler(final String serverAuthCode);
    }

    public static GoogleSignInDialogFragment newInstance(final String message){
        GoogleSignInDialogFragment fragment = new GoogleSignInDialogFragment();
        Bundle args = new Bundle();
        args.putString(Utils.MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    public GoogleSignInDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            message = getArguments().getString(Utils.MESSAGE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.google_sign_in));

        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_google_sign_in, null);
        final TextView textView = (TextView) view.findViewById(R.id.sign_in_rationale);
        textView.setText(message);
        final AlertDialog alertDialog = alertDialogBuilder.setView(view).show();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(true);

        mSignInButton = (SignInButton) view.findViewById(R.id.sign_in_button);
        mSignInButton.setSize(SignInButton.SIZE_STANDARD);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });



        startGoogleSignin();

        return alertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, Utils.SIGN_IN);
    }

    private void startGoogleSignin() {
        Scope scope = new Scope("https://www.googleapis.com/auth/cloud-platform");
        Scope scope1 = new Scope("https://www.googleapis.com/auth/userlocation.beacon.registry");
        mGso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(scope, scope1)
                .requestServerAuthCode(getString(R.string.server_client_id))
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                //.enableAutoManage(getActivity() /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGso)
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Utils.SIGN_IN:
                showProgressDialog();
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
                break;
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(Utils.TAG, "handleSignInResult:" + result.isSuccess());
        hideProgressDialog();
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            Log.d(Utils.TAG, "handleSignInResult:" + acct.getEmail() + " " + acct.getDisplayName());
            mServerAuthCode = acct.getServerAuthCode();
            new RequestAccessTokenTask(getActivity(), mRequestAccessTokenCallback, "https://www.googleapis.com/oauth2/v4/token", getString(R.string.server_client_id), acct.getServerAuthCode()).execute();
        } else {
            Toast.makeText(getActivity(), getString(R.string.google_sign_in_denied), Toast.LENGTH_LONG).show();
        }
    }

    Callback mRequestAccessTokenCallback = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {

        }

        @Override
        public void onResponse(Response response) throws IOException {

            try {
                String body = response.body().string();
                final JSONObject jsonResponse = new JSONObject(body);
                Log.v(Utils.TAG, jsonResponse.toString());
                if (!response.isSuccessful()) {
                } else {
                    final String access_token = jsonResponse.getString("access_token");
                    if(jsonResponse.has("refresh_token")) {
                        final String refresh_token = jsonResponse.getString("refresh_token");
                        saveAccessToken(access_token, refresh_token);
                    } else {
                        saveNewAccessToken(access_token);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void saveAccessToken(final String accessToken, final String refresh_token){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Utils.ACCESS_TOKEN_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Utils.ACCESS_TOKEN, accessToken);
        editor.putString(Utils.REFRESH_TOKEN, refresh_token);
        editor.commit();
        hideProgressDialog();
        dismiss();
        ((OnSignInCompletion) getParentFragment()).onSignInCompleteHandler(mServerAuthCode);
    }

    private void saveNewAccessToken(final String accessToken){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Utils.ACCESS_TOKEN_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Utils.ACCESS_TOKEN, accessToken);
        editor.commit();
        dismiss();
        ((OnSignInCompletion) getParentFragment()).onSignInCompleteHandler(mServerAuthCode);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.please_wait));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}

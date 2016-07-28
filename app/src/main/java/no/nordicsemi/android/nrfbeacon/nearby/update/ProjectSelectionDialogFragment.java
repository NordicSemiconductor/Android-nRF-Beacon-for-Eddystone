package no.nordicsemi.android.nrfbeacon.nearby.update;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.sample.libproximitybeacon.Project;
import com.google.sample.libproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.common.ProjectsAdapter;
import no.nordicsemi.android.nrfbeacon.nearby.util.HttpCallback;
import no.nordicsemi.android.nrfbeacon.nearby.util.ListProjectsTask;
import no.nordicsemi.android.nrfbeacon.nearby.util.RefreshAccessTokenTask;
import no.nordicsemi.android.nrfbeacon.nearby.util.Utils;

/**
 * Created by rora on 18.07.2016.
 */
public class ProjectSelectionDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener{

    private TextView mProjectsSummary;
    private ListView mProjectsListView;
    private ArrayList<Project> mProjectList;
    private ProjectsAdapter mAdapterProjectsList;
    private ProgressDialog mProgressDialog;
    private Button mOkButton;
    private Button mRefreshButton;
    private String mServerAuthCode;
    private Context mContext;

    public interface OnProjectSelectionListener{
        void handleProjectSelection();
        void cancelProjectSelection();
        String getServerAuthCode();
        void displayGoogleSignInDialog();
        void displayErrorDialog(final String errorCode, final String message, final String status);
    }

    public static ProjectSelectionDialogFragment newInstance() {
        ProjectSelectionDialogFragment fragment = new ProjectSelectionDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.project_selection));
        final View view =  LayoutInflater.from(getActivity()).inflate(R.layout.fragment_project_selection, null);

        final AlertDialog alertDialog = alertDialogBuilder.setView(view).setPositiveButton(getString(R.string.open_console), null).setNeutralButton(getString(R.string.refresh), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        mProjectsSummary = (TextView) view.findViewById(R.id.projects_message);
        mProjectsListView = (ListView) view.findViewById(R.id.projects_list);

        mProjectList = new ArrayList<>();
        mAdapterProjectsList = new ProjectsAdapter(getActivity(), mProjectList);
        mProjectsListView.setAdapter(mAdapterProjectsList);
        mProjectsListView.setOnItemClickListener(this);

        mProjectsSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        final String accessToken = getActivity().getSharedPreferences(Utils.ACCESS_TOKEN_INFO, Context.MODE_PRIVATE).getString(Utils.ACCESS_TOKEN, "");
        listProjectsFromConsole(accessToken);

        mOkButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mOkButton.setVisibility(View.GONE);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.DEVELOPER_CONSOLE_URL));
                getActivity().startActivity(browserIntent);
            }
        });

        mRefreshButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String accessToken = mContext.getSharedPreferences(Utils.ACCESS_TOKEN_INFO, Context.MODE_PRIVATE).getString(Utils.ACCESS_TOKEN, "");
                listProjectsFromConsole(accessToken);
            }
        });

        alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    ((OnProjectSelectionListener)getParentFragment()).cancelProjectSelection();
                }

                return false;
            }
        });

        return alertDialog;
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Save project information to be used by beacons fragment for Nearby Search
        Project project = mAdapterProjectsList.getItem(position);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Utils.PROJECT_INFO, Context.MODE_PRIVATE);
        final String projectName = project.getName();
        final String projectID = project.getProjectId();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Utils.PROJECT_NAME, projectName);
        editor.putString(Utils.PROJECT_ID, projectID);
        editor.commit();
        ProximityBeaconImpl proximityBeacon = new ProximityBeaconImpl(getActivity(), mAdapterProjectsList.getItem(position));
        proximityBeacon.listNamespaces(mListNamespaces);
    }

    Callback mListNamespaces = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {

        }

        @Override
        public void onResponse(Response response) throws IOException {
            try {
                Log.v(Utils.TAG, response.toString());
                String body = response.body().string();
                final JSONObject jsonResponse = new JSONObject(body);
                if (!response.isSuccessful()) {
                    final JSONObject jsonError = jsonResponse.getJSONObject("error");
                    final int errorCode = jsonError.getInt("code");
                    final String message = jsonError.getString("message");
                    ProximityApiErrorDialogFragment errorDialogFragment;
                    switch (errorCode) {
                        case Utils.ERROR_UNAUTHORIZED:
                            mServerAuthCode = ((OnProjectSelectionListener)getParentFragment()).getServerAuthCode();
                            if(mServerAuthCode != null) {
                                final String refreshToken = getActivity().getSharedPreferences(Utils.ACCESS_TOKEN_INFO, Context.MODE_PRIVATE).getString(Utils.REFRESH_TOKEN, "");
                                if(!refreshToken.isEmpty())
                                    new RefreshAccessTokenTask(mRefreshAccessTokenCallback, refreshToken, getString(R.string.server_client_id), mServerAuthCode).execute();
                            } else {
                                dismiss();
                                ((OnProjectSelectionListener)getParentFragment()).displayGoogleSignInDialog();
                            }
                            return;
                        default:
                            dismiss();
                            String status = "";
                            if(jsonError.has("status")) {
                                status = jsonError.getString("status");
                            }
                            ((OnProjectSelectionListener)getParentFragment()).displayErrorDialog(String.valueOf(errorCode), message, status);

                            return;
                    }
                } else {
                    final JSONArray jsonNamespaces = jsonResponse.getJSONArray("namespaces");
                    final JSONObject jsonProject = jsonNamespaces.getJSONObject(0);
                    String namespace = jsonProject.getString("namespaceName");
                    int startIndex = namespace.indexOf("/");
                    final String value = namespace.substring(startIndex + 1, namespace.length());
                    updateProjectNamespace(value);
                    dismiss();
                    ((OnProjectSelectionListener)getParentFragment()).handleProjectSelection();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void updateProjectNamespace(String namespace){

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(Utils.PROJECT_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Utils.PROJECT_NAMESPACE, namespace);
        editor.commit();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
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

    private void listProjectsFromConsole(final String accessToken){
        new ListProjectsTask(mContext, "https://cloudresourcemanager.googleapis.com/v1beta1/projects", mListProjectsCallBack, accessToken).execute();
    }


    Callback mListProjectsCallBack = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {

        }

        @Override
        public void onResponse(Response response) throws IOException {

            try {
                showProgressDialog();
                String body = response.body().string();
                final JSONObject jsonResponse = new JSONObject(body);
                if (!response.isSuccessful()) {
                    final JSONObject jsonError = jsonResponse.getJSONObject("error");
                    final int errorCode = jsonError.getInt("code");
                    final String message = jsonError.getString("message");
                    ProximityApiErrorDialogFragment errorDialogFragment;
                    hideProgressDialog();
                    switch (errorCode) {
                        case Utils.ERROR_UNAUTHORIZED:
                            mServerAuthCode = ((OnProjectSelectionListener)getParentFragment()).getServerAuthCode();
                            if(mServerAuthCode != null) {
                                final String refreshToken = mContext.getSharedPreferences(Utils.ACCESS_TOKEN_INFO, Context.MODE_PRIVATE).getString(Utils.REFRESH_TOKEN, "");
                                if(!refreshToken.isEmpty())
                                    new RefreshAccessTokenTask(mRefreshAccessTokenCallback, refreshToken, getString(R.string.server_client_id), mServerAuthCode).execute();
                            } else {
                                dismiss();
                                ((OnProjectSelectionListener)getParentFragment()).displayGoogleSignInDialog();
                            }
                            return;
                        default:
                            errorDialogFragment = ProximityApiErrorDialogFragment.newInstance(String.valueOf(errorCode), message, "");
                            errorDialogFragment.show(getChildFragmentManager(), null);
                            return;
                    }
                }
                Log.v(Utils.TAG, jsonResponse.toString());
                parseProjectsFromResponse(jsonResponse);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void parseProjectsFromResponse(JSONObject jsonResponse){
        try {
            mProjectList.clear();
            mAdapterProjectsList.notifyDataSetChanged();
            Project project;
            JSONObject tempJsonObj;
            mOkButton.setVisibility(View.VISIBLE);
            if(jsonResponse.has("projects"))
            {
                JSONArray jsonArr = jsonResponse.getJSONArray("projects");
                mProjectsListView.setVisibility(View.VISIBLE);

                for (int i = 0; i < jsonArr.length(); i++) {
                    tempJsonObj = jsonArr.getJSONObject(i);
                    project = new Project(tempJsonObj.getString("name"), tempJsonObj.getString("projectId"));
                    mProjectList.add(project);
                }
                mAdapterProjectsList.notifyDataSetChanged();
            } else {
                mProjectsListView.setVisibility(View.GONE);
                SpannableString stringUrl = new SpannableString(Utils.DEVELOPER_CONSOLE_URL);
                stringUrl.setSpan(new UnderlineSpan(), 0, Utils.DEVELOPER_CONSOLE_URL.length(), 0);
                mProjectsSummary.setVisibility(View.VISIBLE);
                mProjectsSummary.setText(getString(R.string.no_projects_available));
            }
            hideProgressDialog();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    Callback mRefreshAccessTokenCallback = new Callback() {
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
                    dismiss();
                    ((OnProjectSelectionListener)getParentFragment()).displayGoogleSignInDialog();
                } else {
                    final String access_token = jsonResponse.getString("access_token");
                    saveNewAccessToken(access_token);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void saveNewAccessToken(final String accessToken){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(Utils.ACCESS_TOKEN_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Utils.ACCESS_TOKEN, accessToken);
        editor.commit();
    }
}

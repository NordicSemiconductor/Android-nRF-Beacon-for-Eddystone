package no.nordicsemi.android.nrfbeacon.nearby.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by rora on 18.07.2016.
 */
public class Utils {

    public static final String EXTRA_ADAPTER_POSITION = "no.nordicsemi.android.nrfbeacon.extra.adapter_position";
    public static final String TAG = "BEACON";
    private static final String ACCOUNT_NAME_PREF = "userAccount";
    private static final String SHARED_PREFS_NAME = "nrfNearbyInfo";
    public static final String NEARBY_DEVICE_DATA = "NEARBY_DEVICE_DATA";
    private static final String AUTH_PROXIMITY_API = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";
    public static final String AUTH_SCOPE_CONSOLE_PROJECTS = "oauth2:https://www.googleapis.com/auth/cloud-platform";
    public static final String DISPLAY_NOTIFICATION = "no.nordicsemi.android.nrfbeacon.nearby.DISPLAY_NOTIFICATION";
    public static final String REMOVE_NOTIFICATION = "no.nordicsemi.android.nrfbeacon.nearby.REMOVE_NOTIFICATION";
    public static final String NEW_MESSAGE_FOUND = "no.nordicsemi.android.nrfbeacon.nearby.NEW_MESSAGE_FOUND";
    public static final String MESSAGE_LOST = "no.nordicsemi.android.nrfbeacon.nearby.MESSAGE_LOST";
    public static final String NEARBY_SETTINGS_HELP = "NEARBY_SETTINGS_HELP";

    public static final int OPEN_ACTIVITY_REQ = 195; // random
    public static final int REQUEST_CODE_USER_ACCOUNT = 1002;
    public static final int REQUEST_NEARBY_SETTINGS = 2521;
    public static final int REQUEST_PERMISSION_REQ_CODE = 76; // any 8-bit number
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_RESOLVE_ERROR = 261; //random
    public static final int NOTIFICATION_ID = 1;
    public static final int REQUEST_UPDATE_SETTINGS = 252;

    public static final int ERROR_ACCESS_REVOKED = 400;
    public static final int ERROR_UNAUTHORIZED = 401;
    public static final int ERROR_ALREADY_EXISTS = 409;
    public static final int GET_TOKEN = 320;
    public static final int CLEAR_TOKEN = 321;

    public static final String PROJECT_INFO = "PROJECT_INFO";
    public static final String PROJECT_NAME = "PROJECT_NAME";
    public static final String PROJECT_ID = "PROJECT_ID";
    public static String PROJECT_NAMESPACE = "PROJECT_NAMESPACE";
    public static final String REQUEST_GET = "GET";
    public static final String REQUEST_POST = "POST";
    public static final int SIGN_IN = 121;
    public static final String ACCESS_TOKEN_INFO = "ACCESS_TOKEN_INFO";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String DEVELOPER_CONSOLE_URL = "https://console.cloud.google.com";
    public static String API_KEY = "YOUR_API_KEY_GOES_HERE";
    public static String MESSAGE = "MESSAGE";
    public static final String UTILS_API_KEY = "YOUR_API_KEY_GOES_HERE";
    public static final String UTILS_CLIENT_ID = "YOUR_CLIENT_ID_GOES_HERE";
    public static final String ACCESS_TOKEN_URL = "https://www.googleapis.com/oauth2/v4/token";
}

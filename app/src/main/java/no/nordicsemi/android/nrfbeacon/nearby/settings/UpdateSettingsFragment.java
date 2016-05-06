package no.nordicsemi.android.nrfbeacon.nearby.settings;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.common.AboutDialogFragment;
import no.nordicsemi.android.nrfbeacon.nearby.common.OnTutorialsEnabledListener;
import uk.co.deanwild.materialshowcaseview.PrefsManager;

/**
 * Created by rora on 27.04.2016.
 */
public class UpdateSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String GITHUB_APP_URL = "https://github.com/NordicSemiconductor/Android-nRF-Beacon-for-Eddystone";
    private static final String GITHUB_FIRMWARE_URL = "https://github.com/NordicSemiconductor/nrf5-sdk-for-eddystone";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_update);

        final Preference appSource = findPreference(getString(R.string.nrf_beacon_for_eddystone_key));
        appSource.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_APP_URL));
                startActivity(browserIntent);
                return false;
            }
        });

        final Preference firmwareSource = findPreference(getString(R.string.nrf5_sdk_for_eddystone_key));
        firmwareSource.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_FIRMWARE_URL));
                startActivity(browserIntent);
                return false;
            }
        });

        final Preference tutorial = findPreference(getString(R.string.tutorial_settings_key));
        tutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                PrefsManager.resetAll(getActivity());
                ((OnTutorialsEnabledListener) getActivity()).onTutorialsEnabled(true);
                Toast.makeText(getActivity(), getString(R.string.tutorial_enabled), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.v("BEACON", "Callback: " + preference.getKey());
        return false;
    }
}

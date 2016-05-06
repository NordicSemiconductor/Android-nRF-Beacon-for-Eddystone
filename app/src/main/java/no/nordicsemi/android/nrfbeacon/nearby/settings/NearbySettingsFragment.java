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
package no.nordicsemi.android.nrfbeacon.nearby.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.common.OnTutorialsEnabledListener;
import uk.co.deanwild.materialshowcaseview.PrefsManager;

public class NearbySettingsFragment extends PreferenceFragment {

	private static final String GITHUB_APP_URL = "https://github.com/NordicSemiconductor/Android-nRF-Beacon-for-Eddystone";
	private static final String GITHUB_FIRMWARE_URL = "https://github.com/NordicSemiconductor/nrf5-sdk-for-eddystone";
	private boolean mBackgroundScanningEnabled;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings_nearby);
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
}

/*************************************************************************************************************************************************
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
 ************************************************************************************************************************************************/

package no.nordicsemi.android.nrfbeacon.nearby.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import no.nordicsemi.android.nrfbeacon.nearby.R;

public class AboutPreference extends Preference {

	public AboutPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public AboutPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onClick() {
		final View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_dialog_about, null);

		// Create dialog
		final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				dialog.dismiss();
			}
		}).create();

		// Configure buttons
		view.findViewById(R.id.action_facebook).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/227282803964174"));
				final PackageManager packageManager = getContext().getPackageManager();
				final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
				if (list.isEmpty()) {
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/nordicsemiconductor"));
				}
				getContext().startActivity(intent);
				dialog.dismiss();
			}
		});
		view.findViewById(R.id.action_twitter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/NordicTweets"));
				getContext().startActivity(intent);
				dialog.dismiss();
			}
		});
		view.findViewById(R.id.action_linkedin).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("linkedin://company/23302")); // This does not work in LinkedIn 3.3.3 (the current until now)
				final PackageManager packageManager = getContext().getPackageManager();
				final List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
				if (list.isEmpty()) {
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://touch.www.linkedin.com/?dl=no#company/23302"));
				}
				getContext().startActivity(intent);
				dialog.dismiss();
			}
		});
		view.findViewById(R.id.action_youtube).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/user/NordicSemi"));
				getContext().startActivity(intent);
				dialog.dismiss();
			}
		});
		view.findViewById(R.id.action_devzone).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://devzone.nordicsemi.com/questions/"));
				getContext().startActivity(intent);
				dialog.dismiss();
			}
		});

		// Obtain version number
		try {
			final String versionName = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
			final TextView version = (TextView) view.findViewById(R.id.version);
			version.setText(getContext().getString(R.string.version, versionName));
		} catch (final Exception e) {
			// do nothing
		}

		dialog.show();
	}
}

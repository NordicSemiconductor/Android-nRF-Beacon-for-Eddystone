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
package no.nordicsemi.android.nrfbeacon.nearby;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import no.nordicsemi.android.nrfbeacon.nearby.beacon.BeaconsFragment;
import no.nordicsemi.android.nrfbeacon.nearby.update.UpdateFragment;

public class MainActivity extends AppCompatActivity {

    public static final String OPENED_FROM_LAUNCHER = "no.nordicsemi.android.nrfbeacon.nearby.extra.opened_from_launcher";
    public static final String TAG = "BEACON";
    private static final int REQUEST_RESOLVE_ERROR = 261; //random
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_PRX_API = 1003;
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER = 1004;

    private BeaconsFragment mBeaconsFragment;
    private UpdateFragment mUpdateFragment;
    private int mTabPosition = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_RESOLVE_ERROR:
                if(resultCode == Activity.RESULT_OK){
                    mBeaconsFragment.updateNearbyPermissionStatus(true);
                    mBeaconsFragment.checkGoogleApiClientConnectionStateAndSubscribe();
                } else {
                    mBeaconsFragment.updateNearbyPermissionStatus(false);
                    Toast.makeText(this, getString(R.string.rationale_permission_denied), Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_PRX_API:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(this, getString(R.string.retry_beacon_registration), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.rationale_permission_denied) + ". Unable to register beacon", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR_FOR_URL_SHORTNER:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(this, getString(R.string.retry_url_shortner), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.rationale_permission_denied) + ". Unable to shorten URL", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure that Bluetooth exists
        if (!ensureBleExists())
            finish();

        // Setup the custom toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Prepare the sliding tab layout and the view pager
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        final ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        //pager.setOffscreenPageLimit(2);
        pager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        tabLayout.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(final int position) {
                mTabPosition = position;
                switch (position){
                    case 0:

                        break;
                    case 1:
                        //mUpdateFragment.ensurePermissionGranted(new String[]{Manifest.permission.GET_ACCOUNTS});
                        break;
                }
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // we are in main fragment, show 'home up' if entered from Launcher (splash screen activity)
        final boolean openedFromLauncher = getIntent().getBooleanExtra(MainActivity.OPENED_FROM_LAUNCHER, false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(!openedFromLauncher);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Checks whether the device supports Bluetooth Low Energy communication
     *
     * @return <code>true</code> if BLE is supported, <code>false</code> otherwise
     */
    private boolean ensureBleExists() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public void setBeaconsFragment(BeaconsFragment beaconsFragment) {
        this.mBeaconsFragment = beaconsFragment;
    }

    public void setUpdateFragment(UpdateFragment updateFragment) {
        this.mUpdateFragment = updateFragment;
    }

    /**
     * Return a tab position
     * @return
     */
    public int getTabPosition() {
        return mTabPosition;
    }

    private class FragmentAdapter extends FragmentPagerAdapter {

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new BeaconsFragment();
                default:
                case 1:
                    return new UpdateFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getStringArray(R.array.tab_title)[position];
        }
    }
}

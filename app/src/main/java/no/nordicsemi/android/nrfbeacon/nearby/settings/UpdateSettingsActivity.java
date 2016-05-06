package no.nordicsemi.android.nrfbeacon.nearby.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.common.OnTutorialsEnabledListener;
import no.nordicsemi.android.nrfbeacon.nearby.update.UpdateFragment;

/**
 * Created by rora on 27.04.2016.
 */
public class UpdateSettingsActivity extends AppCompatActivity implements OnTutorialsEnabledListener{

    private boolean mTutorialsEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.settings_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState == null)
            getFragmentManager().beginTransaction().replace(R.id.content, new UpdateSettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("TUTORIALS_ENABLED", mTutorialsEnabled);
        setResult(RESULT_OK, result);
        super.onBackPressed();
    }

    @Override
    public void onTutorialsEnabled(boolean flag) {
        mTutorialsEnabled = flag;
    }
}

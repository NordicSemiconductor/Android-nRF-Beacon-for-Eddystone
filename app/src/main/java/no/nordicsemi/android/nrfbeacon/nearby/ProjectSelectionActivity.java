package no.nordicsemi.android.nrfbeacon.nearby;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import no.nordicsemi.android.nrfbeacon.nearby.update.ProjectSelectionDialogFragment;

/**
 * Created by rora on 18.07.2016.
 */
public class ProjectSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_selection);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Project Selection");

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction().replace(R.id.project_container, ProjectSelectionDialogFragment.newInstance(), null).commit();
        }

    }
}

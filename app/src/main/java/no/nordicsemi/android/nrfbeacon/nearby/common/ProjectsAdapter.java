package no.nordicsemi.android.nrfbeacon.nearby.common;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.sample.libproximitybeacon.Project;

import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.MainActivity;
import no.nordicsemi.android.nrfbeacon.nearby.R;

/**
 * Created by rora on 22.10.2015.
 */
public class ProjectsAdapter extends BaseAdapter {

    private ArrayList<Project> mProjectsList;
    private LayoutInflater mInflator;
    private Context context;

    public ProjectsAdapter(Context context, ArrayList<Project> nearbyDevicesMessage) {
        super();
        mProjectsList = nearbyDevicesMessage;
        this.context = context;
        mInflator = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mProjectsList.size();
    }

    @Override
    public Project getItem(int position) {
        return mProjectsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        mProjectsList.clear();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflator.inflate(R.layout.listitem_project, null);
            viewHolder = new ViewHolder();
            viewHolder.projectId = (TextView) convertView.findViewById(R.id.attachment_message);
            viewHolder.name = (TextView) convertView.findViewById(R.id.namespace_message);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Project project = mProjectsList.get(position);
        String name = project.getName();
        String projectId = project.getProjectId();
        viewHolder.name.setText(name);
        viewHolder.projectId.setText(projectId);

        return convertView;
    }

    private static class ViewHolder {
        TextView name;
        TextView projectId;
    }
}


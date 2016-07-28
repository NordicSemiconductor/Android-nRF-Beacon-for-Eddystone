package no.nordicsemi.android.nrfbeacon.nearby.common;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.nearby.messages.Message;

import java.nio.charset.Charset;
import java.util.ArrayList;

import no.nordicsemi.android.nrfbeacon.nearby.R;

/**
 * Created by rora on 22.10.2015.
 */
public class EddystoneBeaconsAdapter extends BaseAdapter {

    private ArrayList<Message> nearbyDeviceMessageList;
    private LayoutInflater mInflator;
    private Context context;

    public EddystoneBeaconsAdapter(Context context, ArrayList<Message> nearbyDevicesMessage) {
        super();
        nearbyDeviceMessageList = nearbyDevicesMessage;
        this.context = context;
        mInflator = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return nearbyDeviceMessageList.size();
    }

    @Override
    public Object getItem(int position) {
        return nearbyDeviceMessageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        nearbyDeviceMessageList.clear();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflator.inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.tvAttachment = (TextView) convertView.findViewById(R.id.attachment_message);
            viewHolder.tvNamespace = (TextView) convertView.findViewById(R.id.namespace_message);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Message message = nearbyDeviceMessageList.get(position);
        String namespace = message.getNamespace();
        final String attachment = new String(message.getContent(), Charset.forName("UTF-8"));
        SpannableString attachmentContent = null;
        if(attachment.startsWith("http")) {
            attachmentContent = new SpannableString(attachment);
            attachmentContent.setSpan(new UnderlineSpan(), 0, attachment.length(), 0);
            viewHolder.tvAttachment.setText(attachmentContent);
        } else {
            viewHolder.tvAttachment.setText(attachment);
        }
        if (namespace != null && namespace.length() > 0)
            viewHolder.tvNamespace.setText(namespace);
        //viewHolder.projectId.setText(attachment);

        if(attachmentContent != null && attachmentContent.toString().startsWith("http")) {
            //viewHolder.projectId.setPaintFlags(viewHolder.projectId.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            viewHolder.tvAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(attachment));
                    context.startActivity(browserIntent);
                }
            });
        } else {
            viewHolder.tvAttachment.setOnClickListener(null);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tvNamespace;
        TextView tvAttachment;
    }
}


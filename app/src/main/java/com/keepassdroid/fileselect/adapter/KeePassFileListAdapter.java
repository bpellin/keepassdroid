package com.keepassdroid.fileselect.adapter;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.keepass.R;

import java.util.LinkedList;
import java.util.List;

public class KeePassFileListAdapter extends BaseAdapter {

    private Context context;
    private List<KeePassFileListItem> items = new LinkedList<>();

    public KeePassFileListAdapter(Context context, List<KeePassFileListItem> items) {
        this.context = context;
        this.items = items;
    }

    public void addAdapterItem(KeePassFileListItem item) {
        items.add(item);
    }

    public int getCount() {
        return items.size();
    }

    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;

        if (convertView != null) {
            rowView = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.file_row, parent, false);
        }

        AppCompatTextView fileNameTextView = (AppCompatTextView) rowView.findViewById(R.id.file_name);
        AppCompatTextView filePathTextView = (AppCompatTextView) rowView.findViewById(R.id.file_path);

        KeePassFileListItem currentFile = (KeePassFileListItem) getItem(position);
        fileNameTextView.setText(currentFile.getFileName());
        filePathTextView.setText(currentFile.getFilePath());

        return rowView;
    }
}

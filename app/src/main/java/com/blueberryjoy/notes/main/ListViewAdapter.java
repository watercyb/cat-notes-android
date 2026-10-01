package com.blueberryjoy.notes.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.blueberryjoy.notes.BuildConfig;
import com.blueberryjoy.notes.R;

import java.util.List;

public class ListViewAdapter extends ArrayAdapter<ListViewItem> {

    public ListViewAdapter(Context context, List<ListViewItem> data) {
        super(context, 0, data);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.listview_main_item, parent, false);
        }

        ListViewItem item = getItem(position);

        TextView text = convertView.findViewById(R.id.textView);

        if (!BuildConfig.DEBUG) text.setTextSize(18);

        assert item != null;
        text.setText(item.getTitle());

        return convertView;
    }
}
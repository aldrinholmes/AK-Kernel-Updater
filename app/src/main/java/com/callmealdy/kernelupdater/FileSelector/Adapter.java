package com.callmealdy.kernelupdater.FileSelector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.callmealdy.kernelupdater.R;
import com.callmealdy.kernelupdater.Tools;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Mike on 9/22/2014.
 */
public class Adapter extends ArrayAdapter {

    public ArrayList<File> files;
    private Context context;

    public Adapter(Context context, int resource, ArrayList<File> files) {
        super(context, resource, files);
        this.files = files;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.file_browser_list_item, null);

        if (files.get(position).isDirectory()) {
            convertView.findViewById(R.id.imageView1).setBackground(context.getResources().getDrawable(R.drawable.ic_folder_white_24dp));
        } else if (Tools.getFileExtension(files.get(position)).equalsIgnoreCase(".zip")) {
            convertView.findViewById(R.id.imageView1).setBackground(context.getResources().getDrawable(R.drawable.archive_blue));
        }
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(files.get(position).isDirectory() ? position == 0 ? ".." : File.separator + files.get(position).getName() : files.get(position).getName());

        return convertView;
    }
}

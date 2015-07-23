package com.callmealdy.kernelupdater.FileSelector;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.callmealdy.kernelupdater.R;
import com.callmealdy.kernelupdater.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Mike on 9/22/2014.
 */
public class FileBrowser extends Activity {

    public static String ACTION_DIRECTORY_SELECTED = "THEMIKE10452.FB.FOLDER.SELECTED";
    public File WORKING_DIRECTORY;
    public Boolean PICK_FOLDERS_ONLY;
    Comparator<File> comparator = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            if (f1.isDirectory() && f2.isFile())
                return -2;
            else if (f1.isFile() && f2.isDirectory())
                return 2;
            else
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
        }
    };
    private ListView list;
    private ArrayList<File> items;
    private ArrayList<String> ALLOWED_EXTENSIONS;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_browser_layout);
        overridePendingTransition(R.anim.slide_in_btt, R.anim.stay_still);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getActionBar() != null)
                getActionBar().setElevation(5);
        }

        Bundle extras = getIntent().getExtras();

        try {
            ALLOWED_EXTENSIONS = extras.getStringArrayList("ALLOWED_EXTENSIONS");
        } catch (NullPointerException e) {
            ALLOWED_EXTENSIONS = null;
        }
        try {
            PICK_FOLDERS_ONLY = extras.getBoolean("PICK_FOLDERS_ONLY");
        } catch (NullPointerException e) {
            PICK_FOLDERS_ONLY = false;
        }
        try {
            Bundle bundle = new Bundle();
            bundle.putString("folder", extras.getString("START"));
            updateScreen(bundle);
        } catch (NullPointerException ignored) {
            updateScreen(null);
        }

        findViewById(R.id.btn_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent out = new Intent(ACTION_DIRECTORY_SELECTED);
                out.putExtra("folder", WORKING_DIRECTORY.getAbsolutePath() + File.separator);
                sendBroadcast(out);
                finish();
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public void updateScreen(Bundle pac) {
        final File root = pac == null ?
                Environment.getExternalStorageDirectory() : new File(pac.getString("folder")).isDirectory() ?
                new File(pac.getString("folder")) : Environment.getExternalStorageDirectory();

        WORKING_DIRECTORY = root;
        ((TextView) findViewById(R.id.textView_cd)).setText(root.getAbsolutePath());
        list = (ListView) findViewById(R.id.list);

        if (items == null)
            items = new ArrayList<File>();
        else
            items.clear();

        if (root.listFiles() != null) {
            for (File f : root.listFiles()) {
                if (f.isDirectory()) {
                    items.add(f);
                } else if (!PICK_FOLDERS_ONLY) {
                    if (ALLOWED_EXTENSIONS != null && f.getName().lastIndexOf(".") > 0 && ALLOWED_EXTENSIONS.indexOf(Tools.getFileExtension(f)) > -1) {
                        items.add(f);
                    } else if (ALLOWED_EXTENSIONS == null) {
                        items.add(f);
                    }
                }
            }

            Collections.sort(items, comparator);
        }

        if (root.getParentFile() != null)
            items.add(0, root.getParentFile());

        final Adapter myAdapter = new Adapter(FileBrowser.this, R.layout.file_browser_list_item, items);
        adapter = myAdapter;
        list.setAdapter(myAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (myAdapter.files.get(i).isDirectory()) {
                    if (i == 0 && root.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()))
                        return;
                    Bundle pac = new Bundle();
                    pac.putString("folder", myAdapter.files.get(i).getAbsolutePath());
                    updateScreen(pac);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (WORKING_DIRECTORY.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()))
            return;
        Bundle pac = new Bundle();
        pac.putString("folder", adapter.files.get(0).getAbsolutePath());
        updateScreen(pac);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay_still, R.anim.slide_in_ttb);
    }

}

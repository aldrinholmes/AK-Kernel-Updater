package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.FileSelector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.R;
import hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.Tools;

/**
 * Created by Mike on 9/22/2014.
 */
public class FileBrowser extends Activity {

    public static String ACTION_DIRECTORY_SELECTED = "THEMIKE10452.FB.FOLDER.SELECTED";
    public File WORKING_DIRECTORY;
    public Boolean PICK_FOLDERS_ONLY;
    ListView list;
    ArrayList<File> items;
    ArrayList<String> ALLOWED_EXTENSIONS;
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
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_browser_layout);

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
            refresh(bundle);
        } catch (NullPointerException ignored) {
            refresh(null);
        }

        findViewById(R.id.btn_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent out = new Intent(ACTION_DIRECTORY_SELECTED);
                out.putExtra("folder", WORKING_DIRECTORY.getAbsolutePath());
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

    public void refresh(Bundle pac) {
        final File root = pac == null ? Environment.getExternalStorageDirectory() : new File(pac.getString("folder"));
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
                    refresh(pac);
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
        refresh(pac);
    }

}

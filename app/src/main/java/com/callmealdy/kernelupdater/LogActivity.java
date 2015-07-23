package com.callmealdy.kernelupdater;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.callmealdy.kernelupdater.FileSelector.FileBrowser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Mike on 12/13/2014.
 */
public class LogActivity extends ActionBarActivity {

    private TextView text;
    private ScrollView scrollView;
    private StringBuilder builder;
    private SharedPreferences preferences;

    private static String DEFAULT_NAME = "last_kmsg_%s.txt";
    private static String DEFAULT_LOC = Environment.getExternalStorageDirectory().getAbsolutePath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_layout);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = getSharedPreferences("Settings", MODE_MULTI_PROCESS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getSupportActionBar().setElevation(5);

        scrollView = (ScrollView) findViewById(R.id.scroller);
        text = (TextView) findViewById(R.id.text);
        text.setTextIsSelectable(true);

        final File last_kmsg = new File("/proc/last_kmsg");
        if (!last_kmsg.exists() || !last_kmsg.isFile()) {
            text.setText("last_kmsg NF");
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(LogActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setIndeterminate(true);
                dialog.setMessage(getString(R.string.msg_pleaseWait));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    builder = new StringBuilder();
                    InputStreamReader ISreader = new InputStreamReader(Runtime.getRuntime().exec("su -c cat " + last_kmsg.getAbsolutePath()).getInputStream());
                    BufferedReader reader = new BufferedReader(ISreader);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n\r");
                    }
                    reader.close();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                text.post(new Runnable() {
                    @Override
                    public void run() {
                        text.setText(builder.toString());
                    }
                });
                if (dialog != null && dialog.isShowing())
                    text.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            scrollView.smoothScrollTo(0, text.getBottom());
                        }
                    }, 500);
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_rtl, R.anim.slide_out_rtl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save: {
                save();
                break;
            }
            case R.id.action_search: {
                search();
                break;
            }
            case R.id.action_pageDown: {
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.smoothScrollTo(0, scrollView.getScrollY() + scrollView.getHeight());
                    }
                });
                break;
            }
            case R.id.action_pageUp: {
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.smoothScrollTo(0, scrollView.getScrollY() - scrollView.getHeight());
                    }
                });
                break;
            }
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        final EditText name, location;
        final Button save, browse;

        final Dialog d = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.log_save_layout);
        d.setCancelable(true);
        d.show();

        name = (EditText) d.findViewById(R.id.name);
        location = (EditText) d.findViewById(R.id.location);
        save = (Button) d.findViewById(R.id.save);
        browse = (Button) d.findViewById(R.id.browse);

        name.setText(String.format(DEFAULT_NAME, new SimpleDateFormat("MM_dd__hh_mm").format(new Date())));
        location.setText(preferences.getString("log_save_lastused_location", DEFAULT_LOC));

        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LogActivity.this, FileBrowser.class);
                i.putExtra("PICK_FOLDERS_ONLY", true);
                i.putExtra("START", location.getText().toString());
                startActivity(i);
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        unregisterReceiver(this);
                        try {
                            if (intent.getAction().equals(FileBrowser.ACTION_DIRECTORY_SELECTED))
                                location.setText(intent.getStringExtra("folder"));
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                };
                registerReceiver(receiver, new IntentFilter(FileBrowser.ACTION_DIRECTORY_SELECTED));
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final File out = new File((location.getText().toString().endsWith(File.separator) ? location.getText().toString() : location.getText().toString() + File.separator) + name.getText().toString());

                    if (!out.getParentFile().exists() || !out.getParentFile().isDirectory()) {
                        out.getParentFile().mkdirs();
                    }

                    if (!out.exists() || !out.isFile()) {
                        out.createNewFile();
                    }

                    PrintWriter writer = new PrintWriter(new FileWriter(out));
                    writer.print(builder.toString());
                    writer.flush();
                    writer.close();

                    if (d.isShowing())
                        d.dismiss();

                    preferences.edit().putString("log_save_lastused_location", location.getText().toString()).apply();
                    Toast.makeText(getApplicationContext(), R.string.btn_ok, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void search() {

        final RelativeLayout topSearch = (RelativeLayout) findViewById(R.id.topSearch);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            topSearch.setElevation(5);

            int cx = topSearch.getRight();
            int cy = topSearch.getTop();
            int finalRadius = (int) Math.sqrt(Math.pow(topSearch.getWidth(), 2) + Math.pow(topSearch.getHeight(), 2));

            if (topSearch.getVisibility() == View.INVISIBLE) {
                Animator animator = ViewAnimationUtils.createCircularReveal(topSearch, cx, cy, 0, finalRadius);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationEnd(animation);
                        topSearch.setVisibility(View.VISIBLE);
                    }
                });
                animator.start();
            } else {
                Animator animator = ViewAnimationUtils.createCircularReveal(topSearch, cx, cy, finalRadius, 0);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        topSearch.setVisibility(View.INVISIBLE);
                    }
                });
                animator.start();
            }
        } else {
            topSearch.setVisibility(topSearch.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
        }
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                topSearch.setVisibility(View.INVISIBLE);
                InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(topSearch.getWindowToken(), 0);

                String toFind = ((EditText) findViewById(R.id.searchBox)).getText().toString().trim();

                if (toFind.length() > 0) {
                    new AsyncTask<Void, Void, Boolean>() {
                        SpannableString sString;
                        ProgressDialog dialog;

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            dialog = new ProgressDialog(LogActivity.this);
                            dialog.setCancelable(false);
                            dialog.setIndeterminate(true);
                            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            dialog.setMessage(getString(R.string.msg_pleaseWait));
                            dialog.show();
                        }

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            sString = new SpannableString(builder.toString());
                            String tmp = builder.toString();
                            ArrayList<Integer> indexes = new ArrayList<>();
                            String toFind = ((EditText) findViewById(R.id.searchBox)).getText().toString().trim();
                            String toRep = "";
                            for (int i = 0; i < toFind.length(); i++) {
                                toRep += " ";
                            }

                            while (tmp.contains(toFind.toLowerCase()) || tmp.contains(toFind.toUpperCase())) {
                                if (tmp.contains(toFind.toLowerCase())) {
                                    int i = tmp.indexOf(toFind.toLowerCase());
                                    indexes.add(i);
                                    tmp = tmp.replaceFirst(toFind.toLowerCase(), toRep);
                                }
                                if (tmp.contains(toFind.toUpperCase())) {
                                    int i = tmp.indexOf(toFind.toUpperCase());
                                    indexes.add(i);
                                    tmp = tmp.replaceFirst(toFind.toUpperCase(), toRep);
                                }
                            }
                            for (int i = 0; i < indexes.size(); i++) {
                                int start = indexes.get(i);
                                int end = start + toFind.length();
                                sString.setSpan(new BackgroundColorSpan(Color.YELLOW), start, end, 0);
                                sString.setSpan(new RelativeSizeSpan(2f), start, end, 0);
                            }
                            return indexes.size() > 0;
                        }

                        @Override
                        protected void onPostExecute(Boolean bool) {
                            super.onPostExecute(bool);
                            if (dialog != null && dialog.isShowing()) {
                                dialog.hide();
                            }
                            if (bool) {
                                text.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        text.setText(sString);
                                        Toast.makeText(getApplicationContext(), R.string.msg_textHighlighted, Toast.LENGTH_LONG).show();
                                    }
                                }, 100);
                            } else {
                                Toast.makeText(getApplicationContext(), R.string.msg_textNotFound, Toast.LENGTH_LONG).show();
                            }
                        }
                    }.execute();
                } else {
                    text.setText(builder.toString());
                }
            }
        });
    }

}

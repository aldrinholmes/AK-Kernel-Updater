package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Mike on 9/19/2014.
 */
public class Main extends Activity {

    public static SharedPreferences preferences;
    private File HOST;
    private String DEVICE = Build.DEVICE;
    private String DEVICE_PART, CHANGELOG;
    private boolean DEVICE_SUPPORTED;
    private Tools tools;

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View v = super.onCreateView(name, context, attrs);
        Tools.setDefaultFont(Main.this, "Roboto-Thin.ttf", "Roboto-Thin.ttf");
        return v;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        overridePendingTransition(R.anim.slide_in_ltr, R.anim.slide_out_ltr);
        final Tools tools = new Tools(this);
        this.tools = tools;
        HOST = new File(getFilesDir() + File.separator + "host");

        preferences = getSharedPreferences("Settings", MODE_MULTI_PROCESS);

        initSettings();

        final LinearLayout main = ((LinearLayout) findViewById(R.id.main));

        final View v1 = LayoutInflater.from(this).inflate(R.layout.kernel_info_layout, null);
        ((TextView) v1.findViewById(R.id.text)).setText(tools.getFormattedKernelVersion());

        final Card card1 = new Card(this, "Installed kernel", false, v1);
        card1.getPARENT().setAnimation(getIntroSet(1000, 0));

        main.addView(card1.getPARENT());
        card1.getPARENT().animate();

        final ProgressBar progressBar = new ProgressBar(Main.this);
        progressBar.setAnimation(getIntroSet(1000, 400));
        main.addView(progressBar);
        progressBar.animate();

        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Boolean>() {
                    Card card;

                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        return tools.downloadFile(Keys.DEFAULT_SOURCE, HOST.getAbsolutePath(), false);
                    }

                    @Override
                    protected void onPostExecute(Boolean downloadSuccessful) {
                        super.onPostExecute(downloadSuccessful);

                        if (!downloadSuccessful) {
                            return;
                        }

                        DEVICE_SUPPORTED = false;

                        getDevicePart();

                        if (!DEVICE_SUPPORTED) {
                            return;
                        }

                        progressBar.postOnAnimation(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                        progressBar.startAnimation(getOutroSet(600, 0));

                        if (getLatestVerionName().equalsIgnoreCase(Tools.INSTALLED_KERNEL_VERSION)) {
                            TextView textView = new TextView(Main.this);
                            textView.setText("You are up-to-date");
                            textView.setGravity(Gravity.CENTER_HORIZONTAL);
                            textView.setTextAppearance(Main.this, android.R.style.TextAppearance_Medium);
                            textView.setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-ThinItalic.ttf"), Typeface.BOLD_ITALIC);
                            textView.setTextColor(getResources().getColor(R.color.card_text_light));
                            main.addView(textView);
                            textView.startAnimation(getIntroSet(1200, 0));
                            return;
                        }

                        View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.new_kernel_layout, null);
                        ((TextView) v.findViewById(R.id.text)).setText(getLatestVerionName());

                        v.findViewById(R.id.btn_changelog).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                TextView textView = new TextView(Main.this);
                                textView.setText(CHANGELOG);
                                textView.setHorizontallyScrolling(true);
                                textView.setTextAppearance(getApplicationContext(), android.R.style.TextAppearance_Small);
                                textView.setTextColor(getResources().getColor(R.color.card_text));
                                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                                View view1 = LayoutInflater.from(Main.this).inflate(R.layout.blank_view, null);
                                view1.setPadding(15, 15, 15, 15);
                                ((LinearLayout) view1).addView(textView, params);
                                new AlertDialog.Builder(Main.this)
                                        .setView(view1)
                                        .setTitle("Changelog")
                                        .setNeutralButton("Dismiss", null)
                                        .show();

                            }
                        });

                        v.findViewById(R.id.btn_getLatestVersion).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                final String link = getLatestDownloadLink();
                                if (link != null) {
                                    final boolean useAndroidDownloadManager = false;
                                    if (!useAndroidDownloadManager) {
                                        showProgressDialog();
                                    }
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... voids) {
                                            tools.downloadFile(link, preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, null) + getLatestZipName(), useAndroidDownloadManager);
                                            return null;
                                        }
                                    }.execute();
                                }
                            }
                        });

                        card = new Card(getApplicationContext(), "Latest version", false, v);

                        main.addView(card.getPARENT());
                        card.getPARENT().startAnimation(getIntroSet(1000, 200));

                    }
                }.execute();
            }
        }, 1000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_refresh:
                onCreate(null);
                return true;
            case R.id.action_settings:
                Intent i = new Intent(Main.this, Settings.class);
                startActivity(i);
                return true;
        }
        return false;
    }

    private AnimationSet getIntroSet(int duration, int startOffset) {
        AlphaAnimation animation1 = new AlphaAnimation(0, 1);

        TranslateAnimation animation2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_PARENT, -1,
                Animation.RELATIVE_TO_SELF, 0);

        final AnimationSet set = new AnimationSet(false);
        set.addAnimation(animation1);
        set.addAnimation(animation2);
        set.setDuration(duration);
        set.setStartOffset(startOffset);

        return set;
    }

    private AnimationSet getOutroSet(int duration, int startOffset) {
        AlphaAnimation animation1 = new AlphaAnimation(1, 0);

        TranslateAnimation animation2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 10);

        final AnimationSet set = new AnimationSet(false);
        set.addAnimation(animation1);
        set.addAnimation(animation2);
        set.setDuration(duration);
        set.setStartOffset(startOffset);

        return set;
    }

    private void getDevicePart() throws DeviceNotSupportedException {
        Scanner s;
        DEVICE_PART = "";
        CHANGELOG = "";
        try {
            s = new Scanner(HOST);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //TODO
            return;
        }
        String pattern = String.format("<%s>", DEVICE);
        String line;
        while (s.hasNextLine()) {
            if ((line = s.nextLine()).equalsIgnoreCase(pattern)) {
                DEVICE_SUPPORTED = true;
                DEVICE_PART += line + "\n";
                break;
            }
        }
        if (DEVICE_SUPPORTED) {
            while (s.hasNextLine()) {
                line = s.nextLine();
                if (line.equalsIgnoreCase(String.format("<%s/>", DEVICE)))
                    break;

                if (line.equalsIgnoreCase("<changelog>")) {
                    DEVICE_PART += line + "\n";
                    while (s.hasNextLine() && !(line = s.nextLine()).equalsIgnoreCase("</changelog>")) {
                        CHANGELOG += line + "\n";
                        DEVICE_PART += line + "\n";
                    }
                }

                DEVICE_PART += line + "\n";
            }
        }
    }

    private String getLatestVerionName() {
        Scanner s = new Scanner(DEVICE_PART);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.toLowerCase().contains(Keys.KEY_KERNEL_VERSION))
                return line.split(":=")[1];
        }
        s.close();
        return "Unavailable";
    }

    private String getLatestZipName() {
        String raw = null;
        HttpURLConnection connection = null;
        try {
            raw = (connection = (HttpURLConnection) new URL(getLatestDownloadLink()).openConnection()).getHeaderField("Content-Disposition");
        } catch (Exception e) {
            if (connection != null)
                connection.disconnect();
        }

        if (raw != null && raw.contains("=")) {
            try {
                if (raw.split("=")[1].contains(";"))
                    return raw.split("=")[1].split(";")[0].replaceAll("\"", "");
                else
                    return raw.split("=")[1];
            } finally {
                Log.d("TAG", raw);
                connection.disconnect();
            }
        } else {
            Scanner s = new Scanner(DEVICE_PART);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.toLowerCase().contains(Keys.KEY_KERNEL_ZIPNAME))
                    return line.split(":=")[1];

            }
            s.close();
            return "hC-latest.zip";
        }
    }

    private String getLatestDownloadLink() {
        Scanner s = new Scanner(DEVICE_PART);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.contains(Keys.KEY_KERNEL_HTTPLINK)) {
                s.close();
                return line.split(":=")[1];
            }
        }
        s.close();
        return null;
    }

    private void showProgressDialog() {
        final CustomProgressDialog dialog = new CustomProgressDialog(this);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                tools.cancelDownload = true;
            }
        });
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setProgress((tools.downloadedSize / tools.downloadSize) * 100);
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (dialog.isShowing()) {
                        while (!tools.isDownloading) {
                            //just wait
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                double done = tools.downloadedSize, total = tools.downloadSize;
                                Double progress = (done / total) * 100;
                                dialog.setIndeterminate(false);
                                String done_mb = String.format("%.2g%n", total / Math.pow(2, 20)).trim();
                                String total_mb = String.format("%.2g%n", done / Math.pow(2, 20)).trim();
                                dialog.update(tools.lastDownloadedFile.getName(), done_mb, total_mb);
                                dialog.setProgress(progress.intValue());
                            }
                        });

                        if (!tools.isDownloading)
                            break;

                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void initSettings() {
        if (preferences.getString(Keys.KEY_SETTINGS_SOURCE, null) == null)
            preferences.edit().putString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE).apply();

        if (preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, null) == null)
            preferences.edit().putString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator).apply();

    }

    @Override
    public void onBackPressed() {

    }
}

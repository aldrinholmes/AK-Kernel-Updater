package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
                        DEVICE_SUPPORTED = false;
                        return getDevicePart();
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        super.onPostExecute(success);

                        progressBar.postOnAnimation(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                        progressBar.startAnimation(getOutroSet(600, 0));

                        if (!success) {
                            displayOnScreenMessage(main, R.string.failed_try_again);
                            return;
                        }

                        if (!DEVICE_SUPPORTED) {
                            displayOnScreenMessage(main, R.string.device_not_supported);
                            return;
                        }

                        if (getLatestVerionName().equalsIgnoreCase(Tools.INSTALLED_KERNEL_VERSION)) {
                            displayOnScreenMessage(main, R.string.up_to_date);
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
                                    final boolean b = preferences.getBoolean(Keys.KEY_SETTINGS_USEANDM, false);
                                    String destination = preferences
                                            .getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

                                    BroadcastReceiver downloadHandler = new BroadcastReceiver() {
                                        @Override
                                        public void onReceive(Context context, Intent intent) {
                                            unregisterReceiver(this);
                                            boolean md5Matched = intent.getBooleanExtra("match", true);
                                            if (md5Matched) {
                                                //TODO
                                            } else {
                                                //TODO
                                            }
                                        }
                                    };

                                    registerReceiver(downloadHandler, new IntentFilter(Tools.EVENT_DOWNLOAD_COMPLETE));
                                    registerReceiver(downloadHandler, new IntentFilter(Tools.EVENT_FILE_EXISTS));

                                    tools.downloadFile(Main.this, link, destination, getLatestZipName(), getLatestMD5(), b);
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

    private boolean getDevicePart() throws DeviceNotSupportedException {
        Scanner s;
        DEVICE_PART = "";
        CHANGELOG = "";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(Keys.DEFAULT_SOURCE).openConnection();
            s = new Scanner(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
        return true;
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
        Scanner s = new Scanner(DEVICE_PART);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.toLowerCase().contains(Keys.KEY_KERNEL_ZIPNAME))
                return line.split(":=")[1];

        }
        s.close();
        return "hC-latest.zip";
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

    private String getLatestMD5() {
        Scanner s = new Scanner(DEVICE_PART);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.contains(Keys.KEY_KERNEL_MD5)) {
                s.close();
                return line.split(":=")[1];
            }
        }
        s.close();
        return null;
    }

    private void displayOnScreenMessage(LinearLayout main, int msgId) {
        TextView textView = new TextView(Main.this);
        textView.setText(msgId);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setTextAppearance(Main.this, android.R.style.TextAppearance_Medium);
        textView.setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-ThinItalic.ttf"), Typeface.BOLD_ITALIC);
        textView.setTextColor(getResources().getColor(R.color.card_text_light));
        main.addView(textView);
        textView.startAnimation(getIntroSet(1200, 0));
    }

    private void initSettings() {

        SharedPreferences.Editor editor = preferences.edit();

        if (preferences.getString(Keys.KEY_SETTINGS_SOURCE, null) == null)
            editor.putString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE);

        if (preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, null) == null)
            editor.putString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator);

        if (!preferences.getBoolean(Keys.KEY_SETTINGS_USEANDM, false))
            editor.putBoolean(Keys.KEY_SETTINGS_USEANDM, false);

        if (preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true))
            editor.putBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true);

        if (preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, null) == null)
            editor.putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0");
        else if (!tools.isAllDigits(preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, null).replace(":", "")))
            editor.putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0");

        editor.apply();

    }

    @Override
    public void onBackPressed() {

    }
}

package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
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

    private File HOST;
    private String DEVICE = Build.DEVICE;
    private String DEVICE_PART, CHANGELOG;
    private boolean DEVICE_SUPPORTED;

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View v = super.onCreateView(name, context, attrs);
        Tools.setDefaultFont(Main.this, "Roboto-Thin", "Roboto-Thin.ttf");
        return v;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        final Tools tools = new Tools(this);
        HOST = new File(getFilesDir() + File.separator + "host");

        SharedPreferences preferences = getSharedPreferences("Settings", MODE_MULTI_PROCESS);
        if (preferences.getString(Keys.KEY_CURRENT_SOURCE, null) == null)
            preferences.edit().putString(Keys.KEY_CURRENT_SOURCE, Keys.DEFAULT_SOURCE).apply();

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
                    protected void onPostExecute(Boolean aBoolean) {
                        super.onPostExecute(aBoolean);

                        if (!aBoolean) {
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
                                    new AsyncTask<Void, Void, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... voids) {
                                            tools.downloadFile(link, Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getLatestZipName(), true);
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
        Scanner s = null;
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
        return "Unavailable";
    }

    private String getLatestZipName() {
        String raw = null;
        try {
            raw = ((HttpURLConnection) new URL(getLatestDownloadLink()).openConnection()).getHeaderField("Content-Disposition");
        } catch (Exception e) {
        }

        if (raw != null && raw.contains("=")) {
            return raw.split("=")[1];
        } else {
            Scanner s = new Scanner(DEVICE_PART);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.toLowerCase().contains(Keys.KEY_KERNEL_ZIPNAME))
                    if (line.toLowerCase().contains(".zip"))
                        return line.split(":=")[1];
                    else
                        return line.split(":=")[1] + ".zip";
            }
            return "hC-latest.zip";
        }
    }

    private String getLatestDownloadLink() {
        Scanner s = new Scanner(DEVICE_PART);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.contains(Keys.KEY_KERNEL_HTTPLINK))
                return line.split(":=")[1];
        }
        return null;
    }

}

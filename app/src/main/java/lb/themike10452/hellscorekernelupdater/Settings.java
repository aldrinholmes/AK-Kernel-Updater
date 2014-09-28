package lb.themike10452.hellscorekernelupdater;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import lb.themike10452.hellscorekernelupdater.FileSelector.FileBrowser;
import lb.themike10452.hellscorekernelupdater.Services.BackgroundAutoCheckService;

/**
 * Created by Mike on 9/22/2014.
 */
public class Settings extends Activity {

    private TextView AC_H, AC_M;
    private TextWatcher intervalChanger = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence sequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence sequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            findViewById(R.id.editText_autocheck_h).post(new Runnable() {
                @Override
                public void run() {
                    String hours = AC_H.getText().toString(), minutes = AC_M.getText().toString();
                    if ((Tools.isAllDigits(hours) && Integer.parseInt(hours) == 0) && (Tools.isAllDigits(minutes) && Integer.parseInt(minutes) == 0))
                        return;
                    Main.preferences.edit().putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, (hours.length() > 0 ? hours : "0") + ":" + (minutes.length() > 0 ? minutes : "0")).apply();
                }
            });
        }
    };
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            updateScreen();
            if (s.equals(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL) && Main.preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true)) {
                Intent intent = new Intent(Settings.this, BackgroundAutoCheckService.class);
                stopService(intent);
                startService(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        overridePendingTransition(R.anim.slide_in_rtl, R.anim.slide_out_rtl);

        updateScreen();

        ((CheckBox) findViewById(R.id.checkbox_useAndDM)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_USEANDM, b).apply();
            }
        });

        findViewById(R.id.btn_dlLoc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Settings.this, FileBrowser.class);
                i.putExtra("START", Main.preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, ""));
                i.putExtra("PICK_FOLDERS_ONLY", true);
                startActivity(i);
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(FileBrowser.ACTION_DIRECTORY_SELECTED)) {
                            String newF = intent.getStringExtra("folder");
                            Main.preferences.edit().putString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, newF).apply();
                            ((TextView) findViewById(R.id.textView_dlLoc)).setText(newF);
                            unregisterReceiver(this);
                        }
                    }
                };
                registerReceiver(receiver, new IntentFilter(FileBrowser.ACTION_DIRECTORY_SELECTED));
            }
        });

        findViewById(R.id.btn_upSrc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View child = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.blank_view, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                child.setLayoutParams(params);
                child.setPadding(30, 30, 30, 30);
                final EditText editText = new EditText(Settings.this);
                editText.setSingleLine();
                editText.setHorizontallyScrolling(true);
                if (!Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE).equalsIgnoreCase(Keys.DEFAULT_SOURCE)) {
                    editText.setText(Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE));
                }
                TextView textView = new TextView(Settings.this);
                textView.setText(R.string.prompt_blankDefault);
                ((LinearLayout) child).addView(textView, params);
                ((LinearLayout) child).addView(editText, params);
                Dialog dialog = new Dialog(Settings.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(child);
                dialog.show();
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        String newSource = editText.getText().toString().trim();
                        if (newSource.length() == 0) {
                            newSource = Keys.DEFAULT_SOURCE;
                        }
                        Main.preferences.edit().putString(Keys.KEY_SETTINGS_SOURCE, newSource).apply();
                    }
                });
            }
        });

        ((Switch) findViewById(R.id.switch_bkg_check)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, b).apply();
            }
        });

        AC_H.addTextChangedListener(intervalChanger);
        AC_M.addTextChangedListener(intervalChanger);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                View child = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.blank_view, null);
                final NumberPicker picker = new NumberPicker(Settings.this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                picker.setMaxValue(view == AC_H ? 168 : 59);
                picker.setMinValue(0);
                picker.setValue(Integer.parseInt(Main.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[(view == AC_H ? 0 : 1)]));
                ((LinearLayout) child).addView(picker, params);
                ((LinearLayout) child).setGravity(Gravity.CENTER_HORIZONTAL);
                child.setPadding(30, 0, 30, 0);
                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setTitle(R.string.settings_textView_backgroundCheckInterval);
                builder.setView(child);
                builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        updateTextView((TextView) view, picker.getValue() + "");
                    }
                });
                builder.setNegativeButton(R.string.btn_cancel, null);
                builder.show();
            }
        };

        AC_H.setOnClickListener(listener);
        AC_M.setOnClickListener(listener);

        AC_H.post(new Runnable() {
            @Override
            public void run() {
                AC_H.setLayoutParams(new LinearLayout.LayoutParams(AC_H.getMeasuredWidth(), AC_H.getMeasuredWidth()));
                AC_M.setLayoutParams(new LinearLayout.LayoutParams(AC_M.getMeasuredWidth(), AC_M.getMeasuredWidth()));
            }
        });

        ((Switch) findViewById(R.id.switch_bkg_check)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main.preferences.edit().putBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, b).apply();
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linear_bkg_check_edit);
                for (int i = 0; i < (linearLayout).getChildCount(); i++) {
                    linearLayout.getChildAt(i).setEnabled(b);
                }
                if (!b && BackgroundAutoCheckService.running)
                    stopService(new Intent(Settings.this, BackgroundAutoCheckService.class));
                else if (b && !BackgroundAutoCheckService.running)
                    startService(new Intent(Settings.this, BackgroundAutoCheckService.class));
            }
        });

        Main.preferences.registerOnSharedPreferenceChangeListener(prefListener);

    }

    void updateTextView(TextView v, String s) {
        v.setText(s);
    }

    private void updateScreen() {
        ((Switch) findViewById(R.id.switch_bkg_check)).setChecked(Main.preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true));
        ((TextView) findViewById(R.id.textView_dlLoc)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_DOWNLOADLOCATION, ""));
        ((CheckBox) findViewById(R.id.checkbox_useAndDM)).setChecked(Main.preferences.getBoolean(Keys.KEY_SETTINGS_USEANDM, false));
        (AC_H = (TextView) findViewById(R.id.editText_autocheck_h)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[0]);
        (AC_M = (TextView) findViewById(R.id.editText_autocheck_m)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:00").split(":")[1]);
        if (Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE).equalsIgnoreCase(Keys.DEFAULT_SOURCE))
            ((TextView) findViewById(R.id.textView_upSrc)).setText(getString(R.string.defaultt));
        else
            ((TextView) findViewById(R.id.textView_upSrc)).setText(Main.preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_ltr, R.anim.slide_out_ltr);
    }
}

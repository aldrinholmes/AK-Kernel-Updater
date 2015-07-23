package com.callmealdy.kernelupdater.Services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;

import com.callmealdy.kernelupdater.DeviceNotSupportedException;
import com.callmealdy.kernelupdater.Kernel;
import com.callmealdy.kernelupdater.KernelManager;
import com.callmealdy.kernelupdater.Keys;
import com.callmealdy.kernelupdater.Main;
import com.callmealdy.kernelupdater.R;
import com.callmealdy.kernelupdater.Tools;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Mike on 9/26/2014.
 */
public class BackgroundAutoCheckService extends Service {

    private static final String ACTION = "lb.themike10452.hellscore-u2d.kick";

    private static BroadcastReceiver connectivityReceiver, receiver;

    private static AlarmManager manager;

    private static PendingIntent pendingIntent;

    //this is the background check task
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            boolean DEVICE_SUPPORTED = true;
            boolean CONNECTED = false;
            try {
                CONNECTED = getDevicePart();
            } catch (DeviceNotSupportedException e) {
                DEVICE_SUPPORTED = false;
                BackgroundAutoCheckService.this.stopSelf();
            }

            //if the device is not supported, kill the task
            if (!DEVICE_SUPPORTED) {
                stopSelf();
                return;
            }

            //imagine the scenario where the user sets autocheck interval to 24 hours
            //the app will check once every 24 hours
            //what if at that time the phone wasn't connected to the internet? That would be bad.
            //the app will have to wait another 24 hours to check again...
            //but no! we have to find another way

            if (!CONNECTED && connectivityReceiver == null) { //if the phone was not connected by the time
                //set up a broadcast receiver that detects when the phone is connected to the internet
                connectivityReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                        NetworkInfo info = manager.getActiveNetworkInfo();
                        boolean isConnected = info != null && info.isConnected();

                        if (isConnected) { //if the phone is connected, relaunch a new fresh cycle
                            //unregister the broadcast receiver when it receives the targeted intent
                            //so it doesn't interfere with any newly created receivers in the future
                            unregisterReceiver(this);
                            connectivityReceiver = null;
                            //then launch a new cycle
                            //by stopping and relaunching the service
                            stopSelf();
                            startService(new Intent(BackgroundAutoCheckService.this, BackgroundAutoCheckService.class));
                        }
                    }
                };
                //here we register the broadcast receiver to catch any connectivity change action
                registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

            } else if (CONNECTED) { //else if the phone was connected by the time, we need to check for an update

                //get installed and latest kernel info, and compare them
                Tools.getFormattedKernelVersion();
                String installed = Tools.INSTALLED_KERNEL_VERSION;
                KernelManager.getInstance(getApplicationContext()).sniffKernels(DEVICE_PART);
                Kernel properKernel = KernelManager.getInstance(getApplicationContext()).getProperKernel();
                String latest = properKernel != null ? properKernel.getVERSION() : null;

                //if the user hasn't opened the app and selected which ROM base he uses (AOSP/CM/MIUI etc...)
                //latest will be null
                //we should stop our work until the user sets the missing ROM flag
                if (latest == null) {
                    stopSelf();
                    return;
                }

                //display a notification to the user in case of an available update
                if (!installed.equalsIgnoreCase(latest)) {
                    Intent intent1 = new Intent(BackgroundAutoCheckService.this, Main.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(BackgroundAutoCheckService.this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                    Notification notif = new Notification.Builder(getApplicationContext())
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.msg_updateFound)).build();
                    notif.flags = Notification.FLAG_AUTO_CANCEL;
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(Keys.TAG_NOTIF, 3721, notif);
                }

            }
        }
    };
    private SharedPreferences preferences;
    private String DEVICE_PART;

    public BackgroundAutoCheckService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        //actual work starts here

        preferences = getSharedPreferences("Settings", MODE_MULTI_PROCESS);

        //get the autocheck interval setting value
        String pref = preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0");
        //handle any corruptions that might have happened to the value by returning to the default value (12h00m)
        if (!Tools.isAllDigits(pref.replace(":", ""))) {
            preferences.edit().putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0").apply();
            pref = "12:0";
        }
        //extract the 'hours' part
        String hr = pref.split(":")[0];
        //extract the 'minutes' part
        String mn = pref.split(":")[1];

        //parse them into integers and transform the total amount of time into seconds
        int T = (Integer.parseInt(hr) * 3600) + (Integer.parseInt(mn) * 60);

        //prepare the broadcast receiver
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new Thread(run).start();
            }
        };
        registerReceiver(receiver, new IntentFilter(ACTION));

        //run the check task at a fixed rate
        manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, T * 1000, pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION), 0));

    }

    private boolean getDevicePart() throws DeviceNotSupportedException {
        Scanner s;
        DEVICE_PART = "";
        try {
            if (preferences.getBoolean(Keys.KEY_SETTINGS_USEPROXY, false)) {
                final String proxyHost = preferences.getString(Keys.KEY_SETTINGS_PROXYHOST, Keys.DEFAULT_PROXY);
                System.setProperty("http.proxySet", "true");
                System.setProperty("http.proxyHost", proxyHost.substring(0, proxyHost.indexOf(":")));
                System.setProperty("http.proxyPort", proxyHost.substring(proxyHost.indexOf(":") + 1));
                System.setProperty("https.proxyHost", proxyHost.substring(0, proxyHost.indexOf(":")));
                System.setProperty("https.proxyPort", proxyHost.substring(proxyHost.indexOf(":") + 1));
            } else {
                System.setProperty("http.proxySet", "true");
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(preferences.getString(Keys.KEY_SETTINGS_SOURCE, Keys.DEFAULT_SOURCE)).openConnection();
            s = new Scanner(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        String pattern = String.format("<%s>", Build.DEVICE);

        boolean supported = false;
        while (s.hasNextLine()) {
            if (s.nextLine().equalsIgnoreCase(pattern)) {
                supported = true;
                break;
            }
        }
        if (supported) {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.equalsIgnoreCase(String.format("</%s>", Build.DEVICE)))
                    break;
                DEVICE_PART += line + "\n";
            }
            return true;
        } else {
            throw new DeviceNotSupportedException();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (manager != null && pendingIntent != null)
            manager.cancel(pendingIntent);
        if (receiver != null)
            unregisterReceiver(receiver);
    }
}

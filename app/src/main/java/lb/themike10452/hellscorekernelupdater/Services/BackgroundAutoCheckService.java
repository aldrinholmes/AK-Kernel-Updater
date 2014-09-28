package lb.themike10452.hellscorekernelupdater.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import lb.themike10452.hellscorekernelupdater.DeviceNotSupportedException;
import lb.themike10452.hellscorekernelupdater.Keys;
import lb.themike10452.hellscorekernelupdater.Main;
import lb.themike10452.hellscorekernelupdater.R;
import lb.themike10452.hellscorekernelupdater.Tools;

/**
 * Created by Mike on 9/26/2014.
 */
public class BackgroundAutoCheckService extends IntentService {

    public static boolean running = false;
    private static boolean IO_IS_BUSY;
    private static BroadcastReceiver broadcastReceiver;
    //this is the background check task
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            IO_IS_BUSY = true;
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

            if (!CONNECTED && broadcastReceiver == null) { //if the phone was not connected by the time
                Log.d("TAG", "Connection not found, timer canceled");
                //set up a broadcast receiver that detects when the phone is connected to the internet
                broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                        NetworkInfo info = manager.getActiveNetworkInfo();
                        boolean isConnected = info != null && info.isConnected();

                        if (isConnected) { //if the phone is connected, relaunch a new fresh cycle
                            Log.d("TAG", "Connection detected");
                            //unregister the broadcast receiver when it receives the targeted intent
                            //so it doesn't interfere with any newly created receivers in the future
                            unregisterReceiver(this);
                            broadcastReceiver = null;
                            //then launch a new cycle
                            //by stopping and relaunching the service
                            stopSelf();
                            startService(new Intent(BackgroundAutoCheckService.this, BackgroundAutoCheckService.class));
                        }
                    }
                };
                //here we register the broadcast receiver to catch any connectivity change action
                registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                //then we kill the loop while waiting for a connection to relaunch
                stopSelf();
            } else if (CONNECTED) { //else if the phone was connected by the time, we need to check for an update
                Log.d("TAG", "Checking");

                //get installed and latest kernel info, and compare them
                Tools.getFormattedKernelVersion();
                String installed = Tools.INSTALLED_KERNEL_VERSION;
                String latest = getLatestVerionName();

                //display a notification to the user in case of an available update
                if (!installed.equalsIgnoreCase(latest)) {
                    Log.d("TAG", "Update found");
                    Intent intent1 = new Intent(BackgroundAutoCheckService.this, Main.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(BackgroundAutoCheckService.this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                    Notification notif = new Notification.Builder(getApplicationContext())
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.msg_updateFound)).build();
                    notif.flags = Notification.FLAG_AUTO_CANCEL;
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    manager.notify(Keys.TAG_NOTIF, 3721, notif);
                }

            }
            IO_IS_BUSY = false;
        }
    };
    private String DEVICE_PART;

    public BackgroundAutoCheckService() {
        super("BackgroundAutoCheckService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        running = true;

        SharedPreferences preferences = getSharedPreferences("Settings", MODE_MULTI_PROCESS);

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

        Log.d("TAG", "Service started >> " + T);

        //run the check task at a fixed rate
        //I created a boolean to break the endless loop whenever I want to stop it
        while (running) {

            if (!IO_IS_BUSY)
                new Thread(run).start();
            else
                Log.d("TAG", "IO is busy");

            try {
                //sleep for T milliseconds
                Thread.sleep(T * 1000); //transform T from seconds to milliseconds
            } catch (InterruptedException ignored) {
            }

        }
    }

    private boolean getDevicePart() throws DeviceNotSupportedException {
        Scanner s;
        DEVICE_PART = "";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(Keys.DEFAULT_SOURCE).openConnection();
            s = new Scanner(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        String pattern = String.format("<%s>", Build.DEVICE);

        boolean supported = false;
        while (s.hasNextLine()) {
            String line;
            if ((line = s.nextLine()).equalsIgnoreCase(pattern)) {
                DEVICE_PART += line + "\n";
                supported = true;
                break;
            }
        }
        if (supported) {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.equalsIgnoreCase(String.format("<%s/>", Build.DEVICE)))
                    break;
                DEVICE_PART += line + "\n";
            }
            return true;
        } else {
            throw new DeviceNotSupportedException();
        }

    }

    private String getLatestVerionName() {
        Scanner s = new Scanner(DEVICE_PART);
        while (s.hasNextLine()) {
            String line = s.nextLine();

            if (line.length() == 0)
                continue;

            if (line.charAt(0) == '_' && line.toLowerCase().contains(Keys.KEY_KERNEL_VERSION))
                return line.split(":=")[1];
        }
        s.close();
        return "Unavailable";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("TAG", "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        Log.d("TAG", "Service destroyed");
    }
}

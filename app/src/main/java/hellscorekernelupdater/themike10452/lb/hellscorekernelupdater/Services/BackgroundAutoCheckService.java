package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.Services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.DeviceNotSupportedException;
import hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.Keys;
import hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.Main;
import hellscorekernelupdater.themike10452.lb.hellscorekernelupdater.Tools;

/**
 * Created by Mike on 9/26/2014.
 */
public class BackgroundAutoCheckService extends IntentService {

    private boolean running;
    private String DEVICE_PART;

    public BackgroundAutoCheckService(String name) {
        super(name);
        running = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        while (running) {
            //get the autocheck interval setting value
            String pref = Main.preferences.getString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0");
            //handle any corruptions that might have happened to the value by returning to the default value (12h00m)
            if (!Tools.isAllDigits(pref)) {
                Main.preferences.edit().putString(Keys.KEY_SETTINGS_AUTOCHECK_INTERVAL, "12:0").apply();
                pref = "12:0";
            }
            //extract the 'hours' part
            String hr = pref.split(":")[0];
            //extract the 'minutes' part
            String mn = pref.split(":")[1];

            //parse them into integers and transform the total amount of time into seconds
            int T = (Integer.parseInt(hr) * 3600) + (Integer.parseInt(mn) * 60);

            //run the check task at a fixed rate
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    getDevicePart();
                }
            }, 0, T * 1000); //transform T from seconds to milliseconds

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
            return false;
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
        running = false;
    }
}

package com.callmealdy.kernelupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.callmealdy.kernelupdater.Services.BackgroundAutoCheckService;

/**
 * Created by Mike on 9/26/2014.
 */
public class BootCompleteBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED) {
            SharedPreferences preferences = context.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS);
            if (preferences.getBoolean(Keys.KEY_SETTINGS_AUTOCHECK_ENABLED, true))
                context.startService(new Intent(context, BackgroundAutoCheckService.class));
        }
    }
}

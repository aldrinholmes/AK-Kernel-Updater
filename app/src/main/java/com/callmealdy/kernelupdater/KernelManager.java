package com.callmealdy.kernelupdater;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Iterator;

/**
 * Created by Mike on 10/23/2014.
 */
public class KernelManager {

    public static boolean baseMatchedOnce = false, apiMatchedOnce = false;

    private static KernelManager instance = null;

    private static UniqueSet<Kernel> kernelSet;

    private SharedPreferences preferences;

    private KernelManager(Context c) {
        kernelSet = new UniqueSet<>();
        baseMatchedOnce = false;
        preferences = c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS);
        instance = this;
    }

    public static KernelManager getInstance(Context c) {
        return instance == null ? new KernelManager(c) : instance;
    }

    public boolean add(Kernel k) {
        return kernelSet.add(k);
    }

    public void sniffKernels(String data) {
        String[] parameters = data.split("\\+kernel");
        kernelSet.clear();
        for (String params : parameters) {
            if (params.equals(parameters[0]))
                continue;
            add(new Kernel(params));
        }
    }

    public Kernel getProperKernel() {
        apiMatchedOnce = false;
        baseMatchedOnce = false;

        if (kernelSet.isEmpty()) {
            return null;
        }

        Iterator<Kernel> iterator = kernelSet.iterator();

        while (iterator.hasNext()) {
            Kernel k = iterator.next();
            try {
                boolean a = k.getBASE().contains(preferences.getString(Keys.KEY_SETTINGS_ROMBASE, "").toUpperCase());
                boolean b = k.getAPI().contains(preferences.getString(Keys.KEY_SETTINGS_ROMAPI, "").toUpperCase());
                if (a)
                    baseMatchedOnce = true;
                if (b)
                    apiMatchedOnce = true;
                if (a & b) {
                    if (!k.isTestBuild() || preferences.getBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, false)) {
                        return k;
                    }
                }
            } catch (NullPointerException ignored) {
            }
        }

        return null;
    }

}

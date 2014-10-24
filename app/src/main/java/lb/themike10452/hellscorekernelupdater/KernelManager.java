package lb.themike10452.hellscorekernelupdater;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Mike on 10/23/2014.
 */
public class KernelManager {

    public static boolean baseMatchedOnce = false, apiMatchedOnce = false;

    private static KernelManager instance = null;

    private static Set<Kernel> kernelSet;

    public KernelManager() {
        kernelSet = new HashSet<Kernel>(5, 0.8f);
        baseMatchedOnce = false;
        instance = this;
    }

    public static KernelManager getInstance() {
        return instance == null ? new KernelManager() : instance;
    }

    public boolean add(Kernel k) {
        return kernelSet.add(k);
    }

    public Kernel getProperKernel(Context c) {
        apiMatchedOnce = false;

        if (kernelSet.isEmpty()) {
            return null;
        }

        SharedPreferences preferences = c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS);

        Kernel res = null;

        for (Kernel k : kernelSet) {
            try {
                boolean a = k.getBASE().equalsIgnoreCase(preferences.getString(Keys.KEY_SETTINGS_ROMBASE, ""));
                boolean b = k.getAPI().contains(preferences.getString(Keys.KEY_SETTINGS_ROMAPI, ""));
                if (a)
                    baseMatchedOnce = a;
                if (b)
                    apiMatchedOnce = b;
                if (a & b) {
                    if (k.isTestBuild() && !preferences.getBoolean(Keys.KEY_SETTINGS_LOOKFORBETA, true)) {
                        res = null;
                    } else {
                        res = k;
                        break;
                    }
                }
            } catch (NullPointerException ignored) {
            }
        }

        return res;
    }

}

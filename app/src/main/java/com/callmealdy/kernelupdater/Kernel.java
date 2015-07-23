package com.callmealdy.kernelupdater;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by Mike on 10/23/2014.
 */
public class Kernel implements Comparable<Kernel> {

    public Set<String> getBASE() {
        String[] all = BASE.split(",");
        Set<String> set = new HashSet(all.length);
        for (String s : all)
            set.add(s.trim().toUpperCase());
        return set;
    }

    public Set<String> getAPI() {
        String[] all = API.split(",");
        Set<String> set = new HashSet(all.length);
        for (String s : all)
            set.add(s.trim().toUpperCase());
        return set;
    }

    public String getVERSION() {
        return VERSION;
    }

    public String getZIPNAME() {
        return ZIPNAME;
    }

    public String getHTTPLINK() {
        return HTTPLINK;
    }

    public boolean isTestBuild() {
        return ISTESTBUILD;
    }

    public String getMD5() {
        return MD5;
    }

    private String PARAMS, BASE, API, VERSION, ZIPNAME, HTTPLINK, MD5;
    private boolean ISTESTBUILD;

    public Kernel(String parameters) {
        PARAMS = new String(parameters);

        if (PARAMS == null)
            return;

        Scanner s = new Scanner(PARAMS);

        try {
            String line;
            while (s.hasNextLine()) {
                line = s.nextLine().trim();
                if (line.length() > 0 && line.startsWith("_")) {
                    if (line.contains(Keys.KEY_KERNEL_BASE))
                        BASE = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_API))
                        API = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_VERSION))
                        VERSION = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_ZIPNAME))
                        ZIPNAME = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_HTTPLINK))
                        HTTPLINK = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_MD5))
                        MD5 = line.split(":=")[1].trim();
                    else if (line.contains(Keys.KEY_KERNEL_test))
                        try {
                            ISTESTBUILD = Boolean.parseBoolean(line.split(":=")[1].trim());
                        } catch (Exception ignored) {

                        }
                }
            }
        } finally {
            s.close();
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", VERSION, ISTESTBUILD, API);
    }

    @Override
    public int compareTo(Kernel another) {
        return getMD5().compareTo(another.getMD5());
    }
}

package com.callmealdy.kernelupdater;

import java.util.ArrayList;

/**
 * Created by Mike on 1/8/2015.
 */
public class UniqueSet<Kernel> extends ArrayList<Kernel> {

    @Override
    public boolean add(Kernel object) {
        if (contains(object))
            return false;
        else
            return super.add(object);
    }
}

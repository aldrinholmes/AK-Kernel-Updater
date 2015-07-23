package com.callmealdy.kernelupdater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Mike on 9/19/2014.
 */
public class Card {
    private Context CONTEXT;
    private String TITLE;
    private View PARENT;

    public Card(Context c, String title, boolean placeSeparators, View... views) {
        this(c, title, null, placeSeparators, views);
    }

    public Card(Context c, String title, View addition, boolean placeSeparators, View... views) {
        TITLE = title;
        PARENT = ((LayoutInflater) (CONTEXT = c).getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.card_layout, null);
        ((TextView) PARENT.findViewById(R.id.card_title)).setText(TITLE);
        LinearLayout container = (LinearLayout) PARENT.findViewById(R.id.card_content);
        for (View view : views) {
            container.addView(view);
            if (placeSeparators && !(view == views[views.length - 1]))
                container.addView(PARENT.findViewById(R.id.card_separator));
        }

        if (addition != null) {
            ((RelativeLayout) PARENT.findViewById(R.id.additional)).addView(addition);
        }
    }

    public String getTITLE() {
        return TITLE;
    }

    public View getPARENT() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 10);
        PARENT.setLayoutParams(params);
        return PARENT;
    }

}

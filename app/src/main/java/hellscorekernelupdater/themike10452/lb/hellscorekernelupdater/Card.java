package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Mike on 9/19/2014.
 */
public class Card {
    private Context CONTEXT;

    public String getTITLE() {
        return TITLE;
    }

    private String TITLE;

    public View getPARENT() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 10);
        PARENT.setLayoutParams(params);
        return PARENT;
    }

    private View PARENT;

    public Card(Context c, String title, boolean placeSeparators, View... views) {
        TITLE = title;
        PARENT = ((LayoutInflater) (CONTEXT = c).getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.card_layout, null);
        ((TextView) PARENT.findViewById(R.id.card_title)).setText(TITLE);
        LinearLayout container = (LinearLayout) PARENT.findViewById(R.id.card_content);
        for (View view : views) {
            container.addView(view);
            if (placeSeparators && !(view == views[views.length - 1]))
                container.addView(PARENT.findViewById(R.id.card_separator));
        }
    }

}

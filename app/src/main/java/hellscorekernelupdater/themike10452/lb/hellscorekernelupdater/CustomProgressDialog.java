package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Mike on 9/22/2014.
 */
public class CustomProgressDialog extends Dialog {

    public static String UNIT = " MB";
    private ProgressBar progressBar;
    private int MAX;
    private TextView FILENAME, FILESIZE, DOWNLOADED, PERCENTAGE;

    public CustomProgressDialog(Context context) {
        super(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.progress_dialog_layout);
        MAX = 100;
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        FILENAME = (TextView) findViewById(R.id.textView_filename);
        FILESIZE = (TextView) findViewById(R.id.textView_filesize);
        DOWNLOADED = (TextView) findViewById(R.id.textView_downloaded);
        PERCENTAGE = (TextView) findViewById(R.id.percentage);
    }

    public void update(String filename, String filesize, String downloaded) {
        FILENAME.setText(filename);
        FILESIZE.setText(filesize + UNIT);
        DOWNLOADED.setText(downloaded + UNIT);
    }

    public void setProgress(int percentage) {
        progressBar.setProgress(percentage);
        PERCENTAGE.setText(percentage + "%");
    }

    public void setIndeterminate(boolean b) {
        progressBar.setIndeterminate(b);
    }
}

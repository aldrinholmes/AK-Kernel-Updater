package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mike on 9/19/2014.
 */
public class Tools {

    private Context C;
    private boolean cancelDownload;

    public static String INSTALLED_KERNEL_VERSION = "";
    private static Tools instance;

    public static Tools getInstance() {
        return instance;
    }

    public Tools(Context context) {
        C = context;
        instance = this;
    }

    public String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            procVersionStr = new BufferedReader(new FileReader(new File("/proc/version"))).readLine();

            final String PROC_VERSION_REGEX =
                    "Linux version (\\S+) " +
                            "\\((\\S+?)\\) " +
                            "(?:\\(gcc.+? \\)) " +
                            "(#\\d+) " +
                            "(?:.*?)?" +
                            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)";

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                return "Unavailable";
            } else {
                return (new StringBuilder(INSTALLED_KERNEL_VERSION = m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {
            return "Unavailable";
        }
    }

    public boolean downloadFile(String httpURL, String destination, boolean useAndroidDownloadManager) {

        if (!useAndroidDownloadManager) {

            InputStream stream = null;
            FileOutputStream outputStream = null;
            HttpURLConnection connection = null;

            try {
                cancelDownload = false;
                connection = (HttpURLConnection) new URL(httpURL).openConnection();
                byte[] buffer = new byte[1024];
                int bufferLength;
                int totalSize, downloadedSize = 0;
                totalSize = connection.getContentLength();

                stream = connection.getInputStream();
                outputStream = new FileOutputStream(new File(destination));
                while ((bufferLength = stream.read(buffer)) > 0 && !cancelDownload) {
                    outputStream.write(buffer, 0, bufferLength);
                    downloadedSize += bufferLength;
                }
            } catch (MalformedURLException e) {
                return false;
            } catch (IOException ee) {
                return false;
            } finally {
                if (stream != null)
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                if (outputStream != null)
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                if (connection != null)
                    connection.disconnect();
            }

            return true;
        } else {

            DownloadManager manager = (DownloadManager) C.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(new DownloadManager.Request(Uri.parse(httpURL)));

            return true;
        }
    }


    public static void setDefaultFont(Context context, String staticTypefaceFieldName, String fontAssetName) {
        final Typeface regular = Typeface.createFromAsset(context.getAssets(), fontAssetName);
        replaceFont(staticTypefaceFieldName, regular);
    }

    protected static void replaceFont(String staticTypefaceFieldName, final Typeface newTypeface) {
        try {
            final Field staticField = Typeface.class.getDeclaredField(staticTypefaceFieldName);
            staticField.setAccessible(true);
            staticField.set(null, newTypeface);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}

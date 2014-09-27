package hellscorekernelupdater.themike10452.lb.hellscorekernelupdater;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mike on 9/19/2014.
 */
public class Tools {

    public static String EVENT_DOWNLOAD_COMPLETE = "THEMIKE10452.TOOLS.DOWNLOAD.COMPLETE";
    public static String EVENT_DOWNLOADEDFILE_EXISTS = "THEMIKE10452.TOOLS.DOWNLOAD.FILE.EXISTS";
    public static String EVENT_DOWNLOAD_CANCELED = "THEMIKE10452.TOOLS.DOWNLOAD.CANCELED";

    public static String INSTALLED_KERNEL_VERSION = "";
    private static Tools instance;
    public boolean cancelDownload;
    public boolean isDownloading;
    public int downloadSize, downloadedSize;
    public File lastDownloadedFile;
    private Context C;

    public Tools(Context context) {
        C = context;
        instance = this;
    }

    public static Tools getInstance() {
        return instance;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static String getFileExtension(File f) {
        try {
            return f.getName().substring(f.getName().lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            return "";
        }
    }

    public static boolean isAllDigits(String s) {
        for (char c : s.toCharArray())
            if (!Character.isDigit(c))
                return false;
        return true;
    }

    public static String getMD5Hash(String filePath) {
        String res = null;
        try {
            return new Scanner(Runtime.getRuntime().exec(String.format("md5 %s", filePath)).getInputStream()).next();
        } catch (Exception e) {
            return res;
        }
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

    public void downloadFile(final Activity activity, final String httpURL, final String destination, final String alternativeFilename, final String MD5hash, boolean useAndroidDownloadManager) {

        cancelDownload = false;
        downloadSize = 0;
        downloadedSize = 0;

        if (!useAndroidDownloadManager) {

            final CustomProgressDialog dialog = new CustomProgressDialog(activity);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancelDownload = true;
                }
            });
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setProgress(0);
            dialog.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream stream = null;
                    FileOutputStream outputStream = null;
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) new URL(httpURL).openConnection();
                        String filename;
                        try {
                            filename = connection.getHeaderField("Content-Disposition");
                            if (filename != null && filename.contains("=")) {
                                if (filename.split("=")[1].contains(";"))
                                    filename = filename.split("=")[1].split(";")[0].replaceAll("\"", "");
                                else
                                    filename = filename.split("=")[1];
                            } else {
                                filename = alternativeFilename;
                            }
                        } catch (Exception e) {
                            filename = alternativeFilename;
                        }

                        lastDownloadedFile = new File(destination + filename);
                        byte[] buffer = new byte[1024];
                        int bufferLength;
                        downloadSize = connection.getContentLength();
                        if (MD5hash != null) {
                            if (lastDownloadedFile.exists() && lastDownloadedFile.isFile()) {
                                if (getMD5Hash(lastDownloadedFile.getAbsolutePath()).equalsIgnoreCase(MD5hash) && !cancelDownload) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.setIndeterminate(false);
                                            String total_mb = String.format("%.2g%n", downloadSize / Math.pow(2, 20)).trim();
                                            dialog.update(lastDownloadedFile.getName(), total_mb, total_mb);
                                            dialog.setProgress(100);
                                            C.sendBroadcast(new Intent(EVENT_DOWNLOADEDFILE_EXISTS));
                                        }
                                    });
                                    return;
                                }
                            }
                        }
                        stream = connection.getInputStream();
                        outputStream = new FileOutputStream(lastDownloadedFile);
                        while ((bufferLength = stream.read(buffer)) > 0) {
                            if (cancelDownload)
                                return;
                            isDownloading = true;
                            outputStream.write(buffer, 0, bufferLength);
                            downloadedSize += bufferLength;
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    double done = downloadedSize, total = downloadSize;
                                    Double progress = (done / total) * 100;
                                    dialog.setIndeterminate(false);
                                    String done_mb = String.format("%.2g%n", done / Math.pow(2, 20)).trim();
                                    String total_mb = String.format("%.2g%n", total / Math.pow(2, 20)).trim();
                                    dialog.update(lastDownloadedFile.getName(), done_mb, total_mb);
                                    dialog.setProgress(progress.intValue());
                                }
                            });
                        }

                        Intent out = new Intent(EVENT_DOWNLOAD_COMPLETE);
                        if (MD5hash != null) {
                            out.putExtra("match", MD5hash.equalsIgnoreCase(getMD5Hash(lastDownloadedFile.getAbsolutePath())));
                            out.putExtra("md5", getMD5Hash(lastDownloadedFile.getAbsolutePath()));
                        }
                        C.sendBroadcast(out);

                    } catch (MalformedURLException e) {
                        return;
                    } catch (IOException ee) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(C.getApplicationContext(), "Timeout", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    } finally {
                        if (cancelDownload)
                            C.sendBroadcast(new Intent(EVENT_DOWNLOAD_CANCELED));
                        dialog.dismiss();
                        isDownloading = false;
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
                }
            }).start();

        } else {

            DownloadManager manager = (DownloadManager) C.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(new DownloadManager.Request(Uri.parse(httpURL)));

        }
    }

}

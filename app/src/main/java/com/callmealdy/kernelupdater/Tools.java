package com.callmealdy.kernelupdater;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by Mike on 9/19/2014.
 */
public class Tools {

    public static String EVENT_DOWNLOAD_COMPLETE = "THEMIKE10452.TOOLS.DOWNLOAD.COMPLETE";
    public static String EVENT_DOWNLOADEDFILE_EXISTS = "THEMIKE10452.TOOLS.DOWNLOAD.FILE.EXISTS";
    public static String EVENT_DOWNLOAD_CANCELED = "THEMIKE10452.TOOLS.DOWNLOAD.CANCELED";
    public static String ACTION_INSTALL = "THEMIKE10452.TOOLS.KERNEL.INSTALL";
    public static String ACTION_DISMISS = "THEMIKE10452.TOOLS.DISMISS";

    public static boolean isDownloading;
    public static Activity activity;

    public static Dialog userDialog;
    public static String INSTALLED_KERNEL_VERSION = "";
    private static Tools instance;
    private static Shell.Interactive interactiveShell;
    public boolean cancelDownload;
    public int downloadSize, downloadedSize;
    public File lastDownloadedFile;
    private Context C;

    private Tools(Context context) {
        C = context;
        instance = this;
        if (interactiveShell == null)
            interactiveShell = new Shell.Builder().useSU().setWatchdogTimeout(5).setMinimalLogging(true).open(new Shell.OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    if (exitCode != SHELL_RUNNING)
                        showRootFailDialog();
                }
            });
    }

    public static Tools getInstance(Context c) {
        return instance == null ? new Tools(c) : instance;
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
        String md5hash = "";
        FileInputStream inputStream = null;

        try {

            inputStream = new FileInputStream(filePath);
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[1024];

            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }

            byte[] digestData = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : digestData) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }

            md5hash = sb.toString();

        } catch (Exception ignored) {
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (Exception ignored){}
        }

        return md5hash;
    }

    public static String getFormattedKernelVersion() {
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

    public static int findIndex(String[] strings, String string) {
        if (strings == null)
            return -1;

        for (int i = 0; i < strings.length; i++) {
            if (strings[i].trim().equals(string.trim()))
                return i;
        }

        return -1;
    }

    public static String retainDigits(String data) {
        String newString = "";
        for (char c : data.toCharArray()) {
            if (Character.isDigit(c) || c == '.')
                newString += c;
        }
        return newString;
    }

    public static boolean validateIP(String ip) {
        int dc = 0;
        boolean valid = !ip.contains("..") && !ip.startsWith(".") && !ip.endsWith(".");

        if (valid)
            for (char c : ip.toCharArray()) {
                if (c == '.')
                    dc++;
                else if (!Character.isDigit(c)) {
                    valid = false;
                    break;
                }
            }

        if (valid && dc == 3) {
            String[] parts = ip.split("\\.");
            for (String s : parts) {
                if (Integer.parseInt(s) > 255)
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public void showRootFailDialog() {
        userDialog = new AlertDialog.Builder(C)
                .setTitle(R.string.dialog_title_rootFail)
                .setMessage(R.string.prompt_rootFail)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
        ((TextView) userDialog.findViewById(android.R.id.message)).setTextAppearance(C, android.R.style.TextAppearance_Small);
        ((TextView) userDialog.findViewById(android.R.id.message)).setTypeface(Typeface.createFromAsset(C.getAssets(), "Roboto-Regular.ttf"));
    }

    public void downloadFile(final String httpURL, final String destination, final String alternativeFilename, final String MD5hash, boolean useAndroidDownloadManager) {

        NotificationManager manager = (NotificationManager) C.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(Keys.TAG_NOTIF, 3721);

        activity = (Activity) C;
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

                        filename = Main.preferences.getBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false) ? Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, filename) : filename;

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

                        new File(destination).mkdirs();
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

                    } catch (final MalformedURLException e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(C.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (final IOException ee) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(C.getApplicationContext(), ee.toString(), Toast.LENGTH_SHORT).show();
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

            new AsyncTask<Void, Void, String>() {
                ProgressDialog dialog;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    dialog = new ProgressDialog(activity);
                    dialog.setIndeterminate(true);
                    dialog.setCancelable(false);
                    dialog.setMessage(C.getString(R.string.msg_pleaseWait));
                    dialog.show();
                    userDialog = dialog;
                }

                @Override
                protected String doInBackground(Void... voids) {
                    String filename;
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(httpURL).openConnection();
                        try {
                            filename = connection.getHeaderField("Content-Disposition");
                            if (filename != null && filename.contains("=")) {
                                if (filename.split("=")[1].contains(";"))
                                    return filename.split("=")[1].split(";")[0].replaceAll("\"", "");
                                else
                                    return filename.split("=")[1];
                            } else {
                                return alternativeFilename;
                            }
                        } catch (Exception e) {
                            return alternativeFilename;
                        }
                    } catch (Exception e) {
                        return alternativeFilename;
                    }
                }

                @Override
                protected void onPostExecute(String filename) {
                    super.onPostExecute(filename);

                    final DownloadManager manager = (DownloadManager) C.getSystemService(Context.DOWNLOAD_SERVICE);

                    filename = Main.preferences.getBoolean(Keys.KEY_SETTINGS_USESTATICFILENAME, false) ? Main.preferences.getString(Keys.KEY_SETTINGS_LASTSTATICFILENAME, filename) : filename;

                    Uri destinationUri = Uri.fromFile(lastDownloadedFile = new File(destination + filename));

                    if (MD5hash != null) {
                        if (lastDownloadedFile.exists() && lastDownloadedFile.isFile()) {
                            if (getMD5Hash(lastDownloadedFile.getAbsolutePath()).equalsIgnoreCase(MD5hash) && !cancelDownload) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        C.startActivity(new Intent(C.getApplicationContext(), Main.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                                        C.sendBroadcast(new Intent(EVENT_DOWNLOADEDFILE_EXISTS));

                                        final NotificationManager manager1 = (NotificationManager) C.getSystemService(Context.NOTIFICATION_SERVICE);

                                        String bigText = C.getString(R.string.prompt_install2, C.getString(R.string.btn_install), "");
                                        bigText = bigText.split("\n")[0] + "\n" + bigText.split("\n")[1];

                                        Notification notification = new Notification.Builder(C)
                                                .setContentTitle(C.getString(R.string.dialog_title_readyToInstall))
                                                .setContentText(bigText)
                                                .setStyle(new Notification.BigTextStyle().bigText(bigText))
                                                .setSmallIcon(R.drawable.ic_notification)
                                                .addAction(R.drawable.ic_action_flash_on, C.getString(R.string.btn_install), PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_INSTALL), 0))
                                                .build();

                                        manager1.notify(Keys.TAG_NOTIF, 3723, notification);

                                        C.registerReceiver(new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context context, Intent intent) {
                                                manager1.cancel(Keys.TAG_NOTIF, 3723);
                                                C.unregisterReceiver(this);
                                                createOpenRecoveryScript("install " + lastDownloadedFile.getAbsolutePath(), true, false);
                                            }
                                        }, new IntentFilter(ACTION_INSTALL));

                                    }
                                });

                                return;

                            } else {
                                lastDownloadedFile.delete();
                            }
                        }
                    }

                    new File(destination).mkdirs();

                    final long downloadID = manager
                            .enqueue(new DownloadManager.Request(Uri.parse(httpURL))
                                    .setDestinationUri(destinationUri));

                    isDownloading = true;

                    dialog.setMessage(C.getString(R.string.dialog_title_downloading));
                    userDialog = dialog;

                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {

                            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {

                                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L) != downloadID)
                                    return;

                                C.unregisterReceiver(this);
                                isDownloading = false;

                                if (userDialog != null)
                                    userDialog.dismiss();

                                DownloadManager.Query query = new DownloadManager.Query();
                                query.setFilterById(downloadID);
                                Cursor cursor = manager.query(query);

                                if (!cursor.moveToFirst()) {
                                    C.sendBroadcast(new Intent(EVENT_DOWNLOAD_CANCELED));
                                    return;
                                }

                                int status = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);

                                if (cursor.getInt(status) == DownloadManager.STATUS_SUCCESSFUL) {

                                    lastDownloadedFile = new File(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)));
                                    C.startActivity(new Intent(C.getApplicationContext(), Main.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                                    Intent out = new Intent(EVENT_DOWNLOAD_COMPLETE);
                                    boolean match = true;
                                    String md5 = MD5hash;

                                    if (MD5hash != null) {
                                        out.putExtra("match", match = MD5hash.equalsIgnoreCase(getMD5Hash(lastDownloadedFile.getAbsolutePath())));
                                        out.putExtra("md5", md5 = getMD5Hash(lastDownloadedFile.getAbsolutePath()));
                                    }
                                    C.sendBroadcast(out);

                                    Intent intent1 = new Intent(ACTION_INSTALL);
                                    Intent intent2 = new Intent(ACTION_DISMISS);
                                    if (match) {

                                        String bigText = C.getString(R.string.prompt_install1, C.getString(R.string.btn_install), "");
                                        bigText = bigText.split("\n")[0] + "\n" + bigText.split("\n")[1];

                                        Notification notification = new Notification.Builder(C.getApplicationContext())
                                                .setSmallIcon(R.drawable.ic_notification)
                                                .setContentTitle(C.getString(R.string.msg_downloadComplete))
                                                .setContentText(bigText)
                                                .addAction(R.drawable.ic_action_flash_on, C.getString(R.string.btn_install), PendingIntent.getBroadcast(activity, 0, intent1, 0))
                                                .setStyle(new Notification.BigTextStyle().bigText(bigText))
                                                .build();
                                        final NotificationManager manager1 = (NotificationManager) C.getSystemService(Context.NOTIFICATION_SERVICE);

                                        C.registerReceiver(new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context context, Intent intent) {
                                                C.unregisterReceiver(this);
                                                if (userDialog != null)
                                                    userDialog.dismiss();
                                                manager1.cancel(Keys.TAG_NOTIF, 3722);

                                                createOpenRecoveryScript("install " + lastDownloadedFile.getAbsolutePath(), true, false);
                                            }
                                        }, new IntentFilter(ACTION_INSTALL));

                                        manager1.notify(Keys.TAG_NOTIF, 3722, notification);

                                    } else {

                                        Notification notification = new Notification.Builder(C.getApplicationContext())
                                                .setSmallIcon(R.drawable.ic_notification)
                                                .setContentTitle(C.getString(R.string.dialog_title_md5mismatch))
                                                .setContentText(C.getString(R.string.prompt_md5mismatch, MD5hash, md5))
                                                .addAction(R.drawable.ic_action_flash_on, C.getString(R.string.btn_install), PendingIntent.getBroadcast(activity, 0, intent1, 0))
                                                .addAction(R.drawable.ic_action_download, C.getString(R.string.btn_downloadAgain), PendingIntent.getBroadcast(activity, 0, intent2, 0))
                                                .setStyle(new Notification.BigTextStyle().bigText(C.getString(R.string.prompt_md5mismatch, MD5hash, md5)))
                                                .build();
                                        final NotificationManager manager1 = (NotificationManager) C.getSystemService(Context.NOTIFICATION_SERVICE);

                                        BroadcastReceiver receiver1 = new BroadcastReceiver() {
                                            @Override
                                            public void onReceive(Context context, Intent intent) {
                                                C.unregisterReceiver(this);

                                                if (userDialog != null)
                                                    userDialog.dismiss();

                                                manager1.cancel(Keys.TAG_NOTIF, 3722);

                                                if (intent.getAction().equals(ACTION_INSTALL)) {
                                                    createOpenRecoveryScript("install " + lastDownloadedFile.getAbsolutePath(), true, false);
                                                } else {
                                                    context.startActivity(new Intent(context, Main.class));
                                                }
                                            }
                                        };

                                        C.registerReceiver(receiver1, new IntentFilter(ACTION_INSTALL));
                                        C.registerReceiver(receiver1, new IntentFilter(ACTION_DISMISS));

                                        manager1.notify(Keys.TAG_NOTIF, 3722, notification);
                                    }

                                } else {
                                    Toast.makeText(C, "error" + ": " + cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    };

                    C.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                }
            }.execute();

        }
    }

    public static Double getMinVer(String data) {
        Scanner s = new Scanner(data);
        try {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("#define min_ver*")) {
                    return Double.parseDouble(line.split("=")[1].trim());
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            s.close();
        }

    }

    public void createOpenRecoveryScript(String line, final boolean rebootAfter, final boolean append) {
        if (interactiveShell != null && interactiveShell.isRunning()) {
            interactiveShell.addCommand("echo " + line + (append ? " >> " : ">") + "/cache/recovery/openrecoveryscript", 23, new Shell.OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    if (exitCode != 0)
                        showRootFailDialog();
                    else if (rebootAfter) {
                        if (activity != null)
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(C, R.string.onReboot, Toast.LENGTH_LONG).show();
                                }
                            });
                        interactiveShell.addCommand("reboot recovery");
                    }
                }
            });

        } else {
            showRootFailDialog();
        }
    }

}

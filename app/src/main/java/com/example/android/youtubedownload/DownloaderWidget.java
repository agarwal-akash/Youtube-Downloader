package com.example.android.youtubedownload;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * Implementation of App Widget functionality.
 */
public class DownloaderWidget extends AppWidgetProvider {
    public static String WIDGET_BUTTON = "akash.WIDGET_BUTTON";

    private final int TIMEOUT_CONNECTION = 5000;//5sec
    private final int TIMEOUT_SOCKET = 30000;//30sec
    public final static String API_KEY="BbAAyDXVMZmshKzH21exFvYMPKVep1I2gccjsnUccEjp9NN8ZV";
    public String Vid;
    public String downloadURL;
    public String name;
    public EditText filename;

    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    //persmission method.
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        Intent intent=new Intent(WIDGET_BUTTON);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.downloader_widget);
        views.setOnClickPendingIntent(R.id.fabWidget,pendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);

        if (WIDGET_BUTTON.equals(intent.getAction())) {
            //verifyStoragePermissions(context);
            String path="";
            ClipboardManager clipboard = (ClipboardManager)context.getSystemService(CLIPBOARD_SERVICE);
            if(clipboard.hasPrimaryClip()== true) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                path = item.getText().toString();
            }

            //name = filename.getText().toString();

            //int start=path.indexOf('=');
            //start++;
            StringBuffer str=new StringBuffer();
            for(int i=path.length()-1;i>=0;i--)
            {
                if(path.charAt(i)=='/')
                    break;
                str.append(path.charAt(i));
            }
            Vid=(str.reverse()).toString();
            //Vid=path.substring(start);
            //Toast.makeText(this,Vid, Toast.LENGTH_SHORT).show();
            Log.d("MainActivity-OnCreate",Vid);
            Toast.makeText(context,"Starting download...",Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    downloadFile(Vid,name,context);
                }
            }).start();
        }
    }

    private void downloadFile(String vid,String file,Context context)
    {

        //getting Vid
        String API_URL="https://ytgrabber.p.mashape.com/app/get/"+vid;
        String finalURL="";
        try {
            URL url = new URL(API_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            String baseAuthStr = API_KEY;
            urlConnection.addRequestProperty("X-Mashape-Key",baseAuthStr);
            urlConnection.addRequestProperty("Accept", "application/json");
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                finalURL= stringBuilder.toString();
                Log.d("download","final url:"+finalURL);
            }
            catch (Exception e)
            {
                Log.e("ERROR",e.getMessage(),e);
            }
            finally{
                urlConnection.disconnect();
            }

            try {
                JSONObject object = (JSONObject) new JSONTokener(finalURL).nextValue();
                JSONArray link = object.getJSONArray("link");
                JSONObject mp4=link.getJSONObject(0);
                downloadURL=mp4.getString("url");
            } catch (JSONException e) {
                // Appropriate error handling code
                Log.e("MainActivity","Could not fetch JSON");
            }
            Log.d("MainActvity","DOWNLOAD URL:"+downloadURL);

        }
        catch(Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            Log.d("Could not connect....","");
        }
        /**
         try
         {
         URL u = null;
         try {
         u = new URL(downloadURL);
         } catch (MalformedURLException e) {
         Log.d("MalformedException ", e.toString());
         }
         long startTime = System.currentTimeMillis();
         if(downloadURL!="")
         Log.i("Download beginning...",downloadURL);
         URLConnection ucon=u.openConnection();
         InputStream is = ucon.getInputStream();
         BufferedInputStream inStream = new BufferedInputStream(is, 1024*1024 * 5);
         FileOutputStream outStream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/Download/"+filename);
         byte[] buff = new byte[5 * 1024*1024];
         //Read bytes (and store them) until there is nothing more to read(-1)
         int len;
         while ((len = inStream.read(buff)) != -1) {
         outStream.write(buff,0,len);
         }
         //clean up
         outStream.flush();
         outStream.close();
         inStream.close();
         Log.i("done", "download completed in "
         + ((System.currentTimeMillis() - startTime) / 1000)
         + " sec");

         } catch (IOException e) {
         Log.d("Error", e.getMessage(),e);
         }**/
        //You can try to accomplish great things with foolishness,
        //highly motivated he was as he wrote the code to download a file from its url
        //with lots of zeal and enthusiasm.
        //he then realized after committing the folly,
        //that he never thought of the kind and generous service offered by the DownloadManager
        //and so he laughed at himself and thus life moved on....
        Uri uri=Uri.parse(downloadURL);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("Youtube Video Downloader");
        request.setDescription("downloading youtube video...");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"youtubevideo.mp4");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // to notify when download is complete
        request.allowScanningByMediaScanner();// if you want to be available from media players
        DownloadManager manager = (DownloadManager)context.getSystemService(DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }
}


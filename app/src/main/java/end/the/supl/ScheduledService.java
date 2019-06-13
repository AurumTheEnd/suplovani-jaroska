package end.the.supl;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static end.the.supl.BaseApp.CHANNEL_NEW_PAGE_ID;

public class ScheduledService extends Service{
    private static final String LOG_TAG = "ScheduledService";
    private static final String NOTIFICATION_ID_FORMAT = "HHmmssSSS";

    private Timer timer;
    private Context mContext;
    private int idToCheck;
    private boolean restartService;

    private RequestQueue queue;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand: ");
        /*
        idToCheck = intent.getExtras().getInt("idToCheck");

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                final String url = "https://is.jaroska.cz/suplovani.php?id=" + String.valueOf(idToCheck) + "&skola=0";

                StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
                    @SuppressLint("SimpleDateFormat")
                    @Override
                    public void onResponse(String response) {

                        if(response.length() > 0){

                            Intent i = new Intent(ScheduledService.this, PageActivity.class);
                            i.putExtra("url", url);

                            TaskStackBuilder builder = TaskStackBuilder.create(ScheduledService.this).addNextIntentWithParentStack(i);
                            PendingIntent pendingIntent = builder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                            restartService = true;

                            Notification notification = new NotificationCompat.Builder(ScheduledService.this, CHANNEL_NEW_PAGE_ID)
                                    .setSmallIcon(R.drawable.ic_notification_white)
                                    .setColor(ContextCompat.getColor(ScheduledService.this, R.color.colorAccent))
                                    .setContentTitle(getString(R.string.notification_new_substitution_found_title))// + ", id = " + idToCheck)
                                    .setContentText(getString(R.string.notification_new_substitution_found_message))// + "(check done at " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ", " + cal.get(Calendar.DAY_OF_WEEK) + ". " + cal.get(Calendar.MONTH) + ". " +  + cal.get(Calendar.YEAR) + ")"
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                    .build();
                            notificationManager.notify(Integer.parseInt(new SimpleDateFormat(NOTIFICATION_ID_FORMAT).format(new Date())), notification);
                        }

                        if(restartService){
                            stopService(new Intent(ScheduledService.this, ScheduledService.class));
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });

                queue.add(stringRequest);
            }
        }, 0, 15*60*1000);
        */
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(LOG_TAG, "onTaskRemoved: ");
        /*Intent restartService = new Intent(ScheduledService.this, ScheduledService.class);
        restartService.putExtra("idToCheck", rootIntent.getExtras().getInt("idToCheck"));
        startService(restartService);*/
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate: ");
        /*

        super.onCreate();

        mContext = getApplicationContext();
        queue = Volley.newRequestQueue(ScheduledService.this);

        timer = new Timer();

        restartService = false;*/
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy: ");
        /*timer.cancel();

        super.onDestroy();

        if(restartService){
            Intent i = new Intent(ScheduledService.this, ScheduledService.class);

            i.putExtra("idToCheck", (idToCheck+1));

            ScheduledService.this.startService(i);
        }*/
    }
}

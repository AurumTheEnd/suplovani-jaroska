package end.the.supl;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
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

import static end.the.supl.BaseApp.CHANNEL_NEW_PAGE_ID;
import static end.the.supl.MainActivity.getItemFromStringPage;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "AlarmReceiver";
    private static final String NOTIFICATION_ID_FORMAT = "HHmmssSSS";
    private static final int ALARM_ID = 321;

    private int idToCheck;
    private RequestQueue queue;

    @Override
    public void onReceive(Context context, Intent intent) {
        queue = Volley.newRequestQueue(context);
        idToCheck = intent.getExtras().getInt("idToCheck");
        final String url = "https://is.jaroska.cz/suplovani.php?id=" + String.valueOf(idToCheck) + "&skola=0";

        StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onResponse(String response) {
                if(response.length() > 0){

                    ListItem item = getItemFromStringPage(response, idToCheck, url);

                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    Calendar cal = Calendar.getInstance();

                    Intent intentNotification = new Intent(context, PageActivity.class);
                    intentNotification.putExtra("url", item.getUrl());
                    intentNotification.putExtra("date", item.getDate());

                    TaskStackBuilder builder = TaskStackBuilder.create(context).addNextIntentWithParentStack(intentNotification);
                    PendingIntent pendingIntentNotification = builder.getPendingIntent(cal.get(Calendar.MILLISECOND), PendingIntent.FLAG_UPDATE_CURRENT);

                    Notification notification = new NotificationCompat.Builder(context, CHANNEL_NEW_PAGE_ID)
                            .setSmallIcon(R.drawable.ic_notification_white)
                            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                            .setContentTitle(context.getString(R.string.notification_new_substitution_found_title) + " - " + new SimpleDateFormat("EEEE d. M. yyyy").format(item.getDate()))
                            .setContentText(context.getString(R.string.notification_new_substitution_found_message))// + "(check done at " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ", " + cal.get(Calendar.DAY_OF_WEEK) + ". " + cal.get(Calendar.MONTH) + ". " +  + cal.get(Calendar.YEAR) + ")"
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntentNotification)
                            .setAutoCancel(true)
                            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                            .build();
                    notificationManager.notify(Integer.parseInt(new SimpleDateFormat(NOTIFICATION_ID_FORMAT).format(new Date())), notification);

                    Intent newIntent = new Intent(context, AlarmReceiver.class);
                    newIntent.putExtra("idToCheck", idToCheck+1);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            ALARM_ID, // id, optional
                            newIntent, // intent to launch
                            PendingIntent.FLAG_CANCEL_CURRENT);

                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 15 * 60 * 1000, pendingIntent);
                } else {
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) { }
        });

        queue.add(stringRequest);
    }
}

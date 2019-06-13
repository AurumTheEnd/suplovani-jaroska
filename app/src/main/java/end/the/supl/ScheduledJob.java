package end.the.supl;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static end.the.supl.BaseApp.CHANNEL_DEBUG_ID;
import static end.the.supl.BaseApp.CHANNEL_NEW_PAGE_ID;
import static end.the.supl.MainActivity.getItemFromStringPage;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScheduledJob extends JobService {
    private static final String LOG_TAG = "ScheduledJob";
    private static final String REQUEST_TAG = "BackgroundRequests";
    private static final String NOTIFICATION_ID_FORMAT = "HHmmssSSS";

    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {

        checkForNewPage(params);

        return true;
    }

    private void checkForNewPage(JobParameters params) {
        RequestQueue queue = Volley.newRequestQueue(this);
        final int idToCheck = (int) params.getExtras().get("idToCheck");
        final String url = "https://is.jaroska.cz/suplovani.php?id=" + String.valueOf(idToCheck) + "&skola=0";

        StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onResponse(String response) {
                if(response.length() > 0){

                    ListItem item = getItemFromStringPage(response, idToCheck, url);

                    Intent i = new Intent(ScheduledJob.this, PageActivity.class);

                    i.putExtra("url", item.getUrl());
                    i.putExtra("date", item.getDate());

                    Calendar cal = Calendar.getInstance();

                    TaskStackBuilder builder = TaskStackBuilder.create(getApplicationContext()).addNextIntentWithParentStack(i);
                    PendingIntent pendingIntent = builder.getPendingIntent(cal.get(Calendar.MILLISECOND), PendingIntent.FLAG_UPDATE_CURRENT);

                    pushNotification(
                            getString(R.string.notification_new_substitution_found_title) + " - " + new SimpleDateFormat("EEEE d. M. yyyy").format(item.getDate()),
                            getString(R.string.notification_new_substitution_found_message),
                            pendingIntent,
                            CHANNEL_NEW_PAGE_ID,
                            NotificationCompat.PRIORITY_HIGH
                    );

                    if(jobCancelled){
                        queue.cancelAll(REQUEST_TAG);
                        return;
                    }

                    PersistableBundle bundle = new PersistableBundle();
                    bundle.putInt("idToCheck", (idToCheck+1));

                    ComponentName componentName = new ComponentName(ScheduledJob.this, ScheduledJob.class);
                    JobInfo jobInfo = new JobInfo.Builder(123, componentName)
                            //.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setPeriodic(15 * 60 * 1000)
                            .setExtras(bundle)
                            .setPersisted(true)
                            .setBackoffCriteria(15 * 60 * 1000, JobInfo.BACKOFF_POLICY_LINEAR)
                            .build();

                    JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

                    jobScheduler.cancel(params.getJobId());

                    jobScheduler.schedule(jobInfo);
                } else {
                    jobFinished(params, false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(jobCancelled){
                    return;
                }

                jobFinished(params, false);
            }
        });

        if(jobCancelled){
            return;
        }

        stringRequest.setTag(REQUEST_TAG);
        queue.add(stringRequest);
    }

    private void pushNotification(String title, String message,/* Context context,*/ PendingIntent pendingIntent, String notificationChannel, int priority){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannel)
                .setSmallIcon(R.drawable.ic_notification_white)
                .setColor(ContextCompat.getColor(ScheduledJob.this, R.color.colorAccent))
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(true)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        if(pendingIntent != null){
            builder.setContentIntent(pendingIntent);
        }
        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationChannel, Integer.parseInt(new SimpleDateFormat(NOTIFICATION_ID_FORMAT).format(new Date())), notification);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        jobCancelled = true;
        return true;
    }
}

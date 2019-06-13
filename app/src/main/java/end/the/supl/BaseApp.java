package end.the.supl;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.provider.Settings;

public class BaseApp extends Application {
    public static final String CHANNEL_NEW_PAGE_ID = "ChannelNewPage";
    public static final String CHANNEL_DEBUG_ID = "ChannelDebug";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channelNewPage = new NotificationChannel(
                    CHANNEL_NEW_PAGE_ID,
                    getString(R.string.notifications_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channelNewPage.enableLights(true);
            channelNewPage.setDescription(getString(R.string.notifications_channel_description));
            channelNewPage.enableVibration(true);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channelNewPage.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes);

            NotificationChannel channelDebug = new NotificationChannel(
                    CHANNEL_DEBUG_ID,
                    "Debug notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            channelDebug.enableLights(false);
            channelDebug.setDescription("Just a debug.");
            channelDebug.enableVibration(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channelNewPage);
            notificationManager.createNotificationChannel(channelDebug);
        }
    }
    //TODO add static functions getItem and pushNotif with Context param
}

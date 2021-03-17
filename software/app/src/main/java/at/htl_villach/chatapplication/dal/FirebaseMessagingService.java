package at.htl_villach.chatapplication.dal;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.Build;

import com.google.firebase.messaging.RemoteMessage;

import at.htl_villach.chatapplication.R;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendOreoNotification(remoteMessage);
        } else {
            sendNotification(remoteMessage);
        }

    }

    private void sendOreoNotification(RemoteMessage remoteMessage) {
        String notification_title = remoteMessage.getNotification().getTitle();
        String notification_message = remoteMessage.getNotification().getBody();

        OreoNotification oreoNotification = new OreoNotification(this);
        Notification.Builder builder = oreoNotification.getOreoNotification(notification_title, notification_message);

        int mNotificationId = (int) System.currentTimeMillis();

        oreoNotification.getManager().notify(mNotificationId, builder.build());
    }

    private void sendNotification(RemoteMessage remoteMessage) {
        String notification_title = remoteMessage.getNotification().getTitle();
        String notification_message = remoteMessage.getNotification().getBody();

        Notification mBuilder = new Notification.Builder(this)
                .setContentTitle(notification_title)
                .setContentText(notification_message)
                .setSmallIcon(R.mipmap.icon_app)
                .build();

        int mNotificationId = (int) System.currentTimeMillis();

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder);
    }
}

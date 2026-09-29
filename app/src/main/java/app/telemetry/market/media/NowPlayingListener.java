package app.telemetry.market.media;

import android.app.Notification;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import app.telemetry.market.widget.TelemetryWidgetProvider;

/**
 * Reads the active media notification (Spotify and the rest) so the tape can
 * show the app icon and track title. Nothing is stored except title, artist,
 * and package name.
 */
public class NowPlayingListener extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        rescan();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (isMedia(sbn)) rescan();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (isMedia(sbn)) rescan();
    }

    private void rescan() {
        NowPlaying.Track best = null;
        long bestAt = -1;
        StatusBarNotification[] all;
        try {
            all = getActiveNotifications();
        } catch (Throwable t) {
            return;
        }
        if (all != null) {
            for (StatusBarNotification sbn : all) {
                NowPlaying.Track t = read(sbn);
                if (t == null || !t.playing) continue;
                if (sbn.getPostTime() >= bestAt) {
                    best = t;
                    bestAt = sbn.getPostTime();
                }
            }
        }
        if (NowPlaying.unchanged(this, best)) return;
        NowPlaying.save(this, best);
        TelemetryWidgetProvider.redraw(this);
    }

    private static boolean isMedia(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return false;
        Notification n = sbn.getNotification();
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return false;
        if (Notification.CATEGORY_TRANSPORT.equals(n.category)) return true;
        Bundle extras = n.extras;
        return extras != null && extras.containsKey(Notification.EXTRA_MEDIA_SESSION);
    }

    private NowPlaying.Track read(StatusBarNotification sbn) {
        if (!isMedia(sbn)) return null;
        Notification n = sbn.getNotification();
        Bundle extras = n.extras == null ? Bundle.EMPTY : n.extras;
        String title = text(extras.getCharSequence(Notification.EXTRA_TITLE));
        String artist = text(extras.getCharSequence(Notification.EXTRA_TEXT));
        boolean playing = false;
        MediaSession.Token token = token(extras);
        if (token != null) {
            try {
                MediaController mc = new MediaController(this, token);
                MediaMetadata md = mc.getMetadata();
                if (md != null) {
                    String t = md.getString(MediaMetadata.METADATA_KEY_TITLE);
                    String a = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
                    if (t != null && !t.isEmpty()) title = t;
                    if (a != null && !a.isEmpty()) artist = a;
                }
                PlaybackState st = mc.getPlaybackState();
                playing = st != null && st.getState() == PlaybackState.STATE_PLAYING;
            } catch (Throwable ignored) {
            }
        }
        if (title.isEmpty()) return null;
        if (artist.equalsIgnoreCase(title)) artist = "";
        return new NowPlaying.Track(title, artist, sbn.getPackageName(), playing);
    }

    private static String text(CharSequence s) {
        return s == null ? "" : s.toString().trim();
    }

    @SuppressWarnings("deprecation")
    private static MediaSession.Token token(Bundle extras) {
        if (extras == null || !extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return null;
        if (Build.VERSION.SDK_INT >= 33) {
            return extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token.class);
        }
        return extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
    }
}

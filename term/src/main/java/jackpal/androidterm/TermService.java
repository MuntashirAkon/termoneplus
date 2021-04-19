/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2018-2020 Roumen Petrov.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jackpal.androidterm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.termoneplus.Application;
import com.termoneplus.R;
import com.termoneplus.TermActivity;
import com.termoneplus.services.CommandService;

import java.util.UUID;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.libtermexec.v1.ITerminal;
import jackpal.androidterm.util.SessionList;
import jackpal.androidterm.util.TermSettings;

public class TermService extends Service {
    private static final int RUNNING_NOTIFICATION = 1;

    private final IBinder mTSBinder = new TSBinder();
    private final SessionList mTermSessions = new SessionList();
    private CommandService command_service;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (TermExec.SERVICE_ACTION_V1.equals(intent.getAction())) {
            Log.i("TermService", "Outside process called onBind()");

            return new RBinder();
        } else {
            Log.i("TermService", "Activity called onBind()");

            return mTSBinder;
        }
    }

    @Override
    public void onCreate() {
        /* Put the service in the foreground. */
        Notification notification = buildNotification();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        startForeground(RUNNING_NOTIFICATION, notification);

        command_service = new CommandService(this);
        command_service.start();

        Log.d(Application.APP_TAG, "TermService started");
    }

    @Override
    public void onDestroy() {
        command_service.stop();

        for (TermSession session : mTermSessions) {
            /* Don't automatically remove from list of sessions -- we clear the
             * list below anyway and we could trigger
             * ConcurrentModificationException if we do */
            session.setFinishCallback(null);
            session.finish();
        }
        mTermSessions.clear();
        stopForeground(true);
    }

    public int getSessionCount() {
        return mTermSessions.size();
    }

    public TermSession getSession(int index) {
        try {
            return mTermSessions.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public SessionList getSessions() {
        return mTermSessions;
    }

    public void addSession(TermSession session) {
        addSession(session, this::onSessionFinish);
    }

    private void addSession(TermSession session, TermSession.FinishCallback callback) {
        mTermSessions.add(session);
        session.setFinishCallback(callback);
    }

    private void onSessionFinish(TermSession session) {
        mTermSessions.remove(session);
    }

    private Notification buildNotification() {
        NotificationChannelCompat.create(this);

        Intent notifyIntent = new Intent(this, TermActivity.class);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                Application.NOTIFICATION_CHANNEL_SESSIONS)
                .setSmallIcon(R.drawable.ic_stat_service_notification_icon)
                .setContentTitle(getText(R.string.application_terminal))
                .setContentText(getText(R.string.service_notify_text))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setTicker(getText(R.string.service_notify_text))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);
        return builder.build();
    }


    private static class NotificationChannelCompat {
        private static void create(TermService service) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O /*API Level 26*/) return;
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            Compat26.create(service);
        }

        @RequiresApi(26)
        private static class Compat26 {
            private static void create(TermService service) {
                NotificationChannel channel = new NotificationChannel(
                        Application.NOTIFICATION_CHANNEL_SESSIONS,
                        "TermOnePlus",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("TermOnePlus running notification");
                channel.setShowBadge(false);

                // Register the channel with the system ...
                // Note we can't change the importance or other notification behaviors after this.
                NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }


    public class TSBinder extends Binder {
        public TermService getService() {
            Log.i("TermService", "Activity binding to service");
            return TermService.this;
        }
    }

    private final class RBinder extends ITerminal.Stub {
        @Override
        public IntentSender startSession(final ParcelFileDescriptor pseudoTerminalMultiplexerFd,
                                         final ResultReceiver callback) {
            final String sessionHandle = UUID.randomUUID().toString();

            // distinct Intent Uri and PendingIntent requestCode must be sufficient to avoid collisions
            final Intent switchIntent = new Intent()
                    .setClassName(Application.ID, Term.class.getName())
                    .setAction(Application.ACTION_OPEN_NEW_WINDOW)
                    .setData(Uri.parse(sessionHandle))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(Application.ARGUMENT_TARGET_WINDOW, sessionHandle);

            final PendingIntent result = PendingIntent.getActivity(getApplicationContext(), sessionHandle.hashCode(),
                    switchIntent, 0);

            final PackageManager pm = getPackageManager();
            final String[] pkgs = pm.getPackagesForUid(getCallingUid());
            if (pkgs == null || pkgs.length == 0)
                return null;

            for (String packageName : pkgs) {
                try {
                    final PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);

                    final ApplicationInfo appInfo = pkgInfo.applicationInfo;
                    if (appInfo == null)
                        continue;

                    final CharSequence label = pm.getApplicationLabel(appInfo);

                    if (!TextUtils.isEmpty(label)) {
                        final String niceName = label.toString();

                        new Handler(Looper.getMainLooper()).post(() -> {
                            GenericTermSession session = null;
                            try {
                                final TermSettings settings = new TermSettings(getApplicationContext());

                                session = new BoundSession(pseudoTerminalMultiplexerFd, settings, niceName);
                                session.setHandle(sessionHandle);
                                session.setTitle("");
                                session.initializeEmulator(80, 24);

                                addSession(session, new RBinderCleanupCallback(result, callback));
                            } catch (Exception whatWentWrong) {
                                Log.e("TermService", "Failed to bootstrap AIDL session: "
                                        + whatWentWrong.getMessage());

                                if (session != null)
                                    session.finish();
                            }
                        });

                        return result.getIntentSender();
                    }
                } catch (PackageManager.NameNotFoundException ignore) {
                }
            }

            return null;
        }
    }

    private final class RBinderCleanupCallback implements TermSession.FinishCallback {
        private final PendingIntent result;
        private final ResultReceiver callback;

        public RBinderCleanupCallback(PendingIntent result, ResultReceiver callback) {
            this.result = result;
            this.callback = callback;
        }

        @Override
        public void onSessionFinish(TermSession session) {
            result.cancel();

            callback.send(0, new Bundle());

            mTermSessions.remove(session);
        }
    }
}

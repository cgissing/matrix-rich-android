/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import im.vector.app.R
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import timber.log.Timber

/** SC wrapper around [startForegroundCompatUpstream] which catches [ForegroundServiceStartNotAllowedException].
 * Come on Element, don't you care about these crashes spamming your rageshake inbox from FDroid users?
 * */
fun Service.startForegroundCompat(
        id: Int,
        notification: Notification,
        provideForegroundServiceType: (() -> Int)? = null,
        errorNotificationTitle: String = getString(CommonStrings.notification_foreground_service_failed_title),
        errorNotificationSummary: String = getString(CommonStrings.notification_foreground_service_failed_summary),
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            startForegroundCompatUpstream(id, notification, provideForegroundServiceType)
            Timber.tag("SchildiService").d("Started foreground service $id successfully on >= S")
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Timber.tag("SchildiService").e(e, "Failed to start foreground service")
            val notificationManager = NotificationManagerCompat.from(this)
            val errorNotification = NotificationCompat.Builder(this, NotificationUtils.SC_APP_ERRORS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_status_bar_sc)
                    .setContentTitle(errorNotificationTitle)
                    .setContentText(errorNotificationSummary)
                    .setColor(ThemeUtils.getColor(this, android.R.attr.colorPrimary))
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .build()
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Timber.tag("SchildiService").w("Not allowed to notify.")
            } else {
                notificationManager.notify("FAILED_FG_SERVICE_TAG", id, errorNotification)
            }
        }
    } else {
        startForegroundCompatUpstream(id, notification, provideForegroundServiceType)
        Timber.tag("SchildiService").d("Started foreground service $id successfully on < S")
    }
}

fun Service.startForegroundCompatUpstream(
        id: Int,
        notification: Notification,
        provideForegroundServiceType: (() -> Int)? = null
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
                id,
                notification,
                provideForegroundServiceType?.invoke() ?: ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )
    } else {
        startForeground(id, notification)
    }
}

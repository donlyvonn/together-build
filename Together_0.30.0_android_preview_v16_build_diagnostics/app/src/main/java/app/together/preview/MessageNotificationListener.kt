package app.together.preview

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.util.AtomicFile

/** Optional notification capture. This is NOT a complete SMS/RCS/iMessage history API. */
class MessageNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName !in setOf("com.google.android.apps.messaging", "com.bluebubbles.messaging.alpha")) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isBlank() || body.isBlank()) return
        // Only collect message-category notifications. Other alerts from these apps
        // (backup errors, account prompts, etc.) are not conversation messages.
        if (sbn.notification.category != Notification.CATEGORY_MESSAGE) return
        val record = JSONObject().put("package", sbn.packageName).put("title", title)
            .put("body", body).put("timestamp", sbn.postTime).put("notificationKey", sbn.key)
        val file = File(filesDir, "notification_messages.json")
        synchronized(MessageNotificationListener::class.java) {
            // Do not overwrite potentially recoverable history if it cannot be parsed.
            val records = if (!file.exists()) JSONArray() else try {
                JSONArray(file.readText())
            } catch (_: Exception) {
                android.util.Log.e("TogetherPreview", "Notification history unreadable; refusing overwrite")
                return
            }
            // A notification can be updated repeatedly. Retain one snapshot per
            // notification key and text; preserve distinct updates as separate entries.
            for (i in 0 until records.length()) {
                val existing = records.optJSONObject(i) ?: continue
                if (existing.optString("notificationKey") == sbn.key &&
                    existing.optString("body") == body &&
                    existing.optString("title") == title) return
            }
            records.put(record)
            while (records.length() > 250) records.remove(0)
            val atomic = AtomicFile(file)
            val stream = atomic.startWrite()
            try {
                stream.write(records.toString().toByteArray(Charsets.UTF_8))
                atomic.finishWrite(stream)
            } catch (e: Exception) {
                atomic.failWrite(stream)
                throw e
            }
        }
    }
}

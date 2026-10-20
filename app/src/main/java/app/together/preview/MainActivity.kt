package app.together.preview

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import app.together.core.*
import java.io.File
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import org.json.JSONArray

/** Offline interaction prototype: no messaging permissions, network or live message access. */
class MainActivity : Activity() {
    private val exportRequestCode = 7030
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != exportRequestCode || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        val file = File(filesDir, "notification_messages.json")
        if (!file.exists()) {
            Toast.makeText(this, "No captured history to export", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not open destination")
            Toast.makeText(this, "History exported", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed; original data unchanged", Toast.LENGTH_LONG).show()
        }
    }

    private val google = SourceThread(Source.GOOGLE_MESSAGES, "squish-google-demo", setOf("demo-contact"))
    private val bubble = SourceThread(Source.OPENBUBBLES, "squish-openbubbles-demo", setOf("demo-contact"))
    private lateinit var store: LinkStorage
    private lateinit var links: ThreadLinks
    private lateinit var status: TextView
    private lateinit var timeline: LinearLayout
    private val messages = listOf(
        Message(Source.GOOGLE_MESSAGES, google.threadId, "g-1", "Me", 1000L, "See you soon!", Delivery.SENT, localId="g-1"),
        Message(Source.OPENBUBBLES, bubble.threadId, "b-1", "Squish", 2000L, "Yeah that works!! We miss you!", Delivery.RECEIVED, localId="b-1")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LinkStorage(File(filesDir, "preview_links.txt"))
        links = try { store.load() } catch (e: Exception) { ThreadLinks() }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 44, 30, 20); setBackgroundColor(Color.rgb(246,240,248)) }
        val heading = TextView(this).apply { text="Together · 0.30 preview"; textSize=25f; setTextColor(Color.rgb(70,38,80)) }
        root.addView(heading)
        root.addView(TextView(this).apply { text="OFFLINE DEMO — not connected to Google Messages or OpenBubbles"; textSize=13f; setPadding(0,12,0,20) })
        status = TextView(this).apply { textSize=17f; setPadding(0,8,0,12) }; root.addView(status)
        val linkButton = Button(this).apply { text="Link / unlink Squish demo threads"; setOnClickListener { if (links.members("squish").isEmpty()) links.link("squish", listOf(google,bubble), true) else links.unlink("squish"); store.save(links); render() } }
        root.addView(linkButton)
        root.addView(Button(this).apply { text="Open Madrid integration status"; setOnClickListener {
            val caps = OpenMadridExperimental.documentedCapabilities
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Open Madrid · Experimental")
                .setMessage("Registration verified (upstream): ${caps.registrationVerified}\n" +
                    "Live sending verified: ${caps.liveSendVerified}\n" +
                    "Live receiving verified: ${caps.liveReceiveVerified}\n" +
                    "Attachments verified: ${caps.attachmentAccessVerified}\n\n" +
                    "Direct sending is disabled. This preview does not connect to Apple or Open Madrid.")
                .setPositiveButton("OK", null).show()
        } })
        root.addView(Button(this).apply { text="Grant optional notification access"; setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } })
        root.addView(Button(this).apply { text="View captured notifications"; setOnClickListener { showNotifications() } })
        root.addView(Button(this).apply { text="Show combined captured messages"; setOnClickListener { showCombinedCaptured() } })
        root.addView(Button(this).apply { text="Export captured notifications (JSON)"; setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "together-notification-backup.json")
            }
            startActivityForResult(intent, exportRequestCode)
        } })
        root.addView(Button(this).apply { text="Delete captured notifications"; setOnClickListener {
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Delete captured notifications?")
                .setMessage("This permanently deletes notification copies saved by the preview. It does not delete messages in Google Messages or OpenBubbles.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    val file = File(filesDir, "notification_messages.json")
                    val deleted = !file.exists() || file.delete()
                    Toast.makeText(this@MainActivity, if (deleted) "Captured notifications deleted" else "Could not delete history", Toast.LENGTH_SHORT).show()
                    if (deleted) render()
                }.show()
        } })
        timeline = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val scroller = ScrollView(this).apply { isFillViewport = true; addView(timeline) }
        root.addView(scroller, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        setContentView(root); render()
    }
    private fun showNotifications() {
        timeline.removeAllViews()
        val file = File(filesDir, "notification_messages.json")
        val items = try { JSONArray(file.readText()) } catch (_: Exception) { JSONArray() }
        if (items.length() == 0) timeline.addView(TextView(this).apply { text="No notifications captured. Grant access, then receive a new message." })
        for (i in items.length() - 1 downTo 0) {
            val item = items.optJSONObject(i) ?: continue
            timeline.addView(TextView(this).apply {
                text="${item.optString("package")} · ${item.optString("title")}\n${item.optString("body")}" 
                textSize=15f; setTextColor(Color.rgb(45,31,50)); setPadding(12,14,12,14)
            })
        }
    }

    /** Best-effort display of captured notifications, NOT a complete messaging history. */
    private fun showCombinedCaptured() {
        timeline.removeAllViews()
        val file = File(filesDir, "notification_messages.json")
        val items = try { JSONArray(file.readText()) } catch (_: Exception) { JSONArray() }
        val grouped = mutableListOf<Triple<Long, String, String>>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val pkg = item.optString("package")
            val service = when (pkg) {
                "com.google.android.apps.messaging" -> "Google Messages"
                "com.bluebubbles.messaging.alpha" -> "OpenBubbles"
                else -> continue
            }
            val title = item.optString("title")
            val body = item.optString("body")
            if (body.isNotBlank()) grouped.add(Triple(item.optLong("timestamp"), service, "$title\n$body"))
        }
        if (grouped.isEmpty()) timeline.addView(TextView(this).apply { text="No captured messages yet. Notification access is optional and does not provide complete history." })
        grouped.sortedBy { it.first }.forEach { (_, service, content) ->
            timeline.addView(TextView(this).apply {
                text="$service\n$content"
                textSize=16f
                setTextColor(Color.rgb(45,31,50))
                setPadding(12,16,12,16)
            })
        }
    }

    private fun render() {
        val linked = links.members("squish").isNotEmpty()
        status.text = if (linked) "Squish · linked (2 services)" else "Squish · separate conversations"
        timeline.removeAllViews()
        val shown = if (linked) UnifiedTimeline.merge(listOf(google,bubble),messages) else UnifiedTimeline.merge(listOf(google),messages)
        shown.forEach { m -> timeline.addView(TextView(this).apply { text="${m.source.name} · ${m.sender}\n${m.text}"; textSize=17f; setTextColor(Color.rgb(45,31,50)); setPadding(12,20,12,20) }) }
        if (!linked) timeline.addView(TextView(this).apply { text="OpenBubbles demo thread is separate until linked."; setPadding(12,16,12,16) })
    }
}

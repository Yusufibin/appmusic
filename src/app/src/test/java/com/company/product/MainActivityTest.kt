package com.company.product

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34], qualifiers = "w393dp-h851dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityTest {
    private lateinit var controller: ActivityController<MainActivity>
    private lateinit var activity: MainActivity

    @Before fun create() {
        SessionState.mutable.value = ConnectionState()
        RuntimeEnvironment.getApplication().getSharedPreferences("connection_v2", Context.MODE_PRIVATE).edit().clear().commit()
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        activity = controller.get()
        // The empty store is read on Dispatchers.IO; let its completion return to the main looper.
        val button = activity.findViewById<MaterialButton>(R.id.btnSave)
        repeat(100) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            if (button.isEnabled) return
            Thread.sleep(5)
        }
        assertTrue("Configuration initialization should complete", button.isEnabled)
    }

    @After fun destroy() {
        controller.pause().stop().destroy()
        SessionState.mutable.value = ConnectionState()
        UiLogger.clear()
    }

    @Test fun `launch is offline and does not start a service`() {
        assertEquals(activity.getString(R.string.status_stopped), activity.findViewById<TextView>(R.id.tvStatus).text)
        assertNull(shadowOf(activity).nextStartedService)
    }
    @Test fun `invalid setup shows field errors without connecting`() {
        activity.findViewById<MaterialButton>(R.id.btnSession).performClick()
        assertNotNull(activity.findViewById<TextInputLayout>(R.id.tokenLayout).error)
        assertNotNull(activity.findViewById<TextInputLayout>(R.id.ownerLayout).error)
        assertNull(shadowOf(activity).nextStartedService)
    }
    @Test fun `active session locks credentials but keeps stop available`() {
        SessionState.mutable.value = ConnectionState(ConnectionPhase.CONNECTED, "demo_bot")
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(activity.findViewById<TextInputLayout>(R.id.tokenLayout).isEnabled)
        assertTrue(activity.findViewById<MaterialButton>(R.id.btnSession).isEnabled)
        assertEquals(activity.getString(R.string.stop), activity.findViewById<MaterialButton>(R.id.btnSession).text)
    }
    @Test fun `connection errors restore editing and show the problem`() {
        SessionState.mutable.value = ConnectionState(ConnectionPhase.ERROR, problem = "Jeton révoqué")
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.findViewById<TextInputLayout>(R.id.tokenLayout).isEnabled)
        assertEquals("Jeton révoqué", activity.findViewById<TextView>(R.id.tvSessionDetail).text)
    }
    @Test fun `journal can be cleared from the screen`() {
        UiLogger.log("TEST", "Visible event")
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.findViewById<TextView>(R.id.tvLog).text.contains("Visible event"))
        activity.findViewById<MaterialButton>(R.id.btnClear).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(activity.getString(R.string.empty_journal), activity.findViewById<TextView>(R.id.tvLog).text.toString())
    }
    @Test
    @Config(sdk = [34])
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `dashboard renders at phone dimensions`() {
        render("dashboard-light.png")
    }
    @Test
    @Config(sdk = [34], qualifiers = "+night")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `dashboard renders in dark mode`() {
        render("dashboard-dark.png")
    }

    private fun render(filename: String) {
        activity.findViewById<android.widget.EditText>(R.id.etDeviceName).setText("Mon Pixel")
        val content = activity.findViewById<android.view.ViewGroup>(R.id.page).getChildAt(0)
        content.measure(View.MeasureSpec.makeMeasureSpec(786, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        content.layout(0, 0, content.measuredWidth, content.measuredHeight)
        val bitmap = Bitmap.createBitmap(content.measuredWidth, content.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(activity.getColor(R.color.canvas))
        content.draw(canvas)
        val file = File("build/reports/previews", filename).apply { requireNotNull(parentFile).mkdirs() }
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        assertTrue(file.length() > 10_000)
    }
}

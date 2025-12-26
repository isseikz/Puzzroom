package tokyo.isseikuzumaki.vibeterminal.service

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.LayoutInflater
import tokyo.isseikuzumaki.vibeterminal.R
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Presentation for secondary display.
 *
 * This displays a simple red screen on the secondary display
 * as a proof-of-concept for persistent display functionality.
 *
 * The presentation uses TYPE_APPLICATION_OVERLAY window type to remain
 * visible even when the user switches to other apps on the main display.
 */
class TerminalPresentation(
    outerContext: Context,
    display: Display
) : Presentation(outerContext, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("TerminalPresentation onCreate")

        // Inflate layout for the presentation
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.presentation_terminal_test, null)
        setContentView(view)

        Logger.d("Presentation content view set")
    }

    override fun onStart() {
        super.onStart()
        Logger.d("TerminalPresentation onStart")
    }

    override fun onStop() {
        super.onStop()
        Logger.d("TerminalPresentation onStop")
    }
}

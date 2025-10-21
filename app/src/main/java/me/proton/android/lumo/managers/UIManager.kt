package me.proton.android.lumo.managers

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.toColorInt

private const val TAG = "UIManager"

/**
 * Manager class that handles UI-related operations including status bar configuration,
 * edge-to-edge display, and other UI state management. Separates UI concerns from MainActivity.
 */
class UIManager(private val activity: Activity) {

    /**
     * Initialize UI with edge-to-edge display and proper status bar configuration
     */
    fun initializeUI() {
        Log.d(TAG, "Initializing UI with edge-to-edge display")

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Configure status bar
        configureStatusBar()
    }

    /**
     * Enable edge-to-edge display with automatic status bar icon management
     */
    private fun enableEdgeToEdge() {
        try {
            Log.d(TAG, "Enabling edge-to-edge display")

            // Modern approach using WindowCompat
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)

            Log.d(TAG, "Edge-to-edge display enabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling edge-to-edge display", e)
        }
    }

    /**
     * Configure status bar for optimal visibility
     */
    fun configureStatusBar() {
        try {
            Log.d(TAG, "Configuring status bar for visibility")

            // Use modern WindowInsetsController for all supported versions
            val windowInsetsController =
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)

            // Ensure status bar is visible
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())

            // Force dark icons on light background for better visibility
            windowInsetsController.isAppearanceLightStatusBars = true

            activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.statusBarColor = "#F5F5F5".toColorInt() // Light gray

            Log.d(TAG, "Status bar configured with light background and dark icons")

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring status bar", e)
        }
    }

    /**
     * Handle configuration changes (orientation, theme, etc.)
     */
    fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "Configuration changed - reconfiguring UI")

        // Reconfigure status bar on configuration changes
        configureStatusBar()

        // Log configuration details for debugging
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> Log.d(TAG, "Orientation: Portrait")
            Configuration.ORIENTATION_LANDSCAPE -> Log.d(TAG, "Orientation: Landscape")
            else -> Log.d(TAG, "Orientation: Unknown")
        }

        when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> Log.d(TAG, "Theme: Dark mode")
            Configuration.UI_MODE_NIGHT_NO -> Log.d(TAG, "Theme: Light mode")
            else -> Log.d(TAG, "Theme: Unknown")
        }
    }

    /**
     * Handle activity resume - ensure UI state is correct
     */
    fun onResume() {
        Log.d(TAG, "Activity resumed - ensuring status bar visibility")
        configureStatusBar()
    }

    /**
     * Set status bar color
     */
    fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor = color
            Log.d(TAG, "Status bar color set to: ${Integer.toHexString(color)}")
        }
    }

    /**
     * Set navigation bar color
     */
    fun setNavigationBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.navigationBarColor = color
            Log.d(TAG, "Navigation bar color set to: ${Integer.toHexString(color)}")
        }
    }

    /**
     * Toggle light/dark status bar icons
     */
    fun setStatusBarIconsLight(light: Boolean) {
        try {
            val windowInsetsController =
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = light
            Log.d(TAG, "Status bar icons set to: ${if (light) "light" else "dark"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting status bar icon color", e)
        }
    }

    /**
     * Toggle light/dark navigation bar icons
     */
    fun setNavigationBarIconsLight(light: Boolean) {
        try {
            val windowInsetsController =
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            windowInsetsController.isAppearanceLightNavigationBars = light
            Log.d(TAG, "Navigation bar icons set to: ${if (light) "light" else "dark"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting navigation bar icon color", e)
        }
    }

    /**
     * Hide status bar
     */
    fun hideStatusBar() {
        try {
            val windowInsetsController =
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
            Log.d(TAG, "Status bar hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding status bar", e)
        }
    }

    /**
     * Show status bar
     */
    fun showStatusBar() {
        try {
            val windowInsetsController =
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
            Log.d(TAG, "Status bar shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing status bar", e)
        }
    }

    /**
     * Set window flags
     */
    fun setWindowFlags(flags: Int, mask: Int) {
        activity.window.setFlags(flags, mask)
        Log.d(TAG, "Window flags set: flags=$flags, mask=$mask")
    }

    /**
     * Clear window flags
     */
    fun clearWindowFlags(flags: Int) {
        activity.window.clearFlags(flags)
        Log.d(TAG, "Window flags cleared: $flags")
    }
}

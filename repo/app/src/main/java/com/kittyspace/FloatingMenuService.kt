package com.kittyspace

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kittyspace.ui.theme.MyApplicationTheme

class FloatingMenuService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: ComposeView
    
    private var params: WindowManager.LayoutParams? = null
    private var appName: String = "UNKNOWN GAME"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appName = intent?.getStringExtra("APP_NAME") ?: "UNKNOWN GAME"
        
        if (!::windowManager.isInitialized) {
            setupFloatingWindow()
        }
        
        return START_NOT_STICKY
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
        
        floatingView = ComposeView(this).apply {
            setContent {
                MyApplicationTheme {
                    FloatingMenuOverlay(
                     appName = appName,
                    onClose = { stopSelf() },
                    onDrag = { dx, dy ->
                    moveMenu(dx, dy)
                  
                     
                     })
                                        
                }
            }
        }

        // Required setup for Compose in a WindowManager view
        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        floatingView.setViewTreeLifecycleOwner(lifecycleOwner)
        floatingView.setViewTreeViewModelStoreOwner(lifecycleOwner)
        floatingView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        
        windowManager.addView(floatingView, params)
    }
         private fun moveMenu(dx: Float, dy: Float) {
    params?.let {
        it.x += dx.toInt()
        it.y += dy.toInt()
        windowManager.updateViewLayout(floatingView, it)
    }
}
    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}

@Composable
@Composable
fun FloatingMenuOverlay(
    appName: String,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    var isMinimized by remember { mutableStateOf(true) }
    
    if (isMinimized) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFB388FF))
                .clickable { isMinimized = false },
            contentAlignment = Alignment.Center
        ) {
            GlowPawPrint(Modifier.size(32.dp))
        }
    } else {
        FloatingMenuContent(
         appName = appName,
         onCloseMenu = onClose,
          onMinimizeMenu = { isMinimized = true },
         onDrag = onDrag
       )
    }
}

// Minimal implementation of LifecycleOwner, ViewModelStoreOwner and SavedStateRegistryOwner for Compose
private class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: androidx.savedstate.SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    fun performRestore(savedState: android.os.Bundle?) = savedStateRegistryController.performRestore(savedState)
}

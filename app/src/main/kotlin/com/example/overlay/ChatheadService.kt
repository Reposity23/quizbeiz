package com.example.overlay

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.net.HttpURLConnection
import java.net.URL

class ChatheadService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var chatHead: ImageView
    private lateinit var messageView: TextView
    private lateinit var mediaProjection: MediaProjection
    private var lastTapTime: Long = 0
    private val openAIService = OpenAIService()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
        
        setupChathead()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<Intent>("screenCaptureIntent")?.let { screenCaptureIntent ->
            val mediaProjectionManager = 
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(
                Activity.RESULT_OK,
                screenCaptureIntent
            )
        }
        return START_STICKY
    }

    private fun setupChathead() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        chatHead = ImageView(this).apply {
            loadImageFromUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Facebook_Messenger_logo_2020.svg/1200px-Facebook_Messenger_logo_2020.svg.png")
        }
        
        messageView = TextView(this).apply {
            visibility = View.GONE
            setBackgroundResource(R.drawable.message_background)
            setTextColor(resources.getColor(android.R.color.white, null))
            setPadding(16, 8, 16, 8)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        setupTouchListener(params)
        windowManager.addView(chatHead, params)
    }

    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        var initialX: Int = 0
        var initialY: Int = 0
        var initialTouchX: Float = 0f
        var initialTouchY: Float = 0f

        chatHead.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < 300) {
                        startScreenCapture()
                    }
                    lastTapTime = currentTime
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(chatHead, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun startScreenCapture() {
        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        // Capture image after a short delay to ensure display is ready
        Handler(Looper.getMainLooper()).postDelayed({
            captureScreen()
        }, 100)
    }

    private fun captureScreen() {
        val image = imageReader?.acquireLatestImage()
        image?.use { img ->
            val planes = img.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * img.width
            
            val bitmap = Bitmap.createBitmap(
                img.width + rowPadding / pixelStride,
                img.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Perform OCR on the bitmap
            performOCR(bitmap)
        }
        
        virtualDisplay?.release()
        imageReader?.close()
    }

    private fun performOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                if (extractedText.isNotEmpty()) {
                    scope.launch {
                        val response = openAIService.sendQuery(extractedText)
                        showMessageBubble(response)
                    }
                }
            }
            .addOnFailureListener { e ->
                showMessage("OCR failed: ${e.message}")
            }
    }

    private fun showMessageBubble(message: String) {
        messageView.text = message
        messageView.visibility = View.VISIBLE
        
        // Hide message after 7 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            messageView.visibility = View.GONE
        }, 7000)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Chathead Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Active")
            .setContentText("Tap to return to app")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(chatHead)
        mediaProjection.stop()
    }

    private fun loadImageFromUrl(url: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                
                val input = connection.inputStream
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                
                withContext(Dispatchers.Main) {
                    chatHead.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // If loading fails, retry after 1 second
                delay(1000)
                loadImageFromUrl(url)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "ChatheadServiceChannel"
        private const val NOTIFICATION_ID = 1
    }
}
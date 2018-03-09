package me.t.networkspeed

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.annotation.RequiresApi
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by lai
 * on 2018/1/23.
 */

class NetworkSpeedService : Service() {

    private var mLastTotalRxBytes: Long = 0
    private var mLastTimeStamp: Long = 0
    private var mLastSpeed: Long = 0
    private var mDelay: Long = 2000

    private lateinit var mTimer: Timer
    private lateinit var mTask: NetWorkSpeedTask
    private lateinit var mScreenOnReceiver: ScreenOnBroadcastReceiver

    private var mNotificationManager: NotificationManager? = null
    private lateinit var mNotificationPendingIntent: PendingIntent

    @SuppressLint("HandlerLeak")
    private val mHandler = object : Handler() {

        override fun handleMessage(msg: Message) {
            val targets = msg.obj as Array<*>
            val speed = targets[0] as String
            val unit = targets[1] as String
            createNotification(speed, unit)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("NetworkSpeedService not support bindService")
    }

    override fun onCreate() {
        super.onCreate()
        mLastTotalRxBytes = getTotalRxBytes()
        mLastTimeStamp = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        mNotificationPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        mDelay = intent.getLongExtra("delay", 2000)
        mTimer = Timer()
        mTask = NetWorkSpeedTask(this)
        mTimer.schedule(mTask, 0, mDelay)

        mScreenOnReceiver = ScreenOnBroadcastReceiver(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(mScreenOnReceiver, intentFilter)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        unregisterReceiver(mScreenOnReceiver)
        stopForeground(true)
        mHandler.removeCallbacksAndMessages(null)
        mTask.cancel()
        mTimer.cancel()
        super.onDestroy()
    }

    private fun getTotalRxBytes(): Long {
        return if (TrafficStats.getUidRxBytes(applicationInfo.uid) ==
                TrafficStats.UNSUPPORTED.toLong()) 0
        else TrafficStats.getTotalRxBytes()
    }

    private fun showNetSpeed() {
        val nowTotalRxBytes = getTotalRxBytes()
        val nowTimeStamp = System.currentTimeMillis()
        val deltaBytes = nowTotalRxBytes - mLastTotalRxBytes
        val deltaTime = nowTimeStamp - mLastTimeStamp
        val speed = deltaBytes * 1000 / deltaTime

        val msg = mHandler.obtainMessage()
        msg.what = 100
        val targets = arrayOfNulls<String>(2)
        when {
            speed >= 1000 * 1000 -> {
                var speedOfM = speed / 1024F / 1024F
                speedOfM = Math.round(speedOfM * 10).toFloat() / 10
                targets[0] = speedOfM.toString()
                targets[1] = "M/s"
            }
            speed >= 1000 -> {
                val speedOfK = speed / 1024
                targets[0] = speedOfK.toString()
                targets[1] = "K/s"
            }
            else -> {
                targets[0] = speed.toString()
                targets[1] = "B/s"
            }
        }
        msg.obj = targets
        mHandler.sendMessage(msg)

        mLastSpeed = speed
        mLastTimeStamp = nowTimeStamp
        mLastTotalRxBytes = nowTotalRxBytes
    }

    private fun drawNumberBitmap(speed: String, unit: String): Bitmap {
        val scale = resources.displayMetrics.density
        val px = (24f * scale + 0.5f).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_4444)

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.isFakeBoldText = true

        val textWidth = paint.measureText(speed).toInt()
        val textHeight = 18
        val x = (bitmap.width - textWidth) / 2
        canvas.drawText(speed, x.toFloat(), textHeight.toFloat(), paint)

        val width = paint.measureText(unit).toInt()
        val x1 = (bitmap.width - width) / 2
        canvas.drawText(unit, x1.toFloat(), bitmap.height.toFloat(), paint)

        return bitmap
    }

    private fun createNotification(speed: String, unit: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannelId(speed, unit)
            return
        }

        val bitmap = drawNumberBitmap(speed, unit)
        val builder = Notification.Builder(this)
                .setOngoing(true)
                .setAutoCancel(false)
                .setSmallIcon(Icon.createWithBitmap(bitmap))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setContentText("当前网速: $speed$unit")
                .setPriority(Notification.PRIORITY_MIN)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(mNotificationPendingIntent)
        startForeground(1001, builder.build())
        bitmap.recycle()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelId(speed: String, unit: String) {
        val id = "me.t.networkspeed.notification_id"
        val name = "NetworkSpeed"
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_MIN)
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        if (mNotificationManager == null) {
            mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        mNotificationManager?.createNotificationChannel(channel)

        val bitmap = drawNumberBitmap(speed, unit)
        val notificationBuilder = Notification.Builder(this, id)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setSmallIcon(Icon.createWithAdaptiveBitmap(drawNumberBitmap(speed, unit)))
                .setContentIntent(mNotificationPendingIntent)
                .setWhen(System.currentTimeMillis())
        startForeground(1001, notificationBuilder.build())
        bitmap.recycle()
    }

    private fun startTimer() {
        stopTimer()
        mTimer = Timer()
        mTask = NetWorkSpeedTask(this)
        mTimer.schedule(mTask, 0, mDelay)
    }

    private fun stopTimer() {
        mTask.cancel()
        mTimer.cancel()
    }

    internal class NetWorkSpeedTask(service: NetworkSpeedService) : TimerTask() {
        private val mWeakReference: WeakReference<NetworkSpeedService> = WeakReference(service)
        override fun run() {
            val service = mWeakReference.get()
            service?.let { service.showNetSpeed() }
        }
    }

    internal class ScreenOnBroadcastReceiver(service: NetworkSpeedService) : BroadcastReceiver() {
        private val mWeakReference: WeakReference<NetworkSpeedService> = WeakReference(service)
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val service = mWeakReference.get()
            when (action) {
                Intent.ACTION_SCREEN_ON -> service?.startTimer()
                Intent.ACTION_SCREEN_OFF -> service?.stopTimer()
            }
        }
    }
}

package com.awesomeproject

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.appboy.Appboy
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.firebase.messaging.FirebaseMessaging

class NotificationModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val NAME = "Notification"
        var notificationToken: String? = null

        const val PUSH_MESSAGES = "messages"
        const val PUSH_SUBSCRIPTION = "subscription"

        const val ACTION_DELETED = "deleted"
        const val ACTION_RECEIVED = "received"
        const val ACTION_OPENED = "opened"

        private const val PUSH_TOKEN = "push-token"

        // to standardise between Android & iOS
        private val STATUSES = hashMapOf(
            "denied" to 1,
            "authorized" to 2
        )
        private val ACTIONS = hashMapOf(
            ACTION_DELETED to ACTION_DELETED,
            ACTION_RECEIVED to ACTION_RECEIVED,
            ACTION_OPENED to ACTION_OPENED
        )
        private val TYPES =
            hashMapOf(PUSH_MESSAGES to PUSH_MESSAGES, PUSH_SUBSCRIPTION to PUSH_SUBSCRIPTION)

        private var instance: NotificationModule? = null
        private val queuedEvents: HashMap<String, WritableMap> = HashMap()
        fun getInstance(): NotificationModule? {
            return instance
        }

        fun queueEvent(key: String, args: WritableMap) {
            queuedEvents[key] = args
        }
    }

    private lateinit var notificationManagerCompat: NotificationManagerCompat
    override fun initialize() {
        super.initialize()
        notificationManagerCompat = NotificationManagerCompat.from(reactContext)

        instance = this
        val iterator = queuedEvents.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            sendEvent(item.key, item.value)
        }
        queuedEvents.clear()
    }

    override fun getName(): String {
        return NAME
    }

    override fun getConstants(): MutableMap<String, Any> {
        val constants = HashMap<String, Any>()
        constants["statuses"] = STATUSES
        constants["actions"] = ACTIONS
        constants["types"] = TYPES
        return constants
    }

    fun sendEvent(eventName: String, params: WritableMap) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    @ReactMethod
    fun isNotificationPermissionGranted(promise: Promise) {
        val status = when (notificationManagerCompat.areNotificationsEnabled()) {
            true -> STATUSES["authorized"]
            false -> STATUSES["denied"]
        }
        promise.resolve(status)
    }

    @ReactMethod
    fun requestPermission() {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, reactContext.packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:".plus(reactContext.packageName))
            }
        }
        reactContext.currentActivity?.startActivity(intent)
    }

    @ReactMethod
    fun getNotificationToken(promise: Promise) {
        if (notificationToken.isNullOrBlank()) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener {
                promise.resolve(it)
            }
        } else {
            promise.resolve(notificationToken)
        }
    }

    @ReactMethod
    fun ensurePushRegistered() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener {
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(PUSH_TOKEN, it)
                Log.d("CHECK BRAZE", "ensurePushRegistered >> $it")
                val appboyInstance = Appboy.getInstance(reactContext)
                if (!appboyInstance.registeredPushToken.equals(it)) {
                    Log.d("CHECK BRAZE", "ensurePushRegistered condition >> $it")
                    appboyInstance.registerAppboyPushMessages(it)
                }

            }
    }
}

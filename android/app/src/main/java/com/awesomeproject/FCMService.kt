package com.awesomeproject

import com.appboy.Appboy
import com.appboy.AppboyFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Appboy.getInstance(this).registerAppboyPushMessages(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (AppboyFirebaseMessagingService.handleBrazeRemoteMessage(this, message)) {
            // This Remote Message originated from Braze and a push notification was displayed.
            // No further action is needed.
            return
        }
    }
}
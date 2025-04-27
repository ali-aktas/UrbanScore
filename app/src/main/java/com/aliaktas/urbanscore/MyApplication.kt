package com.aliaktas.urbanscore

import android.app.Application
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.google.firebase.FirebaseApp
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()

        // Mevcut başlatma kodları
        FirebaseApp.initializeApp(this)

        // Firebase Analytics başlatma
        firebaseAnalytics = Firebase.analytics

        // Firebase Crashlytics yapılandırma
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        MobileAds.initialize(this) { initializationStatus: InitializationStatus ->
        }

        // RevenueCat'i başlat
        RevenueCatManager.initialize(this)
    }
}

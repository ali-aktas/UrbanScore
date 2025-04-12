package com.aliaktas.urbanscore

import android.app.Application
import com.aliaktas.urbanscore.util.RevenueCatManager
import com.google.firebase.FirebaseApp
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Mevcut başlatma kodları
        FirebaseApp.initializeApp(this)

        MobileAds.initialize(this) { initializationStatus: InitializationStatus ->
        }

        // RevenueCat'i başlat
        RevenueCatManager.initialize(this)
    }

}

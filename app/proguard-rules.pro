# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Firestore
-keep class com.aliaktas.urbanscore.data.model.** { *; }
-keepclassmembers class com.aliaktas.urbanscore.data.model.** { *; }

# ÖNEMLİ: Firebase deserilaştırma için gerekli
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Spesifik olarak CuratedCityItem için (ekstra güvenlik)
-keep class com.aliaktas.urbanscore.data.model.CuratedCityItem { *; }
-keepclassmembers class com.aliaktas.urbanscore.data.model.CuratedCityItem { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# RevenueCat
-keep class com.revenuecat.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule

# AdMob ve UMP
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# Chart kütüphanesi
-keep class com.github.mikephil.charting.** { *; }
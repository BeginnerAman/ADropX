# ADrop Proguard rules
-keepattributes *Annotation*
-dontwarn com.journeyapps.barcodescanner.**
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-keep class com.google.zxing.** { *; }

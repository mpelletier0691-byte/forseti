# Forseti ProGuard rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Room
-keep class androidx.room.** { *; }

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class com.forseti.**$$serializer { *; }
-keepclassmembers class com.forseti.** {
    *** Companion;
}
-keepclasseswithmembers class com.forseti.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# PDF rendering uses the framework's android.graphics.pdf.PdfRenderer; no
# third-party PDF library to keep here.

# ML Kit
-keep class com.google.mlkit.** { *; }

# SLF4J: transitive slf4j-api (Ktor / OkHttp) without a binding trips R8 "Missing class
# org.slf4j.impl.StaticLoggerBinder". We ship slf4j-android; these silence optional
# discovery paths some dependency JARs still reference.
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder

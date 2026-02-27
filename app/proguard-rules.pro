# AudioSpy — keep model + DB classes
-keep class com.audiospy.model.** { *; }
-keep class com.audiospy.db.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

-keepattributes *Annotation*, Signature, Exception

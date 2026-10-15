# Prevents extension classes (annotated with `@Extension`) from being removed, renamed or repackged.
-keep @com.google.appinventor.components.annotations.Extension public class * {
  public *;
}

# Prevents helper classes from being removed, renamed or repackaged
-keepnames class * implements com.google.appinventor.components.common.OptionList {
  *;
}

-dontwarn kotlinx.atomicfu.**
-dontwarn kotlin.concurrent.atomics.**
-dontwarn kotlin.jvm.internal.EnhancedNullability
-dontwarn kotlin.reflect.full.**
-dontwarn kotlin.reflect.jvm.**
-dontwarn androidx.annotation.**
-dontwarn java.beans.**
-dontwarn java.awt.**
-dontwarn java.lang.management.**
-dontwarn com.sun.nio.file.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn org.graalvm.nativeimage.**

-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

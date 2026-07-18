# MO MACRO ProGuard Rules
-keepattributes Signature
-keepattributes *Annotation*

# Keep JSON classes
-keep class org.json.** { *; }
-dontwarn org.json.**

# Keep macro classes
-keep class com.monaim.studio.macro.** { *; }
-keep class com.monaim.studio.service.** { *; }

# Accessibility Service
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

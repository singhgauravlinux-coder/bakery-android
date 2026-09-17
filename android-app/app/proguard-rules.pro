# Add project specific ProGuard rules here.
# Retrofit/OkHttp/Gson generally work fine with default R8 rules on recent AGP versions;
# add -keep rules here if you hit reflection issues with your model classes.

# ---------------------------------------------------------------------------
# Gson wire contract: field names ARE the JSON keys Gson emits via reflection.
# Without this, R8 renames/shrinks these classes and every auth/order call
# gets obfuscated JSON keys ({"a":..,"b":..}) and 400s on the backend.
# Verified in CI by scripts/verify-minified-wire-contract.sh.
# ---------------------------------------------------------------------------
-keep class com.crumbandember.app.data.model.** { *; }
-keepclassmembers class com.crumbandember.app.data.model.** { *; }

# Gson needs generic type info at runtime (e.g. List<T>, TypeToken) and
# annotations (e.g. @SerializedName) to survive.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

# Retrofit uses generic signatures on service interface methods reflectively.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep interface com.crumbandember.app.data.api.** { *; }

# ---------------------------------------------------------------------------
# R8 / ProGuard rules for minified (release) builds.
#
# WHY THIS FILE IS NOT EMPTY ANY MORE
#
# Retrofit serialises request/response bodies with Gson, and Gson maps JSON
# keys to *Java field names by reflection*. None of our models are annotated
# with @SerializedName, so with `isMinifyEnabled = true` R8 is free to rename
#   RegisterRequest.email    -> a
#   RegisterRequest.password -> b
#   RegisterRequest.name     -> c
# and the production APK posts {"a":"...","b":"...","c":"..."} to
# /api/auth/register. auth-service sees neither `email` nor `password` and
# answers 400 before it touches the database. Debug builds are not minified,
# so this is invisible until a release APK is installed on a real device.
#
# Field names in data/model are a wire contract with the backend. Keep them
# verbatim. This covers responses too — a renamed field is never populated on
# deserialisation, which lands as a null in a non-null Kotlin property.
# ---------------------------------------------------------------------------

# --- Wire models: field names must survive minification --------------------
-keep class com.crumbandember.app.data.model.** { *; }

# --- Retrofit --------------------------------------------------------------
# Retrofit reads return types, suspend-function continuations and method
# annotations reflectively at proxy-creation time.
-keep,allowobfuscation,allowshrinking interface com.crumbandember.app.data.api.ApiService

-keepattributes Signature
-keepattributes InnerClasses, EnclosingMethod
-keepattributes AnnotationDefault
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations

-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit's own service-method annotations
-keep class retrofit2.http.** { *; }

# --- Gson ------------------------------------------------------------------
# Generic TypeTokens (e.g. Response<List<Product>>) resolve through Signature,
# kept above; these keep the reflective machinery itself intact.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn sun.misc.**

# --- OkHttp / Okio ---------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Keep line numbers so release crash reports stay readable --------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

package org.phenoapps.intercross.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri

internal fun openUri(context: Context, url: String) {
    startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

internal fun openAppOrStore(context: Context, packageName: String) {
    val packageManager = context.packageManager
    try {
        packageManager.getPackageInfo(packageName, 0)
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startSafely(context, it)
            return
        }
    } catch (_: PackageManager.NameNotFoundException) {
    }
    openAppStore(context, packageName)
}

internal fun openAppStore(context: Context, packageName: String) {
    val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        openUri(context, "https://play.google.com/store/apps/details?id=$packageName")
    }
}

internal fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

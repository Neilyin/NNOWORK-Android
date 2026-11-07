package com.neilyin.nnowork.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** 開啟本 App 的 Google Play 頁面（更新、給評分共用）。 */
object PlayStore {
    fun marketUri(packageName: String): Uri = Uri.parse("market://details?id=$packageName")

    fun webUri(packageName: String): Uri =
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

    /** 先走 market://（直接進 Play 商店 App），沒有商店 App 時退回 https。 */
    fun openListing(context: Context) {
        val pkg = context.packageName
        val market = Intent(Intent.ACTION_VIEW, marketUri(pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            val web = Intent(Intent.ACTION_VIEW, webUri(pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(web) }
        }
    }
}

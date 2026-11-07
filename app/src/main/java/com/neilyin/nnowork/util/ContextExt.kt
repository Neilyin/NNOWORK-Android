package com.neilyin.nnowork.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** 從 Compose 的 LocalContext 往上找宿主 Activity（找不到回 null）。 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

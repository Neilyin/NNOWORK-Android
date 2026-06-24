package com.neilyin.nnowork.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** 全 App 共用的 Moshi 實例。 */
object JsonProvider {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    inline fun <reified T> adapter() = moshi.adapter(T::class.java)
}

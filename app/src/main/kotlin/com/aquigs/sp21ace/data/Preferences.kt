package com.aquigs.sp21ace.data

import android.content.SharedPreferences

/** The value saved under [key]. A name no value has, such as one a later version dropped, reads as [default] rather than failing the load. */
internal inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String, default: T): T {
    val name = getString(key, null)
    return enumValues<T>().firstOrNull { it.name == name } ?: default
}

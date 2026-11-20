package com.aquigs.sp21ace.data

import android.content.SharedPreferences

/** The value saved under [key]. A name no value has, such as one a later version dropped, reads as [default] rather than failing the load. */
internal inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String, default: T): T = enumNamed(getString(key, null)) ?: default

/** The value named [name], or null for none or a name no value has, such as one a later version added. */
internal inline fun <reified T : Enum<T>> enumNamed(name: String?): T? = enumValues<T>().firstOrNull { it.name == name }

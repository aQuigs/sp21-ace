package com.aquigs.sp21ace

import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/** The object after a trip through Java serialization, the way the activity's saved state keeps it through process death. */
fun Serializable.serializedAndBack(): Any {
    val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(this) } }.toByteArray()
    return ObjectInputStream(bytes.inputStream()).use { it.readObject() }
}

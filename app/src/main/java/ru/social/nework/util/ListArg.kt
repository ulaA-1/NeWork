package ru.social.nework.util

import android.os.Bundle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object LongArrayArg: ReadWriteProperty<Bundle, LongArray?> {

    override fun setValue(thisRef: Bundle, property: KProperty<*>, value: LongArray?) {
        thisRef.putLongArray(property.name, value ?: LongArray(0))
    }

    override fun getValue(thisRef: Bundle, property: KProperty<*>): LongArray? =
        thisRef.getLongArray(property.name)
}
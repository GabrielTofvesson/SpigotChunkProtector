package dev.w1zzrd.spigot.chunkprotector.collection

import java.util.*
import kotlin.Comparator

class BinaryCache<K, V> (cacheSize: Int, private val comparator: Comparator<K>, private val cacheMiss: (K) -> V?, private val keyType: Class<K>) {

    companion object {
        inline fun <reified K, V> makeCache(cacheSize: Int, comparator: Comparator<K>, noinline cacheMiss: (K) -> V?) =
            BinaryCache(cacheSize, comparator, cacheMiss, K::class.java)

        inline fun <reified K: Comparable<K>, V> makeCache(cacheSize: Int, noinline cacheMiss: (K) -> V?) =
            BinaryCache(cacheSize, Comparable<K>::compareTo, cacheMiss, K::class.java)
    }

    private val keys = java.lang.reflect.Array.newInstance(keyType, cacheSize) as Array<K>
    private val values = Array<Any?>(cacheSize) { null }
    private val ages = java.lang.reflect.Array.newInstance(keyType, cacheSize) as Array<K> // Essentially a fifo queue

    private var entryCount = 0
    private var oldest = 0

    fun clearValues(value: V) {
        val scratch1 = java.lang.reflect.Array.newInstance(keyType, ages.size) as Array<K>

        // Since ages softly depend on key/value entries, it's easiest to process them first
        var copyIndex = 0
        for (index in oldest until entryCount)
            if (values[indexOf(ages[index])] != value)
                scratch1[copyIndex++] = ages[index]

        if (entryCount == ages.size) {
            for (index in 0 until oldest)
                if (values[indexOf(ages[index])] != value)
                    scratch1[copyIndex++] = ages[index]
        }

        // No change
        if (copyIndex == entryCount)
            return

        // Just re-index the queue so that the oldest entry lies at index 0
        System.arraycopy(scratch1, 0, ages, 0, copyIndex)
        oldest = 0

        copyIndex = 0

        val scratch2 = Array<Any?>(values.size){ null }

        for (index in 0 until entryCount)
            if (values[index] != value) {
                scratch1[copyIndex] = keys[index]
                scratch2[copyIndex++] = values[index]
            }

        System.arraycopy(scratch1, 0, keys, 0, copyIndex)
        System.arraycopy(scratch2, 0, values, 0, copyIndex)

        entryCount -= copyIndex
    }

    operator fun get(key: K): V? {
        var index = indexOf(key)

        // Cache hit
        if (index >= 0)
            return values[index] as V

        // Cache miss
        index = -(index + 1)

        val value = cacheMiss(key) ?: return null

        if (entryCount < keys.size) {
            System.arraycopy(keys, index, keys, index + 1, entryCount - index)
            System.arraycopy(values, index, values, index + 1, entryCount - index)

            ages[(oldest + entryCount).rem(ages.size)] = key

            ++entryCount
        } else {
            // We're out of spaces. This works
            if (index > 0)
                --index

            // Find oldest entry
            val oldestIndex = indexOf(ages[oldest])

            if (oldestIndex > index) {
                System.arraycopy(keys, index, keys, index + 1, oldestIndex - index)
                System.arraycopy(values, index, values, index + 1, oldestIndex - index)
            } else if (oldestIndex < index) {
                System.arraycopy(keys, oldestIndex + 1, keys, oldestIndex, index - oldestIndex)
                System.arraycopy(values, oldestIndex + 1, values, oldestIndex, index - oldestIndex)
            }

            // Overwrite oldest entry with new entry
            ages[oldest] = key

            // Re-index age list so that current oldest entry becomes youngest
            oldest = (oldest + 1).rem(ages.size)
        }

        keys[index] = key
        values[index] = value

        return value
    }

    private fun indexOf(key: K) =
        keys.binarySearch(key, comparator, 0, entryCount)
}
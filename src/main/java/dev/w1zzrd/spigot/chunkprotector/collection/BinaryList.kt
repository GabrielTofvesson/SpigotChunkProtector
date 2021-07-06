package dev.w1zzrd.spigot.chunkprotector.collection

import dev.w1zzrd.spigot.chunkprotector.collection.BinaryList.Companion.wrapSortedList
import org.bukkit.configuration.serialization.ConfigurationSerializable

class SerializableBinaryList<T>: ConfigurationSerializable {
    lateinit var list: BinaryList<T>
        private set

    constructor(map: Map<String, Any?>) {
        if (map.containsKey("list"))
            list = wrapSortedList({ a, b -> (a as Comparable<T>).compareTo(b) }, map["list"] as MutableList<T>)
    }

    constructor(list: BinaryList<T>) {
        this.list = list
    }

    override fun serialize() = mutableMapOf("list" to list.toMutableList())
}

class BinaryList<T> private constructor(private val backing: MutableList<T>, private val comparator: Comparator<in T>): MutableList<T> by backing {

    companion object {
        fun <K> newBinaryList(comparator: Comparator<in K>, backingFactory: () -> MutableList<K> = ::ArrayList) =
            BinaryList(backingFactory(), comparator)

        fun <K: Comparable<K>> newBinaryList(backingFactory: () -> MutableList<K> = ::ArrayList) =
            newBinaryList({ a, b -> a.compareTo(b) }, backingFactory)

        fun <K> wrapSortedList(comparator: Comparator<in K>, backingList: MutableList<K>) =
            BinaryList(backingList, comparator)

        fun <K: Comparable<K>> wrapSortedList(backingList: MutableList<K>) =
            wrapSortedList({ a, b -> a.compareTo(b) }, backingList)
    }

    override fun add(element: T): Boolean {
        val index = binarySearch(element, comparator)
        if (index >= 0)
            return false

        backing.add(-(index + 1), element)
        return true
    }

    override fun addAll(elements: Collection<T>) = elements.map(this::add).reduce(Boolean::or)
    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        throw UnsupportedOperationException("Sorted list does not support inserting elements at a specific index")
    }

    override fun remove(element: T): Boolean {
        val index = binarySearch(element, comparator)
        if (index < 0)
            return false

        backing.removeAt(index)
        return true
    }

    override fun removeAll(elements: Collection<T>) = elements.map(this::remove).reduce(Boolean::or)

    override fun contains(element: T) = binarySearch(element, comparator) >= 0
    fun <K : Comparable<K>> contains(element: K, convert: T.() -> K) = binarySearch { it.convert().compareTo(element) } >= 0
    override fun indexOf(element: T): Int {
        val index = binarySearch(element, comparator)
        if (index < 0)
            return -1

        return index
    }

    fun getOrAdd(element: T): T {
        val index = binarySearch(element, comparator)
        return if (index >= 0)
            backing[index]
        else {
            backing.add(-(index + 1), element)
            element
        }
    }
}
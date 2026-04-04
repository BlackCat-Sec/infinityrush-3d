package com.relicrush.game.utils

class ObjectPool<T>(private val factory: () -> T) {
    private val items = ArrayDeque<T>()

    fun obtain(): T {
        return if (items.isEmpty()) factory() else items.removeLast()
    }

    fun recycle(item: T) {
        items.addLast(item)
    }

    fun recycleAll(collection: MutableList<T>) {
        collection.forEach(::recycle)
        collection.clear()
    }
}

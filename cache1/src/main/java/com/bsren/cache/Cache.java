package com.bsren.cache;

public interface Cache<K,V> {

    V getIfPresent(Object key);

    void put(K key,V value);

    void invalidate(Object key);

    int size();

    void cleanUp();
}

package com.bsren.cache;

import java.util.Map;

public interface Cache<K,V> {

    V getIfPresent(Object key);

    void put(K key,V value);

    void invalidate(Object key);

    void invalidateAll(Iterable<?> keys);

    void putAll(Map<? extends K, ? extends V> m);

    int size();

    void cleanUp();
}

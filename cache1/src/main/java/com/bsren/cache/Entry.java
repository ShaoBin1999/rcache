package com.bsren.cache;

public interface Entry<K,V> {


    Entry<K,V> getNext();

    int getHash();

    K getKey();

    Object getValueReference();

    void setValue(Object value);
}

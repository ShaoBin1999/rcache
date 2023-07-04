package com.bsren.cache;

public class StrongEntry<K,V> implements Entry<K,V> {

    public StrongEntry(K key, int hash, Entry<K,V> next){
        this.key = key;
        this.hash = hash;
        this.next = next;
    }

    K key;

    int hash;

    Entry<K,V> next;

    Object value;

    @Override
    public Entry<K, V> getNext() {
        return next;
    }

    @Override
    public int getHash() {
        return hash;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public Object getValueReference() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

}

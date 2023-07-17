package com.bsren.cache;

import com.google.j2objc.annotations.Weak;

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

    @Override
    public void setAccessTime(long time) {

    }

    @Override
    public long getAccessTime() {
        return 0;
    }

    @Override
    public void setWriteTime(long time) {

    }

    @Override
    public long getWriteTime() {
        return 0;
    }

    @Override
    public Entry<K, V> getNextInAccessQueue() {
        return null;
    }

    @Override
    public void setNextInAccessQueue(Entry<K, V> next) {

    }

    @Override
    public Entry<K, V> getPreviousInAccessQueue() {
        return null;
    }

    @Override
    public void setPreviousInAccessQueue(Entry<K, V> previous) {

    }

    @Override
    public Entry<K, V> getNextInWriteQueue() {
        return null;
    }

    @Override
    public void setNextInWriteQueue(Entry<K, V> next) {

    }

    @Override
    public Entry<K, V> getPreviousInWriteQueue() {
        return null;
    }

    @Override
    public void setPreviousInWriteQueue(Entry<K, V> previous) {

    }

    @Weak Entry<K,V> nextAccess = null;

    @Weak Entry<K,V> prevAccess = null;

    @Weak Entry<K,V> nextWrite = null;

    @Weak Entry<K,V> prevWrite = null;




}

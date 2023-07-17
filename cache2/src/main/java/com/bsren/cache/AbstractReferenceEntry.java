package com.bsren.cache;

public abstract class AbstractReferenceEntry<K,V> implements Entry<K,V> {

	@Override
	public Entry<K, V> getNext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getHash() {
		throw new UnsupportedOperationException();
	}

	@Override
	public K getKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getValueReference() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAccessTime(long time) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getAccessTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setWriteTime(long time) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getWriteTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> getNextInAccessQueue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNextInAccessQueue(Entry<K, V> next) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> getPreviousInAccessQueue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPreviousInAccessQueue(Entry<K, V> previous) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> getNextInWriteQueue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNextInWriteQueue(Entry<K, V> next) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entry<K, V> getPreviousInWriteQueue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPreviousInWriteQueue(Entry<K, V> previous) {
		throw new UnsupportedOperationException();
	}
}

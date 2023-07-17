package com.bsren.cache;

public enum NullEntry implements Entry<Object,Object> {
	INSTANCE;

	@Override
	public Entry<Object, Object> getNext() {
		return null;
	}

	@Override
	public int getHash() {
		return 0;
	}

	@Override
	public Object getKey() {
		return null;
	}

	@Override
	public Object getValueReference() {
		return null;
	}

	@Override
	public void setValue(Object value) {

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
	public Entry<Object, Object> getNextInAccessQueue() {
		return null;
	}

	@Override
	public void setNextInAccessQueue(Entry<Object, Object> next) {

	}

	@Override
	public Entry<Object, Object> getPreviousInAccessQueue() {
		return null;
	}

	@Override
	public void setPreviousInAccessQueue(Entry<Object, Object> previous) {

	}

	@Override
	public Entry<Object, Object> getNextInWriteQueue() {
		return null;
	}

	@Override
	public void setNextInWriteQueue(Entry<Object, Object> next) {

	}

	@Override
	public Entry<Object, Object> getPreviousInWriteQueue() {
		return null;
	}

	@Override
	public void setPreviousInWriteQueue(Entry<Object, Object> previous) {

	}
}

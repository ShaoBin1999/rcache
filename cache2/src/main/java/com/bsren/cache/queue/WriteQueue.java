package com.bsren.cache.queue;

import com.bsren.cache.AbstractReferenceEntry;
import com.bsren.cache.Entry;
import com.bsren.cache.NullEntry;
import com.google.common.collect.AbstractSequentialIterator;

import java.util.AbstractQueue;
import java.util.Iterator;

public class WriteQueue<K,V>  extends AbstractQueue<Entry<K,V>> {

	final Entry<K,V> head = new AbstractReferenceEntry<K, V>() {
		@Override
		public long getWriteTime() {
			return Long.MAX_VALUE;
		}

		@Override
		public void setWriteTime(long time) {
		}

		Entry<K,V> nextWrite = this;

		@Override
		public Entry<K, V> getNextInWriteQueue() {
			return nextWrite;
		}

		@Override
		public void setNextInWriteQueue(Entry<K, V> next) {
			this.nextWrite = next;
		}

		Entry<K,V> previousWrite = this;

		@Override
		public Entry<K, V> getPreviousInWriteQueue() {
			return previousWrite;
		}

		@Override
		public void setPreviousInWriteQueue(Entry<K, V> previous) {
			this.previousWrite = previous;
		}

	};

	@Override
	public boolean offer(Entry<K, V> entry) {
		connectWriteOrder(entry.getPreviousInWriteQueue(),entry.getNextInWriteQueue());
		connectWriteOrder(head.getPreviousInWriteQueue(),entry);
		connectWriteOrder(entry,head);
		return true;
	}

	@Override
	public Entry<K, V> peek() {
		Entry<K, V> next = head.getNextInWriteQueue();
		if(next==head){
			return null;
		}else {
			return next;
		}
	}

	@Override
	public Entry<K, V> poll() {
		Entry<K, V> next = head.getNextInWriteQueue();
		if(head==next){
			return null;
		}
		remove(next);
		return next;
	}

	@Override
	public boolean remove(Object o) {
		Entry<K,V> e = (Entry<K, V>) o;
		connectWriteOrder(e.getPreviousInWriteQueue(),e.getNextInWriteQueue());
		nuffifyWriteOrder(e);
		return e.getNextInWriteQueue()!=NullEntry.INSTANCE;
	}

	@Override
	public boolean contains(Object o) {
		Entry<K,V> e = (Entry<K, V>) o;
		return e.getNextInWriteQueue()!=NullEntry.INSTANCE;
	}

	@Override
	public boolean isEmpty() {
		return head.getNextInWriteQueue()==head;
	}

	@Override
	public int size() {
		int size = 0;
		for (Entry<K,V> e = head.getNextInWriteQueue();e!=head;e = e.getNextInWriteQueue()){
			size++;
		}
		return size;
	}

	@Override
	public void clear() {
		Entry<K,V> e = head.getNextInWriteQueue();
		while (e!=head){
			Entry<K,V> next = e.getNextInWriteQueue();
			nuffifyWriteOrder(next);
			e = next;
		}
		head.setNextInWriteQueue(head);
		head.setPreviousInWriteQueue(head);
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		return new AbstractSequentialIterator<Entry<K, V>>(peek()) {
			@Override
			protected Entry<K, V> computeNext(Entry<K, V> previous) {
				Entry<K, V> next = previous.getNextInWriteQueue();
				return (next == head) ? null : next;
			}
		};
	}

	public static <K,V> void connectWriteOrder(Entry<K,V> previous, Entry<K,V> next){
		previous.setNextInWriteQueue(next);
		next.setPreviousInWriteQueue(previous);
	}

	public static <K,V> void nuffifyWriteOrder(Entry<K,V> nulled){
		Entry<K,V> nullEntry = (Entry<K, V>) NullEntry.INSTANCE;
		nulled.setPreviousInWriteQueue(nullEntry);
		nulled.setNextInWriteQueue(nullEntry);
	}
}

package com.bsren.cache.queue;

import com.bsren.cache.AbstractReferenceEntry;
import com.bsren.cache.Entry;
import com.bsren.cache.NullEntry;
import com.google.common.collect.AbstractSequentialIterator;

import java.util.AbstractQueue;
import java.util.Iterator;

public class AccessQueue<K, V> extends AbstractQueue<Entry<K, V>> {

	final Entry<K, V> head = new AbstractReferenceEntry<K, V>() {
		@Override
		public long getAccessTime() {
			return Long.MAX_VALUE;
		}

		@Override
		public void setAccessTime(long time) {
		}

		Entry<K, V> nextAccess = this;

		@Override
		public Entry<K, V> getNextInAccessQueue() {
			return nextAccess;
		}

		@Override
		public void setNextInAccessQueue(Entry<K, V> next) {
			this.nextAccess = next;
		}

		Entry<K, V> previousAccess = this;

		@Override
		public Entry<K, V> getPreviousInAccessQueue() {
			return previousAccess;
		}

		@Override
		public void setPreviousInAccessQueue(Entry<K, V> previous) {
			this.previousAccess = previous;
		}
	};

	@Override
	public boolean offer(Entry<K, V> entry) {
		connectAccessOrder(entry.getPreviousInAccessQueue(), entry.getNextInAccessQueue());
		connectAccessOrder(entry.getPreviousInAccessQueue(), entry);
		connectAccessOrder(entry, head);
		return true;
	}

	@Override
	public Entry<K, V> peek() {
		Entry<K, V> next = head.getNextInAccessQueue();
		return next == head ? null : next;
	}

	@Override
	public Entry<K, V> poll() {
		Entry<K, V> next = head.getNextInAccessQueue();
		if (next == head) {
			return null;
		}
		remove(next);
		return next;
	}

	@Override
	public boolean remove(Object o) {
		Entry<K, V> e = (Entry<K, V>) o;
		connectAccessOrder(e.getPreviousInAccessQueue(), e.getNextInAccessQueue());
		nullifyAccessOrder(e);
		return e.getNextInAccessQueue() != NullEntry.INSTANCE;
	}

	@Override
	public boolean contains(Object o) {
		Entry<K, V> entry = (Entry<K, V>) o;
		return entry.getNextInAccessQueue() != NullEntry.INSTANCE;
	}

	@Override
	public boolean isEmpty() {
		return head.getNextInAccessQueue() == head;
	}

	@Override
	public int size() {
		int size = 0;
		for (Entry<K, V> e = head.getNextInAccessQueue(); e != head; e = e.getNextInAccessQueue()) {
			size++;
		}
		return size;
	}

	@Override
	public void clear() {
		Entry<K, V> e = head.getNextInAccessQueue();
		while (e != null) {
			Entry<K, V> next = e.getNextInAccessQueue();
			nullifyAccessOrder(e);
			e = next;
		}
		head.setNextInAccessQueue(head);
		head.setPreviousInAccessQueue(head);
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

	public static <K, V> void connectAccessOrder(Entry<K, V> previous, Entry<K, V> next) {
		previous.setNextInAccessQueue(next);
		next.setPreviousInAccessQueue(previous);
	}

	public static <K, V> void nullifyAccessOrder(Entry<K, V> nulled) {
		Entry<K, V> nullEntry = (Entry<K, V>) NullEntry.INSTANCE;
		nulled.setPreviousInAccessQueue(nullEntry);
		nulled.setNextInAccessQueue(nullEntry);
	}


}

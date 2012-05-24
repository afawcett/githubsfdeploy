package com.example.model;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Ryan Brainard
 */
public abstract class IteratorFilter<T> implements Iterator<T> {

    private final Iterator<T> filteree;
    private Queue<T> nexts = new ArrayBlockingQueue<T>(1);

    public IteratorFilter(Iterator<T> filteree) {
        this.filteree = filteree;
    }

    @Override
    public synchronized boolean hasNext() {
        if (!nexts.isEmpty()) {
            return true;
        }

        while (filteree.hasNext()) {
            T maybeNext = filteree.next();
            if (canBeNext(maybeNext)) {
                nexts.offer(maybeNext);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return nexts.remove();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from a filter");
    }

    abstract boolean canBeNext(T maybeNext);
}

package com.example.model;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Ryan Brainard
 */
public abstract class AbstractRichSObjectFieldIteratorFilter extends RichSObjectWrapper {

    public AbstractRichSObjectFieldIteratorFilter(RichSObject wrapped) {
        super(wrapped);
    }

    @Override
    public Iterator<RichField> getFields() {
        return new Iterator<RichField>() {

            private final Iterator<RichField> filteree = wrapped.getFields();
            private Queue<RichField> nexts = new ArrayBlockingQueue<RichField>(1);

            @Override
            public synchronized boolean hasNext() {
                if (!nexts.isEmpty()) {
                    return true;
                }

                while (filteree.hasNext()) {
                    RichField maybeNext = filteree.next();
                    if (canBeNext(maybeNext)) {
                        nexts.offer(maybeNext);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public synchronized RichField next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return nexts.remove();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove from a filter");
            }
        };
    }
    
    abstract boolean canBeNext(RichField maybeNext);
}

package com.example.model;

import com.force.api.DescribeSObject;

import java.util.Iterator;

/**
 * @author Ryan Brainard
 */
abstract class RichSObjectWrapper implements RichSObject {

    protected final RichSObject wrapped;

    public RichSObjectWrapper(RichSObject wrapped) {
        this.wrapped = wrapped;
    }
    
    @Override
    public DescribeSObject getMetadata() {
        return wrapped.getMetadata();
    }

    @Override
    public RichField get(String fieldName) {
        return wrapped.get(fieldName);
    }

    @Override
    public Iterator<RichField> getFields() {
        return wrapped.getFields();
    }

    @Override
    public Iterator<RichField> iterator() {
        return wrapped.getFields();
    }
}

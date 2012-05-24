package com.example.model;

import java.util.Iterator;

/**
 * @author Ryan Brainard
 */
public class UpdateableFieldsOnlyFilter extends IteratorFilter<RichSObject.RichField> {

    public UpdateableFieldsOnlyFilter(Iterator<RichSObject.RichField> filteree) {
        super(filteree);
    }

    @Override
    boolean canBeNext(RichSObject.RichField maybeNext) {
        return maybeNext.getMetadata().isUpdateable();
    }

}

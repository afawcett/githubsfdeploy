package com.example.model;

import java.util.Iterator;

/**
 * @author Ryan Brainard
 */
public class PopulatedFieldsOnlyFilter extends IteratorFilter<RichSObject.RichField> {

    public PopulatedFieldsOnlyFilter(Iterator<RichSObject.RichField> filteree) {
        super(filteree);
    }

    @Override
    boolean canBeNext(RichSObject.RichField maybeNext) {
        return maybeNext.getValue() != null;
    }
}

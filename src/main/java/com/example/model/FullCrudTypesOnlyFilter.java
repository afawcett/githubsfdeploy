package com.example.model;

import com.force.api.DescribeSObject;

import java.util.Iterator;

/**
 * @author Ryan Brainard
 */
public class FullCrudTypesOnlyFilter extends IteratorFilter<DescribeSObject> {

    public FullCrudTypesOnlyFilter(Iterator<DescribeSObject> filteree) {
        super(filteree);
    }

    @Override
    boolean canBeNext(DescribeSObject maybeNext) {
        return maybeNext.isCreateable() &&
               maybeNext.isQueryable()  &&
               maybeNext.isUpdateable() &&
               maybeNext.isDeletable();
    }
}

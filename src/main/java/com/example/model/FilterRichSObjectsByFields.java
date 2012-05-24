package com.example.model;

import java.util.Iterator;

/**
 * @author Ryan Brainard
 */
public class FilterRichSObjectsByFields extends RichSObjectWrapper {

    private final IteratorFilter<RichField> fieldFilterIterator;

    public FilterRichSObjectsByFields(RichSObject wrapped, IteratorFilter<RichSObject.RichField> filter) {
        super(wrapped);
        fieldFilterIterator = filter;
    }

    @Override
    public Iterator<RichField> getFields() {
        return fieldFilterIterator;
    }

    public static FilterRichSObjectsByFields UPDATEABLE_FIELDS_ONLY(RichSObject wrapped) {
        return new FilterRichSObjectsByFields(wrapped, new UpdateableFieldsOnlyFilter(wrapped.getFields()));
    }

    public static FilterRichSObjectsByFields POPULATED_FIELDS_ONLY(RichSObject wrapped) {
        return new FilterRichSObjectsByFields(wrapped, new PopulatedFieldsOnlyFilter(wrapped.getFields()));
    }
}

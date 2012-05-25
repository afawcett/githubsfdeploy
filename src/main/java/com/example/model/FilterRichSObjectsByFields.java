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

    public static FilterRichSObjectsByFields CreateableFieldsOnly(RichSObject wrapped) {
        return new FilterRichSObjectsByFields(wrapped, new IteratorFilter<RichField>(wrapped.getFields()) {
            @Override
            boolean canBeNext(RichField maybeNext) {
                return maybeNext.getMetadata().isCreateable() && !isPersonAccountField(maybeNext);
            }
        });
    }

    public static FilterRichSObjectsByFields UpdateableFieldsOnly(RichSObject wrapped) {
        return new FilterRichSObjectsByFields(wrapped, new IteratorFilter<RichField>(wrapped.getFields()) {
            @Override
            boolean canBeNext(RichField maybeNext) {
                return maybeNext.getMetadata().isUpdateable() && !isPersonAccountField(maybeNext);
            }
        });
    }

    public static FilterRichSObjectsByFields PopulatedFieldsOnly(RichSObject wrapped) {
        return new FilterRichSObjectsByFields(wrapped, new IteratorFilter<RichField>(wrapped.getFields()) {
            @Override
            boolean canBeNext(RichField maybeNext) {
                return maybeNext.getValue() != null;
            }
        });
    }

    public static FilterRichSObjectsByFields StringFieldsOnly(RichSObject wrapped) {
        return new FilterRichSObjectsByFields(wrapped, new IteratorFilter<RichField>(wrapped.getFields()) {
            @Override
            boolean canBeNext(RichField maybeNext) {
                return "string".equals(maybeNext.getMetadata().getType());
            }
        });
    }

    private static boolean isPersonAccountField(RichField maybeNext) {
        if ("Account".equals(maybeNext.getParent().getMetadata().getName())) {
            final String fieldName = maybeNext.getMetadata().getName();
            if ("FirstName".equals(fieldName) ||  "LastName".equals(fieldName) || fieldName.endsWith("__pc")) {
                return true;
            }
        }

        return false;
    }
}

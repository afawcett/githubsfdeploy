package com.example.model;

/**
 * @author Ryan Brainard
 */
public class UpdateableFieldsOnlyFilter extends AbstractRichSObjectFieldIteratorFilter {

    public UpdateableFieldsOnlyFilter(RichSObject wrapped) {
        super(wrapped);
    }

    @Override
    boolean canBeNext(RichField maybeNext) {
        return maybeNext.getMetadata().isUpdateable();
    }
}

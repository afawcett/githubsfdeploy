package com.example.model;

/**
 * @author Ryan Brainard
 */
public class PopulatedFieldsOnlyFilter extends AbstractRichSObjectFieldIteratorFilter {

    public PopulatedFieldsOnlyFilter(RichSObject wrapped) {
        super(wrapped);
    }

    @Override
    boolean canBeNext(RichField maybeNext) {
        return maybeNext.getValue() != null;
    }
}

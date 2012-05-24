package com.example.model;

import com.force.api.DescribeSObject;

import java.util.*;

/**
 * @author Ryan Brainard
 */
public class ImmutableRichSObject implements RichSObject {

    private final DescribeSObject metadata;
    private final Map<String, Object> record;
    private final Map<String, DescribeSObject.Field> indexedFieldMetadata;
    private final List<DescribeSObject.Field> sortedFieldMetadata;

    public ImmutableRichSObject(DescribeSObject metadata, Map<String, Object> record) {
        this(metadata, record, ORDER_BY_FIELD_LABEL);
    }

    public ImmutableRichSObject(DescribeSObject metadata, Map<String, Object> record, Comparator<DescribeSObject.Field> sortFieldsBy) {
        this.metadata = metadata;

        final Map<String, Object> tmpRecord = new HashMap<String, Object>(record.size());
        for (Map.Entry<String, Object> field : record.entrySet()) {
            tmpRecord.put(field.getKey().toUpperCase(), field.getValue());
        }
        this.record = Collections.unmodifiableMap(tmpRecord);

        final Map<String,DescribeSObject.Field> tmpIndexedFieldMetadata = new HashMap<String,DescribeSObject.Field>(metadata.getFields().size());
        for (DescribeSObject.Field f : metadata.getFields()) {
            tmpIndexedFieldMetadata.put(f.getName().toUpperCase(), f);
        }
        this.indexedFieldMetadata = Collections.unmodifiableMap(tmpIndexedFieldMetadata);

        final List<DescribeSObject.Field> tmpSortedFieldMetadata = new ArrayList<DescribeSObject.Field>(metadata.getFields());
        Collections.sort(tmpSortedFieldMetadata, sortFieldsBy);
        this.sortedFieldMetadata = Collections.unmodifiableList(tmpSortedFieldMetadata);
    }

    @Override
    public DescribeSObject getMetadata() {
        return metadata;
    }

    @Override
    public RichField get(String fieldName) {
        return new ImmutableRichField(fieldName.toUpperCase());
    }

    @Override
    public Iterator<RichField> iterator() {
        return getFields();
    }

    @Override
    public Iterator<RichField> getFields() {
        return new Iterator<RichField>() {
            private final Iterator<DescribeSObject.Field> fieldIterator = sortedFieldMetadata.iterator();

            @Override
            public boolean hasNext() {
                return fieldIterator.hasNext();
            }

            @Override
            public RichField next() {
                return get(fieldIterator.next().getName());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Fields cannot be removed");
            }
        };
    }

    public class ImmutableRichField implements RichField {

        private final String fieldName;

        public ImmutableRichField(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public DescribeSObject.Field getMetadata() {
            return indexedFieldMetadata.get(fieldName);
        }

        @Override
        public Object getValue() {
            return record.get(fieldName);
        }
    }

    public static Comparator<DescribeSObject.Field> ORDER_BY_FIELD_LABEL = new Comparator<DescribeSObject.Field>() {
        @Override
        public int compare(DescribeSObject.Field o1, DescribeSObject.Field o2) {
            return o1.getLabel().compareTo(o2.getLabel());
        }
    };
}

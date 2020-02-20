package net.sumaris.rdf.model;

import com.google.common.base.Preconditions;

public enum ModelType {

    SCHEMA,
    DATA;

    public static ModelType fromUserString(String userType) {
        Preconditions.checkNotNull(userType);

        switch(userType.toLowerCase()) {
            case "voc":
            case "vocabulary":
            case "term":
            case "terms":
            case "schema":
                return SCHEMA;
            case "data":
            case "object":
                return DATA;
            default:
                throw new IllegalArgumentException("Unknown model type: " + userType);
        }
    }
}

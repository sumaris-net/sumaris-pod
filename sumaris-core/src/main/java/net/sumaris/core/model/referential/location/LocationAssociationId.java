package net.sumaris.core.model.referential.location;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

@Data
@FieldNameConstants
public class LocationAssociationId implements Serializable {
    private int parentLocation;
    private int childLocation;
}

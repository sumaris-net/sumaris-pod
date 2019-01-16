package net.sumaris.core.model.referential.location;

import lombok.Data;

import java.io.Serializable;

@Data
public class LocationAssociationId implements Serializable {
    private int parentLocation;
    private int childLocation;
}

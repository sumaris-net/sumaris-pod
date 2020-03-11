package net.sumaris.core.service.data;

import lombok.Builder;
import lombok.Getter;

/**
 * @author peck7 on 29/11/2019.
 */
@Builder
@Getter
public class TripSaveOptions {

    private boolean withOperations;
    private boolean withMetiers;
}

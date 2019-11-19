package net.sumaris.core.dao.referential.metier;

import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.model.referential.metier.Metier;
import org.springframework.data.jpa.domain.Specification;

public class MetierSpecifications extends ReferentialSpecifications {

    public static Specification<Metier> inGearIds(Integer[] gearIds) {
        return inLevelIds(Metier.Fields.GEAR, gearIds);
    }

}

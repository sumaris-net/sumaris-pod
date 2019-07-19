package net.sumaris.core.dao.referential.metier;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.jpa.SpecificationWithParameters;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;

public class MetierSpecifications extends ReferentialSpecifications {

    public static Specification<Metier> inGearIds(Integer[] gearIds) {
        return inLevelIds(Metier.PROPERTY_GEAR, gearIds);
    }

}

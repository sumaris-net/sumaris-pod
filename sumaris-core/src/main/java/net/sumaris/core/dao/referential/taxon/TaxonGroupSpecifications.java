package net.sumaris.core.dao.referential.taxon;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;

public class TaxonGroupSpecifications {

    public static Specification<TaxonGroup> hasType(Integer taxonGroupTypeId) {
        if (taxonGroupTypeId == null) return null;
        return (root, query, cb) -> cb.equal(
                    root.get(TaxonGroup.PROPERTY_TAXON_GROUP_TYPE).get(Status.PROPERTY_ID),
                    taxonGroupTypeId);
    }


    public static Specification<TaxonGroup> inGearIds(Integer[] gearIds) {
        if (ArrayUtils.isEmpty(gearIds)) return null;
        return (root, query, cb) -> cb.in(
                    root.joinList(TaxonGroup.PROPERTY_METIERS, JoinType.INNER)
                            .join(Metier.PROPERTY_GEAR, JoinType.INNER)
                            .get(Gear.PROPERTY_ID))
                    .value(ImmutableList.copyOf(gearIds));
    }





}

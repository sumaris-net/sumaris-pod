/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.rdf.core.model;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.rdf.core.util.OwlUtils;

import java.lang.reflect.Method;
import java.util.List;

public class ModelEntities {

    public static List<Method> propertyIncludes = ImmutableList.<Method>builder()
            .add(OwlUtils.getterOfField(TaxonName.class, TaxonName.Fields.TAXONOMIC_LEVEL),
                    OwlUtils.getterOfField(TaxonName.class, TaxonName.Fields.REFERENCE_TAXON),
                    OwlUtils.getterOfField(TaxonName.class, TaxonName.Fields.STATUS),
                    OwlUtils.getterOfField(TaxonomicLevel.class, TaxonomicLevel.Fields.STATUS),
                    OwlUtils.getterOfField(Gear.class, Gear.Fields.STATUS),
                    OwlUtils.getterOfField(Location.class, Location.Fields.STATUS),
                    OwlUtils.getterOfField(Location.class, Location.Fields.LOCATION_LEVEL),
                    OwlUtils.getterOfField(PmfmStrategy.class, PmfmStrategy.Fields.STRATEGY),
                    OwlUtils.getterOfField(PmfmStrategy.class, PmfmStrategy.Fields.ACQUISITION_LEVEL))
            .build();

    public static List<Method> propertyExcludes = ImmutableList.<Method>builder()
            .add(
                    OwlUtils.getterOfField(Gear.class, Gear.Fields.STRATEGIES),
                    OwlUtils.getterOfField(Gear.class, Gear.Fields.CHILDREN),
                    OwlUtils.getterOfField(ReferenceTaxon.class, ReferenceTaxon.Fields.PARENT_TAXON_GROUPS),
                    OwlUtils.getterOfField(ReferenceTaxon.class, ReferenceTaxon.Fields.STRATEGIES),
                    OwlUtils.getterOfField(ReferenceTaxon.class, ReferenceTaxon.Fields.TAXON_NAMES)
            ).build();
}

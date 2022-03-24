package net.sumaris.core.dao.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilege;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.*;
import net.sumaris.core.model.referential.conversion.RoundWeightConversion;
import net.sumaris.core.model.referential.conversion.UnitConversion;
import net.sumaris.core.model.referential.conversion.WeightLengthConversion;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.grouping.Grouping;
import net.sumaris.core.model.referential.grouping.GroupingClassification;
import net.sumaris.core.model.referential.grouping.GroupingLevel;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationClassification;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.model.referential.transcribing.TranscribingItem;
import net.sumaris.core.model.technical.configuration.Software;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.model.technical.extraction.ExtractionProductTable;
import net.sumaris.core.model.technical.history.ProcessingFrequency;
import net.sumaris.core.model.technical.versionning.SystemVersion;
import org.nuiton.i18n.I18n;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class
 */
public class ReferentialEntities {

    // Reserved i18n (required when Nuiton init i18n files)
    static {
        I18n.n("sumaris.persistence.table.status");
        I18n.n("sumaris.persistence.table.department");
        I18n.n("sumaris.persistence.table.location");
        I18n.n("sumaris.persistence.table.locationLevel");
        I18n.n("sumaris.persistence.table.gear");
        I18n.n("sumaris.persistence.table.gearLevel");
        I18n.n("sumaris.persistence.table.parameter");
        I18n.n("sumaris.persistence.table.userProfile");
        I18n.n("sumaris.persistence.table.saleType");
        I18n.n("sumaris.persistence.table.taxonGroup");
        I18n.n("sumaris.persistence.table.taxonGroupType");
        I18n.n("sumaris.persistence.table.taxonomicLevel");
        I18n.n("sumaris.persistence.table.referenceTaxon");
        I18n.n("sumaris.persistence.table.taxonName");
        I18n.n("sumaris.persistence.table.metier");
        I18n.n("sumaris.persistence.table.parameter");
        I18n.n("sumaris.persistence.table.pmfm");
        I18n.n("sumaris.persistence.table.matrix");
        I18n.n("sumaris.persistence.table.fraction");
        I18n.n("sumaris.persistence.table.method");
        I18n.n("sumaris.persistence.table.unit");
        I18n.n("sumaris.persistence.table.qualitativeValue");
        I18n.n("sumaris.persistence.table.program");
        I18n.n("sumaris.persistence.table.acquisitionLevel");
        I18n.n("sumaris.persistence.table.transcribingItem");
        I18n.n("sumaris.persistence.table.groupingClassification");
        I18n.n("sumaris.persistence.table.groupingLevel");
        I18n.n("sumaris.persistence.table.grouping");
        I18n.n("sumaris.persistence.table.distanceToCoastGradient");
        I18n.n("sumaris.persistence.table.depthGradient");
        I18n.n("sumaris.persistence.table.nearbySpecificArea");
        I18n.n("sumaris.persistence.table.extractionProduct");
        I18n.n("sumaris.persistence.table.extractionProductTable");
        I18n.n("sumaris.persistence.table.systemVersion");
        I18n.n("sumaris.persistence.table.originItemType");
        I18n.n("sumaris.persistence.table.strategy");
        I18n.n("sumaris.persistence.table.processingFrequency");
        // Conversion
        I18n.n("sumaris.persistence.table.weightLengthConversion");
        I18n.n("sumaris.persistence.table.roundWeightConversion");
        I18n.n("sumaris.persistence.table.unitConversion");
    }

    public static final List<Class<? extends IReferentialEntity>> ROOT_CLASSES = ImmutableList.of(
                Status.class,
                Department.class,
                Location.class,
                LocationLevel.class,
                LocationClassification.class,
                Gear.class,
                GearClassification.class,
                UserProfile.class,
                SaleType.class,
                VesselType.class,
                // Taxon group
                TaxonGroupType.class,
                TaxonGroup.class,
                // Taxon
                TaxonomicLevel.class,
                TaxonName.class,
                // Métier
                Metier.class,
                // Pmfm
                Pmfm.class,
                Parameter.class,
                Matrix.class,
                Fraction.class,
                Method.class,
                Unit.class,
                ParameterGroup.class,
                QualitativeValue.class,
                // Quality
                QualityFlag.class,
                // Program/strategy
                Program.class,
                Strategy.class,
                AcquisitionLevel.class,
                // Transcribing
                TranscribingItem.class,
                // Grouping
                GroupingClassification.class,
                GroupingLevel.class,
                Grouping.class,
                // Fishing Area
                DistanceToCoastGradient.class,
                DepthGradient.class,
                NearbySpecificArea.class,
                // Product
                ExtractionProduct.class,
                ExtractionProductTable.class,
                // Software
                Software.class,
                // Program
                ProgramPrivilege.class,
                // Technical
                SystemVersion.class,
                OriginItemType.class,
                ProcessingFrequency.class
        );

    public static final List<Class<? extends IReferentialEntity>> SUB_CLASSES = ImmutableList.of(
        WeightLengthConversion.class,
        RoundWeightConversion.class
    );

    public static final List<Class<? extends Serializable>> CLASSES = ImmutableList.<Class<? extends Serializable>>builder()
            .addAll(ROOT_CLASSES)
            .addAll(SUB_CLASSES)
            .build();

    public static final Map<String, Class<? extends Serializable>> CLASSES_BY_NAME = Maps.uniqueIndex(
        CLASSES,
        Class::getSimpleName);

    public static final List<Class<? extends Serializable>> LAST_UPDATE_DATE_CLASSES_EXCLUDES = ImmutableList.of(
        // Grouping
        GroupingClassification.class,
        GroupingLevel.class,
        Grouping.class,
        // Product
        ExtractionProduct.class,
        ExtractionProductTable.class,
        // Software
        Software.class,
        // Technical
        SystemVersion.class,
        UnitConversion.class
    );

    public static final Collection<String> LAST_UPDATE_DATE_ENTITY_NAMES = Stream.concat(
            ROOT_CLASSES.stream(),
            SUB_CLASSES.stream()
        ).filter(c -> !LAST_UPDATE_DATE_CLASSES_EXCLUDES.contains(c))
            .map(Class::getSimpleName)
            .collect(Collectors.toList());

    public static final Map<String, PropertyDescriptor> LEVEL_PROPERTY_BY_CLASS_NAME = createLevelPropertyNameMap(ROOT_CLASSES);

    protected static final Map<String, PropertyDescriptor> createLevelPropertyNameMap(List<Class<? extends IReferentialEntity>> classes) {
        Map<String, PropertyDescriptor> result = new HashMap<>();

        // Detect level properties, by name
        classes.forEach((clazz) -> {
            PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(clazz);
            Arrays.stream(pds)
                    .filter(propertyDescriptor -> propertyDescriptor.getName().matches("^.*[Ll]evel([A−Z].*)?$"))
                    .findFirst()
                    .ifPresent(propertyDescriptor -> result.put(clazz.getSimpleName(), propertyDescriptor));
        });

        // Other level (not having "level" in id)
        result.put(Pmfm.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Pmfm.class, Pmfm.Fields.PARAMETER));
        result.put(Fraction.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Fraction.class, Fraction.Fields.MATRIX));
        result.put(QualitativeValue.class.getSimpleName(), BeanUtils.getPropertyDescriptor(QualitativeValue.class, QualitativeValue.Fields.PARAMETER));
        result.put(TaxonGroup.class.getSimpleName(), BeanUtils.getPropertyDescriptor(TaxonGroup.class, TaxonGroup.Fields.TAXON_GROUP_TYPE));
        result.put(TaxonName.class.getSimpleName(), BeanUtils.getPropertyDescriptor(TaxonName.class, TaxonName.Fields.TAXONOMIC_LEVEL));
        result.put(Strategy.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Strategy.class, Strategy.Fields.PROGRAM));
        result.put(Metier.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Metier.class, Metier.Fields.GEAR));
        result.put(GroupingLevel.class.getSimpleName(), BeanUtils.getPropertyDescriptor(GroupingLevel.class, GroupingLevel.Fields.GROUPING_CLASSIFICATION));
        result.put(Grouping.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Grouping.class, Grouping.Fields.GROUPING_LEVEL));
        result.put(ExtractionProduct.class.getSimpleName(), BeanUtils.getPropertyDescriptor(ExtractionProduct.class, ExtractionProduct.Fields.PROCESSING_FREQUENCY));
        result.put(ExtractionProductTable.class.getSimpleName(), BeanUtils.getPropertyDescriptor(ExtractionProductTable.class, ExtractionProductTable.Fields.PRODUCT));
        result.put(LocationLevel.class.getSimpleName(), BeanUtils.getPropertyDescriptor(LocationLevel.class, LocationLevel.Fields.LOCATION_CLASSIFICATION));
        result.put(Gear.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Gear.class, Gear.Fields.GEAR_CLASSIFICATION));
        result.put(Program.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Program.class, Program.Fields.GEAR_CLASSIFICATION));
        result.put(Program.class.getSimpleName(), BeanUtils.getPropertyDescriptor(Program.class, Program.Fields.TAXON_GROUP_TYPE));

        return result;
    }


    public static Optional<String> getLevelPropertyName(String entityName) {
        return getLevelPropertyByClass(getEntityClass(entityName))
                .map(PropertyDescriptor::getName);
    }

    public static <T extends Serializable> Class<T> getEntityClass(String entityName) {
        Preconditions.checkNotNull(entityName, "Missing 'entityName' argument");

        // Get entity class from entityName
        Class<? extends Serializable> entityClass = CLASSES_BY_NAME.get(entityName);
        if (entityClass == null) {
            throw new IllegalArgumentException(String.format("Referential entity [%s] not exists", entityName));
        }

        return (Class<T>) entityClass;
    }

    public static Optional<PropertyDescriptor> getLevelProperty(String entityName) {
        return Optional.ofNullable(LEVEL_PROPERTY_BY_CLASS_NAME.get(entityName));
    }

    public static Optional<PropertyDescriptor> getLevelPropertyByClass(Class<? extends IReferentialEntity> entityClass) {
        return getLevelProperty(entityClass.getSimpleName());
    }

    public static Optional<String> getLevelPropertyNameByClass(Class<? extends IReferentialEntity> entityClass) {
        return getLevelPropertyName(entityClass.getSimpleName());
    }
}

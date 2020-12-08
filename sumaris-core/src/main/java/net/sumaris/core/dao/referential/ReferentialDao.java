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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilege;
import net.sumaris.core.model.administration.programStrategy.Strategy;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.*;
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
import net.sumaris.core.model.technical.versionning.SystemVersion;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.*;
import java.util.stream.Collectors;

public interface ReferentialDao {

    List<Class<? extends IReferentialEntity>> REFERENTIAL_CLASSES = ImmutableList.of(
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
                    Parameter.class,
                    Pmfm.class,
                    Matrix.class,
                    Fraction.class,
                    Method.class,
                    QualitativeValue.class,
                    Unit.class,
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
                    OriginItemType.class
            );

    List<Class<? extends IReferentialEntity>> LAST_UPDATE_DATE_CLASSES_EXCLUDES = ImmutableList.of(
            // Product
            ExtractionProduct.class,
            ExtractionProductTable.class,
            // Software
            Software.class,
            // Technical
            SystemVersion.class
    );

    Collection<String> LAST_UPDATE_DATE_ENTITY_NAMES = REFERENTIAL_CLASSES
            .stream().filter(c -> !LAST_UPDATE_DATE_CLASSES_EXCLUDES.contains(c))
            .map(Class::getSimpleName)
            .collect(Collectors.toList());

    ReferentialVO get(String entityName, int id);

    ReferentialVO get(Class<? extends IReferentialEntity> entityClass, int id);

    default Date getLastUpdateDate() {
        return getLastUpdateDate(LAST_UPDATE_DATE_ENTITY_NAMES);
    }

    Date getLastUpdateDate(Collection<String> entityNames);

    List<ReferentialTypeVO> getAllTypes();

    List<ReferentialVO> getAllLevels(String entityName);

    ReferentialVO getLevelById(String entityName, int levelId);

    List<ReferentialVO> findByFilter(String entityName,
                                     IReferentialFilter filter,
                                     int offset,
                                     int size,
                                     String sortAttribute,
                                     SortDirection sortDirection);

    Long countByFilter(final String entityName, IReferentialFilter filter);

    Optional<ReferentialVO> findByUniqueLabel(String entityName, String label);

    <T extends IReferentialEntity> ReferentialVO toVO(T source);

    <T extends IReferentialVO, S extends IReferentialEntity> Optional<T> toTypedVO(S source, Class<T> targetClazz);

    ReferentialVO save(ReferentialVO source);

    void delete(String entityName, int id);

    Long count(String entityName);

    Long countByLevelId(String entityName, Integer... levelIds);

}

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

package net.sumaris.core.dao.data.batch;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.referential.pmfm.ParameterRepository;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.DenormalizedBatch;
import net.sumaris.core.model.data.DenormalizedBatchSortingValue;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.pmfm.*;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Numbers;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchSortingValueVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataRetrievalFailureException;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 09/06/2020.
 */
@Slf4j
public class DenormalizedBatchSortingValueRepositoryImpl
    extends SumarisJpaRepositoryImpl<DenormalizedBatchSortingValue, Integer, DenormalizedBatchSortingValueVO>
    implements DenormalizedBatchSortingValueSpecifications<DenormalizedBatchSortingValue, DenormalizedBatchSortingValueVO> {

    public DenormalizedBatchSortingValueRepositoryImpl(EntityManager entityManager) {
        super(DenormalizedBatchSortingValue.class, entityManager);
    }

    @Override
    public Class<DenormalizedBatchSortingValueVO> getVOClass() {
        return DenormalizedBatchSortingValueVO.class;
    }

    @Override
    public void toVO(DenormalizedBatchSortingValue source, DenormalizedBatchSortingValueVO target, boolean copyIfNull) {
        super.toVO(source, target, copyIfNull);

    }

    @Override
    public void toEntity(DenormalizedBatchSortingValueVO source, DenormalizedBatchSortingValue target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);

        // Pmfm
        Integer pmfmId = source.getPmfmId() != null ? source.getPmfmId() : (source.getPmfm() != null ? source.getPmfm().getId() : null);
        if (pmfmId != null || copyIfNull) {
            if (pmfmId != null) target.setPmfm(getReference(Pmfm.class, pmfmId));
            else target.setPmfm(null);
        }

        // Parameter
        Integer parameterId = source.getParameter() != null ? source.getParameter().getId() : null;
        if (parameterId != null || copyIfNull) {
            if (parameterId != null) target.setParameter(getReference(Parameter.class, parameterId));
            else target.setParameter(null);
        }

        // Qualitative_value
        Integer qvId = source.getQualitativeValue() != null ? source.getQualitativeValue().getId() : null;
        if (qvId != null || copyIfNull) {
            if (qvId != null) target.setQualitativeValue(getReference(QualitativeValue.class, qvId));
            else target.setQualitativeValue(null);
        }

        // Unit
        Integer unitId = source.getUnit() != null ? source.getUnit().getId() : null;
        if (unitId != null || copyIfNull) {
            if (unitId != null) target.setUnit(getReference(Unit.class, unitId));
            else target.setUnit(null);
        }

        // Link to parent
        Integer batchId = source.getBatchId() != null ? source.getBatchId() : (source.getBatch() != null ? source.getBatch().getId() : null);
        if (batchId != null) {
            target.setBatch(getReference(DenormalizedBatch.class, batchId));
        }
    }

    /* -- protected methods -- */
}
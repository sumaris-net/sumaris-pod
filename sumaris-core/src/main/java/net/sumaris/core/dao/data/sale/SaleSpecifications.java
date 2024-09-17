package net.sumaris.core.dao.data.sale;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.Batch;
import net.sumaris.core.model.data.DenormalizedBatch;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.filter.SaleFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SaleSpecifications extends RootDataSpecifications<Sale> {

    String TRIP_ID_PARAM = "tripId";
    String LOCATION_ID_PARAM = "locationId";
    String LANDING_ID_PARAM = "landingId";

    default Specification<Sale> hasTripId(Integer tripId) {
        if (tripId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, TRIP_ID_PARAM);
            return cb.equal(root.get(Sale.Fields.TRIP).get(IEntity.Fields.ID), param);
        }).addBind(TRIP_ID_PARAM, tripId);
    }

    default Specification<Sale> hasLandingId(Integer landingId) {
        if (landingId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, LANDING_ID_PARAM);
            return cb.equal(root.get(Sale.Fields.LANDING).get(IEntity.Fields.ID), param);
        }).addBind(LANDING_ID_PARAM, landingId);
    }

    default Specification<Sale> hasSaleLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, SaleFilterVO.Fields.LOCATION_IDS);
            return Daos.composePath(root, StringUtils.doting(Sale.Fields.SALE_LOCATION, IEntity.Fields.ID)).in(param);
        }).addBind(SaleFilterVO.Fields.LOCATION_IDS, Arrays.asList(locationIds));
    }

    List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sales);

    List<SaleVO> saveAllByLandingId(int landingId, List<SaleVO> sales);

    default Specification<Sale> needBatchDenormalization(Boolean needBatchDenormalization) {
        if (!Boolean.TRUE.equals(needBatchDenormalization)) return null;

        return BindableSpecification.where((root, query, cb) -> {

            Join<Sale, Batch> catchBatch = Daos.composeJoin(root, Sale.Fields.BATCHES, JoinType.INNER);

            // Sub select that return the update to date denormalized catch batch
            Subquery<Integer> subQuery = query.subquery(Integer.class);
            Root<DenormalizedBatch> denormalizedBatchRoot = subQuery.from(DenormalizedBatch.class);
            subQuery.select(denormalizedBatchRoot.get(DenormalizedBatch.Fields.ID));
            subQuery.where(
                    cb.and(
                            // Catch batch
                            cb.isNull(denormalizedBatchRoot.get(DenormalizedBatch.Fields.PARENT)),
                            // Same sale
                            cb.equal(denormalizedBatchRoot.get(DenormalizedBatch.Fields.SALE), root),
                            // Same catch batch
                            cb.equal(denormalizedBatchRoot.get(DenormalizedBatch.Fields.ID), catchBatch.get(Batch.Fields.ID)),
                            // Same date
                            cb.equal(denormalizedBatchRoot.get(DenormalizedBatch.Fields.UPDATE_DATE), catchBatch.get(Batch.Fields.UPDATE_DATE))
                    )
            );

            return cb.and(
                // Get the catch batch (no parent)
                    cb.isNull(catchBatch.get(Batch.Fields.PARENT)),
                    // And without an update to date denormalization
                    cb.not(cb.exists(subQuery))
            );
        });
    }

}

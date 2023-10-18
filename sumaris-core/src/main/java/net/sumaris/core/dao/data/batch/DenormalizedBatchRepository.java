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

import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.data.DenormalizedBatch;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface DenormalizedBatchRepository
        extends SumarisJpaRepository<DenormalizedBatch, Integer, DenormalizedBatchVO>,
    DenormalizedBatchSpecifications<DenormalizedBatch, DenormalizedBatchVO> {

    @Query("select id from DenormalizedBatch where operation.id = ?1")
    Set<Integer> getAllIdByOperationId(int operationId);

    @Query("select id from DenormalizedBatch where sale.id = ?1")
    Set<Integer> getAllIdBySaleId(int saleId);

    @Override
    @Modifying
    @Query("delete from DenormalizedBatch b where b.id in (:ids)")
    void deleteAllById(@Param("ids") Iterable<? extends Integer> ids);
}

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

package net.sumaris.core.service.referential.pmfm;

import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.PmfmVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface PmfmService {

    @Transactional(readOnly = true)
    List<PmfmVO> findByFilter(IReferentialFilter filter, Page page);

    @Transactional(readOnly = true)
    List<PmfmVO> findByFilter(IReferentialFilter filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    @Transactional(readOnly = true)
    Optional<PmfmVO> findByLabel(final String label);

    @Transactional(readOnly = true)
    PmfmVO getByLabel(String label);

    @Transactional(readOnly = true)
    PmfmVO get(int pmfmId);

    PmfmVO save(PmfmVO pmfm);

    @Transactional(readOnly = true)
    boolean isWeightPmfm(int pmfmId);

    @Transactional(readOnly = true)
    boolean isSortingPmfm(int pmfmId);

    @Transactional(readOnly = true)
    boolean isQuantificationPmfm(int pmfmId);

    @Transactional(readOnly = true)
    boolean isCalculatedPmfm(int pmfmId);

    @Transactional(readOnly = true)
    boolean isGearPmfm(int pmfmId);

    @Transactional(readOnly = true)
    boolean isSurveyPmfm(int pmfmId);

    @Transactional(readOnly = true)
    String computeCompleteName(int pmfmId);

    /**
     * @deprecated Find a better way (not using LABEL prefix)
     */
    @Transactional(readOnly = true)
    @Deprecated
    boolean isGearPhysicalPmfm(int pmfmId);

}

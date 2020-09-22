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

import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.PmfmDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.PmfmVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("pmfmService")
public class PmfmServiceImpl implements PmfmService {

    private static final Logger log = LoggerFactory.getLogger(PmfmServiceImpl.class);

    @Autowired
    protected PmfmDao pmfmDao;

    @Autowired
    protected ReferentialDao referentialDao;

    @Override
    public List<PmfmVO> findByFilter(ReferentialFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return referentialDao.streamByFilter(Pmfm.class, filter, offset, size, sortAttribute, sortDirection)
                .map(pmfmDao::toVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PmfmVO> findByLabel(final String label) {
        return pmfmDao.findByLabel(label);
    }

    @Override
    public PmfmVO getByLabel(final String label) {
        return pmfmDao.getByLabel(label);
    }

    @Override
    public PmfmVO get(int pmfmId) {
        return pmfmDao.get(pmfmId);
    }

	@Override
	public PmfmVO save(PmfmVO pmfm) {
		return pmfmDao.save(pmfm);
	}

    @Override
    public boolean isWeightPmfm(int pmfmId) {
        return pmfmDao.hasLabelSuffix(pmfmId, "WEIGHT");
    }

    @Override
    public boolean isSortingPmfm(int pmfmId) {
        return pmfmDao.hasLabelSuffix(pmfmId, "SORTING");
    }

    @Override
    public boolean isQuantificationPmfm(int pmfmId) {
        return pmfmDao.hasLabelSuffix(pmfmId, "QUANTIFICATION");
    }

    @Override
    public boolean isCalculatedPmfm(int pmfmId) {
        return pmfmDao.hasLabelPrefix(pmfmId, "CALCULATED");
    }

    @Override
    public boolean isGearPmfm(int pmfmId) {
        return pmfmDao.hasLabelPrefix(pmfmId, "GEAR") // Required by SFA historical data
            || pmfmDao.hasMatrixId(pmfmId, MatrixEnum.GEAR.getId()); // Required by Ifremer historical data
    }

    @Override
    public boolean isGearPhysicalPmfm(int pmfmId) {
        return pmfmDao.hasLabelPrefix(pmfmId, "GEAR_PHYSICAL");
    }



}

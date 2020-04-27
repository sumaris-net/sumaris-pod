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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.referential.pmfm.PmfmDao;
import net.sumaris.core.vo.referential.PmfmVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

@Service("pmfmService")
public class PmfmServiceImpl implements PmfmService {

    private static final Logger log = LoggerFactory.getLogger(PmfmServiceImpl.class);

    @Autowired
    protected PmfmDao pmfmDao;

    @Override
    public PmfmVO getByLabel(final String label) {
        Preconditions.checkNotNull(label);
        Preconditions.checkArgument(label.trim().length() > 0);
        return pmfmDao.getByLabel(label.trim());
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
        return hasLabelSuffix(pmfmId, "WEIGHT");
    }

    @Override
    public boolean isSortingPmfm(int pmfmId) {
        return hasLabelSuffix(pmfmId, "SORTING");
    }

    @Override
    public boolean isQuantificationPmfm(int pmfmId) {
        return hasLabelSuffix(pmfmId, "QUANTIFICATION");
    }

    @Override
    public boolean isCalculatedPmfm(int pmfmId) {
        return hasLabelPrefix(pmfmId, "CALCULATED");
    }

    @Override
    public boolean isVesselUsePmfm(int pmfmId) {
        return hasLabelPrefix(pmfmId, "VESSEL_USE");
    }

    @Override
    public boolean isGearUsePmfm(int pmfmId) {
        return hasLabelPrefix(pmfmId, "GEAR_USE");
    }

    @Override
    public boolean isGearPhysicalPmfm(int pmfmId) {
        return hasLabelPrefix(pmfmId, "GEAR_PHYSICAL");
    }

    private boolean hasLabelPrefix(int pmfmId, String... labelPrefixes) {
        return Optional.ofNullable(get(pmfmId))
            .map(PmfmVO::getLabel)
            .map(startsWith(labelPrefixes))
            .orElse(false);
    }

    private boolean hasLabelSuffix(int pmfmId, String... labelSuffixes) {
        return Optional.ofNullable(get(pmfmId))
            .map(PmfmVO::getLabel)
            .map(endsWith(labelSuffixes))
            .orElse(false);
    }

    private Function<String, Boolean> startsWith(String... prefixes) {
        return string -> Arrays.stream(prefixes).anyMatch(string::startsWith);
    }

    private Function<String, Boolean> endsWith(String... suffixes) {
        return string -> Arrays.stream(suffixes).anyMatch(string::endsWith);
    }
}

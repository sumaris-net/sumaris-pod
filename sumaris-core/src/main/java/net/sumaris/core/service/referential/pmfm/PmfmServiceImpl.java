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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.pmfm.ParameterRepository;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.pmfm.MatrixEnum;
import net.sumaris.core.model.referential.pmfm.ParameterGroupEnum;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.ParameterVO;
import net.sumaris.core.vo.referential.ParameterValueType;
import net.sumaris.core.vo.referential.PmfmVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("pmfmService")
@Slf4j
public class PmfmServiceImpl implements PmfmService {

    @Autowired
    protected PmfmRepository pmfmRepository;

    @Autowired
    protected ParameterRepository parameterRepository;

    @Override
    public List<PmfmVO> findByFilter(IReferentialFilter filter, Page page) {
        return pmfmRepository.findAll(filter, page, null);
    }

    @Override
    public List<PmfmVO> findByFilter(IReferentialFilter filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return pmfmRepository.findAll(filter, offset, size, sortAttribute, sortDirection, null).getContent();
    }

    @Override
    public Optional<PmfmVO> findByLabel(final String label) {
        return pmfmRepository.findByLabel(label);
    }

    @Override
    public PmfmVO getByLabel(final String label) {
        return pmfmRepository.getByLabel(label);
    }

    @Override
    public PmfmVO get(int pmfmId) {
        return pmfmRepository.get(pmfmId);
    }

	@Override
	public PmfmVO save(PmfmVO pmfm) {
        Preconditions.checkNotNull(pmfm);
        Preconditions.checkNotNull(pmfm.getParameterId());

        // Check Qualitative values coherence
        ParameterVO parameter = parameterRepository.get(pmfm.getParameterId());
        boolean isParameterQualitative = ParameterValueType.fromParameter(parameter).equals(ParameterValueType.QUALITATIVE_VALUE);
        if (isParameterQualitative) {
            // pmfm qualitative values must be present in parameter qualitative values
            if (!ListUtils.emptyIfNull(parameter.getQualitativeValues()).containsAll(ListUtils.emptyIfNull(pmfm.getQualitativeValues())))
                throw new SumarisTechnicalException("The qualitative value list of this pmfm is incoherent with its parameter");
        } else if (CollectionUtils.isNotEmpty(pmfm.getQualitativeValues())) {
            // reset pmfm qualitative values
            pmfm.setQualitativeValues(null);
        }

		return pmfmRepository.save(pmfm);
	}

    @Override
    public boolean isWeightPmfm(int pmfmId) {
        return pmfmRepository.hasLabelSuffix(pmfmId, "WEIGHT");
    }

    @Override
    public boolean isSortingPmfm(int pmfmId) {
        return pmfmRepository.hasLabelSuffix(pmfmId, "SORTING");
    }

    @Override
    public boolean isQuantificationPmfm(int pmfmId) {
        return pmfmRepository.hasLabelSuffix(pmfmId, "QUANTIFICATION");
    }

    @Override
    public boolean isCalculatedPmfm(int pmfmId) {
        return pmfmRepository.hasLabelPrefix(pmfmId, "CALCULATED");
    }

    @Override
    public boolean isGearPmfm(int pmfmId) {
        return pmfmRepository.hasLabelPrefix(pmfmId, "GEAR") // Required by SFA historical data
            || pmfmRepository.hasMatrixId(pmfmId, MatrixEnum.GEAR.getId()); // Required by Ifremer historical data
    }

    @Override
    public boolean isSurveyPmfm(int pmfmId) {
        return pmfmRepository.hasParameterGroupId(pmfmId, ParameterGroupEnum.SURVEY.getId());
    }

    @Override
    public String computeCompleteName(int pmfmId) {
        return pmfmRepository.computeCompleteName(pmfmId);
    }

    @Override
    public boolean isGearPhysicalPmfm(int pmfmId) {
        return pmfmRepository.hasLabelPrefix(pmfmId, "GEAR_PHYSICAL");
    }


}

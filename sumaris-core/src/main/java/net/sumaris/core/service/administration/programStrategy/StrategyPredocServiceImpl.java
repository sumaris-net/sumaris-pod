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

package net.sumaris.core.service.administration.programStrategy;

import net.sumaris.core.dao.administration.programStrategy.StrategyPredocDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.location.LocationClassificationEnum;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("strategyPredocService")
public class StrategyPredocServiceImpl implements StrategyPredocService {

	private static final Logger log = LoggerFactory.getLogger(StrategyPredocServiceImpl.class);

	@Autowired
	protected StrategyPredocDao strategyPredocDao;

	@Override
	public List<ReferentialVO> findStrategiesReferentials(String entityName, int programId, LocationClassificationEnum locationClassification, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return strategyPredocDao.findStrategiesReferentials(entityName, programId, locationClassification, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public List<ReferentialVO> findStrategiesReferentials(String entityName, int programId, LocationClassificationEnum locationClassification, int offset, int size) {
		return findStrategiesReferentials(entityName, programId, locationClassification, offset, size, IItemReferentialEntity.Fields.LABEL, SortDirection.ASC);
	}

	@Override
	public List<ReferentialVO> findStrategiesReferentials(String entityName, int programId, int offset, int size) {
		return findStrategiesReferentials(entityName, programId, null, offset, size, IItemReferentialEntity.Fields.LABEL, SortDirection.ASC);
	}

	@Override
	public List<String> findStrategiesAnalyticReferences(int programId) {
		return strategyPredocDao.findStrategiesAnalyticReferences(programId);
	}

	@Override
	public List<Integer> findStrategiesDepartments(int programId) {
		return strategyPredocDao.findStrategiesDepartments(programId);
	}

	@Override
	public List<Integer> findStrategiesLocations(int programId, LocationClassificationEnum locationClassification) {
		return strategyPredocDao.findStrategiesLocations(programId, locationClassification);
	}

	@Override
	public List<Integer> findStrategiesTaxonNames(int programId) {
		return strategyPredocDao.findStrategiesTaxonNames(programId);
	}

	@Override
	public List<Integer> findStrategiesPmfms(int programId, Integer referenceTaxonId, String field) {
		return strategyPredocDao.findStrategiesPmfms(programId, referenceTaxonId, field);
	}
}

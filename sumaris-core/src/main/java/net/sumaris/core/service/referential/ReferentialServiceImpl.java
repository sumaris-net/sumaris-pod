package net.sumaris.core.service.referential;

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
import net.sumaris.core.dao.referential.BaseRefRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service("referentialService")
public class ReferentialServiceImpl implements ReferentialService {

	private static final Logger log = LoggerFactory.getLogger(ReferentialServiceImpl.class);

	@Autowired
	protected BaseRefRepository baseRefRepository;

	@Override
	public Date getLastUpdateDate() {
		return baseRefRepository.getLastUpdateDate();
	}

	@Override
	public List<ReferentialTypeVO> getAllTypes() {
		return baseRefRepository.getAllTypes();
	}

	@Override
	public ReferentialVO get(String entityName, int id) {
		return baseRefRepository.get(entityName, id);
	}

	@Override
	public ReferentialVO get(Class<? extends IReferentialWithStatusEntity> entityClass, int id) {
		return baseRefRepository.get(entityClass, id);
	}

	@Override
	public List<ReferentialVO> getAllLevels(final String entityName) {
		return baseRefRepository.getAllLevels(entityName);
	}

	@Override
	public ReferentialVO getLevelById(String entityName, int levelId) {
		return baseRefRepository.getLevelById(entityName, levelId);
	}

	@Override
	public List<ReferentialVO> findByFilter(String entityName, ReferentialFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return baseRefRepository.findByFilter(entityName, filter != null ? filter : new ReferentialFilterVO(), offset, size, sortAttribute,
				sortDirection);
	}

	@Override
	public List<ReferentialVO> findByFilter(String entityName, ReferentialFilterVO filter, int offset, int size) {
		return findByFilter(entityName, filter != null ? filter : new ReferentialFilterVO(), offset, size,
				IItemReferentialEntity.Fields.LABEL,
				SortDirection.ASC);
	}

	@Override
	public Long countByFilter(String entityName, ReferentialFilterVO filter) {
		Preconditions.checkNotNull(entityName);
		if (filter == null) {
			return count(entityName);
		}
		return baseRefRepository.countByFilter(entityName, filter);
	}

	@Override
	public ReferentialVO findByUniqueLabel(String entityName, String label) {
		Preconditions.checkNotNull(entityName);
		Preconditions.checkNotNull(label);
		return baseRefRepository.findByUniqueLabel(entityName, label)
			.orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.error.entity.notfoundByLabel", entityName, label)));
	}

	@Override
	public void delete(final String entityName, int id) {
		baseRefRepository.delete(entityName, id);
	}

	@Override
	public void delete(final String entityName, List<Integer> ids) {
		Preconditions.checkNotNull(entityName);
		Preconditions.checkNotNull(ids);

		ids.stream().forEach(id -> delete(entityName, id));
	}

	@Override
	public Long count(String entityName) {
		Preconditions.checkNotNull(entityName);
		return baseRefRepository.count(entityName);
	}

	@Override
	public Long countByLevelId(String entityName, Integer... levelIds) {
		Preconditions.checkNotNull(entityName);
		return baseRefRepository.countByLevelId(entityName, levelIds);
	}

	@Override
	public ReferentialVO save(ReferentialVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getStatusId(), "Missing statusId");
		Preconditions.checkNotNull(source.getEntityName(), "Missing entityName");

		return baseRefRepository.save(source);
	}

	@Override
	public List<ReferentialVO> save(List<ReferentialVO> beans) {
		Preconditions.checkNotNull(beans);

		return beans.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}
}

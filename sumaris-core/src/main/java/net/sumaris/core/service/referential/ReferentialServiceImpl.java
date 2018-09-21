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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("referentialService")
public class ReferentialServiceImpl implements ReferentialService {

	private static final Log log = LogFactory.getLog(ReferentialServiceImpl.class);

	@Autowired
	protected ReferentialDao referentialDao;

	@Override
	public List<ReferentialTypeVO> getAllTypes() {
		return referentialDao.getAllTypes();
	}

	@Override
	public List<ReferentialVO> getAllLevels(final String entityName) {
		return referentialDao.getAllLevels(entityName);
	}

	@Override
	public ReferentialVO getLevelById(String entityName, int levelId) {
		return referentialDao.getLevelById(entityName, levelId);
	}

	@Override
	public List<ReferentialVO> findByFilter(String entityName, ReferentialFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return referentialDao.findByFilter(entityName, filter != null ? filter : new ReferentialFilterVO(), offset, size, sortAttribute,
				sortDirection);
	}

	@Override
	public List<ReferentialVO> findByFilter(String entityName, ReferentialFilterVO filter, int offset, int size) {
		return referentialDao.findByFilter(entityName, filter, offset, size,
				IItemReferentialEntity.PROPERTY_LABEL,
				SortDirection.ASC);
	}

	@Override
	public void delete(final String entityName, int id) {
		referentialDao.delete(entityName, id);
	}

	@Override
	public void delete(final String entityName, List<Integer> ids) {
		Preconditions.checkNotNull(entityName);
		Preconditions.checkNotNull(ids);

		ids.stream().forEach(id -> delete(entityName, id));
	}

	@Override
	public ReferentialVO save(final String entityName, ReferentialVO source) {
		Preconditions.checkNotNull(entityName, "Missing entityName");
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getStatusId(), "Missing statusId");

		source.setEntityName(entityName); // Need by cache eviction
		return referentialDao.save(entityName, source);
	}

	@Override
	public List<ReferentialVO> save(final String entityName, List<ReferentialVO> beans) {
		Preconditions.checkNotNull(entityName);
		Preconditions.checkNotNull(beans);

		return beans.stream()
				.map(b -> save(entityName, b))
				.collect(Collectors.toList());
	}
}

package net.sumaris.core.service.administration;

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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.service.data.ImageAttachmentService;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("departmentService")
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

	@Autowired
	protected DepartmentRepository departmentRepository;

	@Autowired
	protected ImageAttachmentService imageAttachmentService;

	@Override
	public List<DepartmentVO> findByFilter(DepartmentFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return departmentRepository.findAll(filter == null ? new DepartmentFilterVO() : filter, offset, size, sortAttribute, sortDirection, null).getContent();
	}


	@Override
	public DepartmentVO get(int id) {
		return departmentRepository.get(id);
	}

	@Override
	public List<DepartmentVO> getByIds(int... ids) {
		return Arrays.stream(ids)
				.mapToObj(departmentRepository::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Override
	@Cacheable(cacheNames = CacheConfiguration.Names.DEPARTMENT_LOGO_BY_LABEL, key = "#label", unless="#result==null")
	public ImageAttachmentVO getLogoByLabel(@NonNull final String label) {
		Optional<Department> department = Optional.of(departmentRepository.getById(departmentRepository.getByLabel(label).getId()));

		int logoId = department
			.map(Department::getLogo)
			.map(ImageAttachment::getId)
			.orElseThrow(() -> new DataRetrievalFailureException(I18n.t("sumaris.error.department.logo.notFound")));

		return imageAttachmentService.find(logoId, ImageAttachmentFetchOptions.WITH_CONTENT);
	}

	@Override
	public DepartmentVO save(DepartmentVO department) {
		checkValid(department);

		return departmentRepository.save(department);
	}

	@Override
	public void delete(int id) {
		departmentRepository.deleteById(id);
	}

	/* -- protected methods -- */

	protected void checkValid(DepartmentVO department) {
		Preconditions.checkNotNull(department);
		Preconditions.checkNotNull(department.getLabel(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.department.label")));
		Preconditions.checkNotNull(department.getName(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.department.name")));
		Preconditions.checkNotNull(department.getSiteUrl(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.department.siteUrl")));
		Preconditions.checkNotNull(department.getStatusId(), I18n.t("sumaris.error.validation.required", I18n.t("sumaris.model.department.status")));

	}
}

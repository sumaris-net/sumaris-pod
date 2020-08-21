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
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.data.ImageAttachmentDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.DepartmentFilterVO;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("departmentService")
public class DepartmentServiceImpl implements DepartmentService {

	private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

	@Autowired
	protected DepartmentRepository departmentRepository;

	@Autowired
	protected ImageAttachmentDao imageAttachmentDao;

	@Override
	public List<DepartmentVO> findByFilter(DepartmentFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return departmentRepository.findAll(filter == null ? new DepartmentFilterVO() : filter, offset, size, sortAttribute, sortDirection, null).getContent();
	}


	@Override
	public DepartmentVO get(int departmentId) {
		return departmentRepository.get(departmentId);
	}

	@Override
	public List<DepartmentVO> getByIds(int... ids) {
		return Arrays.stream(ids)
				.mapToObj(departmentRepository::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Override
	public ImageAttachmentVO getLogoByLabel(final String label) {
		Preconditions.checkNotNull(label);
		Optional<Department> department = Optional.of(departmentRepository.getOne(departmentRepository.getByLabel(label).getId()));

		Integer logoId = department
			.map(Department::getLogo)
			.map(ImageAttachment::getId)
			.orElseThrow(() -> new DataRetrievalFailureException(I18n.t("sumaris.error.department.logo.notFound")));

		return imageAttachmentDao.get(logoId);
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

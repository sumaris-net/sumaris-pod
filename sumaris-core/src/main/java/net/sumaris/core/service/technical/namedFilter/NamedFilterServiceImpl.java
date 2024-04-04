package net.sumaris.core.service.technical.namedFilter;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.namedFilter.NamedFilterRepository;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFetchOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterFilterVO;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterSaveOptions;
import net.sumaris.core.vo.technical.namedFilter.NamedFilterVO;
import org.springframework.stereotype.Service;

import graphql.com.google.common.base.Preconditions;

import java.util.List;
import java.util.Optional;


@Service("namedFilterService")
@RequiredArgsConstructor
@Slf4j
public class NamedFilterServiceImpl implements NamedFilterService {

    private final NamedFilterRepository namedFilterRepository;

    @Override
    public Optional<NamedFilterVO> findById(int id, NamedFilterFetchOptions fetchOptions) {
        return namedFilterRepository.findById(id, fetchOptions);
    }

    @Override
    public List<NamedFilterVO> findAll(
            NamedFilterFilterVO filter,
            int offset,
            int size,
            String sortAttribute,
            SortDirection sortDirection,
            NamedFilterFetchOptions fetchOptions) {
        return namedFilterRepository.findAll(
                NamedFilterFilterVO.nullToEmpty(filter),
                offset,
                size,
                sortAttribute,
                sortDirection,
                fetchOptions);
    }


    @Override
    public NamedFilterVO save(NamedFilterVO source, NamedFilterSaveOptions options) {
        checkCanSave(source);
        options = NamedFilterSaveOptions.defaultIfEmpty(options);
        return namedFilterRepository.save(source);
    }

    @Override
    public List<NamedFilterVO> saveAll(List<NamedFilterVO> sources, NamedFilterSaveOptions options) {
        return Beans.getStream(sources)
                .map(entity -> save(entity, options))
                .toList();
    }

    @Override
    public void delete(int id) {
        namedFilterRepository.deleteById(id);
    }

    @Override
    public void deleteAll(List<Integer> ids) {
        Beans.getStream(ids)
                .forEach(namedFilterRepository::deleteById);
    }

    /* -- protected methods -- */

    protected void checkCanSave(NamedFilterVO source) {
	    Preconditions.checkArgument(source.getRecorderPersonId() != null
			    || source.getRecorderPerson() != null
			    || source.getRecorderDepartmentId() != null
			    || source.getRecorderDepartment() != null,
			    "Missing recorderPerson or recorderDepartment");
    }

}

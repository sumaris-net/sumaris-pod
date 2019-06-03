package net.sumaris.core.dao.data;

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

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.data.IRootDataEntity;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.filter.IRootDataFilter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Date;
import java.util.List;

public interface IRootDataDao<
        E extends IRootDataEntity<ID>,
        ID extends Integer,
        V extends IRootDataVO<Integer>,
        F extends IRootDataFilter>
{

    //List<V> getAll(int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions);

    List<V> findByFilter(F filter, int offset, int size, String sortAttribute,
                         SortDirection sortDirection, DataFetchOptions fetchOptions);

    Long countByFilter(F filter);

    V get(int id);

    <C> C get(int id, Class<C> targetClass);

    void delete(int id);

    V save(V vo);

    V toVO(E entity);

    default V control(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    default V validate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    default V unvalidate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }
}

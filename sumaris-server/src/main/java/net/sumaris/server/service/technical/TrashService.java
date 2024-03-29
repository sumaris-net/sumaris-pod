package net.sumaris.server.service.technical;

/*-
 * #%L
 * SUMARiS:: Server
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface TrashService {

    <V> Page<V> findAll(String entityName, Pageable pageable, Class<? extends V> clazz);

    <V> List<V> findAll(String entityName, net.sumaris.core.dao.technical.Page page, Class<? extends V> clazz);

    <V> V getById(String entityName, Serializable id, Class<? extends V> clazz);

    void delete(String entityName, Serializable id);

    <V> Optional<V> findById(String entityName, Serializable id, Class<? extends V> clazz);

    long count(String entityName);

}

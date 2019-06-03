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

import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.IValueObject;
import net.sumaris.core.vo.data.DataFetchOptions;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.Date;

@NoRepositoryBean
public interface IEntityConverter<E extends IEntity<? extends Serializable>, V extends IValueObject<? extends Serializable>> {

    V toVO(E source);

    V toVO(E source, DataFetchOptions fetchOptions);

    void toVO(E source, V target, DataFetchOptions fetchOptions, boolean copyIfNull);

    E toEntity(V source);

    void toEntity(V source, E target, boolean copyIfNull);

    V createVO();

    Class<V> getVOClass();
}

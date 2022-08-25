package net.sumaris.core.dao.technical.jpa;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IValueObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@NoRepositoryBean
public interface SumarisJpaRepository<E extends IEntity<ID>, ID extends Serializable, V extends IValueObject<ID>>
    extends JpaRepository<E, ID> {

    V toVO(E source);

    E toEntity(V source);

    void toEntity(V source, E target, boolean copyIfNull);

    V createVO();

    E createEntity();

    V save(V source);

    V save(V source, boolean checkUpdateDate, boolean lockForUpdate);
}

package net.sumaris.core.dao.administration.user;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.dao.referential.ReferentialSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.administration.user.Department;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author peck7 on 20/08/2020.
 */
public interface DepartmentSpecifications extends ReferentialSpecifications<Integer, Department> {

    String LOGO_PARAMETER = "logo";

    default Specification<Department> withLogo(Boolean withLogo) {
        if (!Boolean.TRUE.equals(withLogo)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            return cb.isNotNull(root.get(Department.Fields.LOGO));
        });
    }
}

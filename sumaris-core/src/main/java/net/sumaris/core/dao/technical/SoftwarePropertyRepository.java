package net.sumaris.core.dao.technical;

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

import net.sumaris.core.model.technical.SoftwareProperty;
import net.sumaris.core.vo.technical.PropertyVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("softwareProperty")
public interface SoftwarePropertyRepository extends JpaRepository<SoftwareProperty, Integer> {


    @Query(value="SELECT new net.sumaris.core.vo.technical.PropertyVO(sp.name, sp.label) " +
            "FROM SoftwareProperty sp INNER JOIN sp.software_fk s " +
            "WHERE s.name = ?1")
    List<PropertyVO> propertiesVO(String name) ;


    @Query(value="SELECT sp FROM SoftwareProperty sp WHERE sp.software_fk = '1'")
    List<SoftwareProperty> properties() ;

}

package net.sumaris.core.service;

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

import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.data.TripDao;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.TripVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component("conversionService")
public class ConversionServiceImpl extends GenericConversionService {

    @Autowired
    private TripDao tripDao;

    @Autowired
    private PersonDao personDao;

    @PostConstruct
    private void initConverters() {

        // Entity->VO converters
        addConverter(Trip.class, TripVO.class, tripDao::toTripVO);
        addConverter(Person.class, PersonVO.class, personDao::toPersonVO);
    }
}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.landing.LandingRepository;
import net.sumaris.core.dao.data.observedLocation.ObservedLocationRepository;
import net.sumaris.core.dao.data.operation.OperationRepository;
import net.sumaris.core.dao.data.trip.TripRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.ObservedLocation;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.TripVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;

@Component("conversionService")
public class ConversionServiceImpl extends GenericConversionService {


    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ObservedLocationRepository observedLocationRepository;

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private LandingRepository landingRepository;

    @Autowired
    private PersonRepository personRepository;


    @Autowired
    private ProgramRepository programRepository;

    /**
     * Add Entity->VO converters
     */
    @PostConstruct
    private void initConverters() {

        // Referential (@see ReferentialServiceImpl)

        // Person : use cacheable VO
        addConverter(Person.class, PersonVO.class, p -> personRepository.findVOById(p.getId()).get());

        // Program/Strategy
        addConverter(Program.class, ProgramVO.class, programRepository::toVO);

        // Data
        addConverter(Trip.class, TripVO.class, tripRepository::toVO);

        addConverter(ObservedLocation.class, ObservedLocationVO.class, observedLocationRepository::toVO);
        addConverter(Operation.class, OperationVO.class, operationRepository::toVO);
        addConverter(Landing.class, LandingVO.class, landingRepository::toVO);
    }

}

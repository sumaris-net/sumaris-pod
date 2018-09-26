package net.sumaris.core.service;

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
    void initConverters() {

        // Entity->VO converters
        addConverter(Trip.class, TripVO.class, tripDao::toTripVO);
        addConverter(Person.class, PersonVO.class, personDao::toPersonVO);
    }
}

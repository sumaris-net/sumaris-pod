package net.sumaris.core.service.data;

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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.data.LandingRepository;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.ObservedLocationDao;
import net.sumaris.core.dao.data.OperationGroupDao;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.aggregatedLanding.AggregatedLandingVO;
import net.sumaris.core.vo.data.aggregatedLanding.VesselActivityVO;
import net.sumaris.core.vo.filter.AggregatedLandingFilterVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("aggregatedLandingService")
public class AggregatedLandingServiceImpl implements AggregatedLandingService {

    private static final Logger log = LoggerFactory.getLogger(AggregatedLandingServiceImpl.class);

    private final ObservedLocationDao observedLocationDao;
    private final LandingRepository landingRepository;
    private final OperationGroupDao operationGroupDao;
    private final MeasurementDao measurementDao;

    public AggregatedLandingServiceImpl(ObservedLocationDao observedLocationDao,
                                        LandingRepository landingRepository,
                                        OperationGroupDao operationGroupDao,
                                        MeasurementDao measurementDao) {
        this.observedLocationDao = observedLocationDao;
        this.landingRepository = landingRepository;
        this.operationGroupDao = operationGroupDao;
        this.measurementDao = measurementDao;
    }

    @Override
    public List<AggregatedLandingVO> findAll(AggregatedLandingFilterVO filter) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getStartDate());
        Preconditions.checkNotNull(filter.getEndDate());
        Preconditions.checkNotNull(filter.getLocationId());
        Preconditions.checkArgument(StringUtils.isNotBlank(filter.getProgramLabel()));

        final Date startDate = Dates.resetTime(filter.getStartDate());
        final Date endDate = Dates.lastSecondOfTheDay(filter.getEndDate());

        List<AggregatedLandingVO> result = new ArrayList<>();
        DataFetchOptions defaultFetchOption = DataFetchOptions.builder()
            .withRecorderDepartment(false)
            .withRecorderPerson(false)
            .withObservers(false)
            .withChildrenEntities(false)
            .build();

        // Get observations
        List<ObservedLocationVO> observedLocations = observedLocationDao.findByFilter(
            ObservedLocationFilterVO.builder()
                .programLabel(filter.getProgramLabel())
                .locationId(filter.getLocationId())
                .startDate(startDate)
                .endDate(endDate)
                .build(),
            0, 1000, null, null,
            defaultFetchOption);

        Map<VesselSnapshotVO, Map<Date, List<LandingVO>>> landingsByBateByVessel = new HashMap<>();

        observedLocations.forEach(observedLocation -> {

            List<LandingVO> landings = landingRepository.findAll(LandingFilterVO.builder()
                    .observedLocationId(observedLocation.getId())
                    .build(),
                defaultFetchOption);

            landings.stream()
                .filter(landing -> landing.getId() != null)
                .forEach(landing -> {
                    if (landing.getDateTime() == null)
                        throw new SumarisTechnicalException(String.format("The landing date is missing for landing id=%s", landing.getId()));

                    Map<Date, List<LandingVO>> landingsByDate = landingsByBateByVessel.computeIfAbsent(landing.getVesselSnapshot(), x -> new HashMap<>());
                    landingsByDate.computeIfAbsent(Dates.resetTime(landing.getDateTime()), x -> new ArrayList<>()).add(landing);
                });
        });

        // Build aggregated landings
        landingsByBateByVessel.keySet().forEach(vessel -> {

            AggregatedLandingVO aggregatedLanding = new AggregatedLandingVO();
            aggregatedLanding.setVesselSnapshot(vessel);
            result.add(aggregatedLanding);
            Map<Date, List<LandingVO>> landingsByDate = landingsByBateByVessel.get(vessel);

            // Iterate days
            landingsByDate.keySet().forEach(date -> landingsByDate.get(date).forEach(landing -> {

                VesselActivityVO activity = new VesselActivityVO();
                activity.setDate(date);
                activity.setRankOrder(landing.getRankOrderOnVessel());
                activity.setComments(landing.getComments());

                // Add measurements
                activity.setMeasurementValues(measurementDao.getLandingMeasurementsMap(landing.getId()));

                // Add metier activity
                if (landing.getTripId() != null) {
                    activity.setTripId(landing.getTripId());

                    List<OperationGroupVO> operationGroups = operationGroupDao.getAllByTripId(landing.getTripId());
                    operationGroups.forEach(operationGroup -> {

                        activity.getMetiers().add(operationGroup.getMetier());

                        // Set comments from operation
                        if (StringUtils.isNotBlank(operationGroup.getComments())) {
                            if (StringUtils.isNotBlank(activity.getComments())) {
                                activity.setComments(activity.getComments() + "\n" + operationGroup.getComments());
                            } else {
                                activity.setComments(operationGroup.getComments());
                            }
                        }
                    });
                }

                // Add this activity
                aggregatedLanding.getVesselActivities().add(activity);

            }));

        });

        return result;
    }

    @Override
    public List<AggregatedLandingVO> saveAllByObservedLocationId(int observedLocationId, List<AggregatedLandingVO> data) {
        return null;
    }




    /* protected methods */

}

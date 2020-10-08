package net.sumaris.core.dao.data.operation;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.data.DataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Operation;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.vo.data.OperationGroupVO;
import net.sumaris.core.vo.filter.OperationGroupFilterVO;
import net.sumaris.core.vo.referential.MetierVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface OperationGroupSpecifications
    extends DataSpecifications<Operation> {

    String TRIP_ID_PARAM = "tripId";

    default Specification<Operation> filter(OperationGroupFilterVO filter) {
        Preconditions.checkNotNull(filter);
        Preconditions.checkNotNull(filter.getTripId());
        BindableSpecification<Operation> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get(Operation.Fields.RANK_ORDER_ON_PERIOD))); // Default sort
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            if (filter.isOnlyUndefined()) {
                return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param),
                    criteriaBuilder.equal(root.get(Operation.Fields.START_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.DEPARTURE_DATE_TIME)),
                    criteriaBuilder.equal(root.get(Operation.Fields.END_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.RETURN_DATE_TIME))
                );
            } else if (filter.isOnlyDefined()) {
                return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param),
                    criteriaBuilder.notEqual(root.get(Operation.Fields.START_DATE_TIME), root.get(Operation.Fields.TRIP).get(Trip.Fields.DEPARTURE_DATE_TIME))
                );
            } else {
                return criteriaBuilder.equal(root.get(Operation.Fields.TRIP).get(IEntity.Fields.ID), param);
            }
        });
        specification.addBind(TRIP_ID_PARAM, filter.getTripId());
        return specification;
    }

    List<OperationGroupVO> saveAllByTripId(int tripId, List<OperationGroupVO> operationGroups);

    void updateUndefinedOperationDates(int tripId, Date startDate, Date endDate);

    /**
     * Get metier ( = operations with same start and end date as trip)
     *
     * @param tripId trip id
     * @return metiers of trip
     */
    List<MetierVO> getMetiersByTripId(int tripId);

    List<MetierVO> saveMetiersByTripId(int tripId, List<MetierVO> metiers);
}

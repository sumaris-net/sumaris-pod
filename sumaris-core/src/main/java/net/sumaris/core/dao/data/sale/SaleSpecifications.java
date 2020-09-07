package net.sumaris.core.dao.data.sale;

import net.sumaris.core.dao.data.RootDataSpecifications;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.vo.data.SaleVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public interface SaleSpecifications extends RootDataSpecifications<Sale> {

    String TRIP_ID_PARAM = "tripId";

    default Specification<Sale> hasTripId(Integer tripId) {
        BindableSpecification<Sale> specification = BindableSpecification.where((root, query, criteriaBuilder) -> {
            ParameterExpression<Integer> param = criteriaBuilder.parameter(Integer.class, TRIP_ID_PARAM);
            return criteriaBuilder.or(
                criteriaBuilder.isNull(param),
                criteriaBuilder.equal(root.get(Sale.Fields.TRIP).get(IEntity.Fields.ID), param)
            );
        });
        specification.addBind(TRIP_ID_PARAM, tripId);
        return specification;
    }

    List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sales);

}

package net.sumaris.core.dao.technical.extraction;

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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.vo.technical.extraction.ExtractionProductSaveOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.ParameterExpression;
import java.util.List;

/**
 * @author peck7 on 21/08/2020.
 */
public interface ExtractionProductSpecifications {

    String DEPARTMENT_ID_PARAM = "departmentId";
    String PERSON_ID_ID_PARAM = "personId";

    default Specification<ExtractionProduct> withRecorderDepartmentId(Integer departmentId) {
        if (departmentId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> parameter = cb.parameter(Integer.class, DEPARTMENT_ID_PARAM);
            return cb.equal(root.get(ExtractionProduct.Fields.RECORDER_DEPARTMENT).get(IEntity.Fields.ID), parameter);
        }).addBind(DEPARTMENT_ID_PARAM, departmentId);
    }


    default Specification<ExtractionProduct> withRecorderPersonIdOrPublic(Integer personId) {
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> parameter = cb.parameter(Integer.class, PERSON_ID_ID_PARAM);
            return cb.or(
                    cb.isNull(parameter),
                    // ENABLE = public extraction type
                    cb.equal(root.get(ExtractionProduct.Fields.STATUS).get(IEntity.Fields.ID), StatusEnum.ENABLE.getId()),
                    // TEMPORARY = private extraction
                    cb.and(
                        cb.equal(root.get(ExtractionProduct.Fields.STATUS).get(IEntity.Fields.ID), StatusEnum.TEMPORARY.getId()),
                        cb.equal(root.get(ExtractionProduct.Fields.RECORDER_PERSON).get(IEntity.Fields.ID), parameter)
                    )
            );
        }).addBind(PERSON_ID_ID_PARAM, personId);
    }

    default Specification<ExtractionProduct> isSpatial(Boolean isSpatial) {
        if (isSpatial == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Boolean> parameter = cb.parameter(Boolean.class, ExtractionProduct.Fields.IS_SPATIAL);
            return cb.equal(root.get(ExtractionProduct.Fields.IS_SPATIAL), parameter);
        }).addBind(ExtractionProduct.Fields.IS_SPATIAL, isSpatial);
    }

    default Specification<ExtractionProduct> withParentId(Integer parentId) {
        if (parentId == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> parameter = cb.parameter(Integer.class, ExtractionProduct.Fields.PARENT);
            return cb.equal(root.get(ExtractionProduct.Fields.PARENT).get(ExtractionProduct.Fields.ID), parameter);
        }).addBind(ExtractionProduct.Fields.PARENT, parentId);
    }

    ExtractionProductVO save(ExtractionProductVO source, ExtractionProductSaveOptions saveOptions);

    List<ExtractionTableColumnVO> getColumnsByIdAndTableLabel(Integer id, String tableLabel);

    void dropTable(String tableName);
}

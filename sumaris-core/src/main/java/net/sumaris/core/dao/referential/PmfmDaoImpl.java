package net.sumaris.core.dao.referential;

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

import net.sumaris.core.util.Beans;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.pmfm.Method;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.vo.referential.ParameterValueType;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository("pmfmDao")
public class PmfmDaoImpl extends HibernateDaoSupport implements PmfmDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(PmfmDaoImpl.class);

    @Autowired
    private ReferentialDao referentialDao;

    public int unitIdNone;

    @PostConstruct
    protected void init() {
        this.unitIdNone = config.getUnitIdNone();
    }

    @Override
    public PmfmVO getByLabel(final String label) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Pmfm> query = builder.createQuery(Pmfm.class);
        Root<Pmfm> root = query.from(Pmfm.class);

        ParameterExpression<String> labelParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(Pmfm.PROPERTY_LABEL), labelParam));

        TypedQuery<Pmfm> q = getEntityManager().createQuery(query)
                .setParameter(labelParam, label);
        return toPmfmVO(q.getSingleResult());
    }

    @Override
    public PmfmVO get(final int pmfmId) {
        return toPmfmVO(get(Pmfm.class, pmfmId));
    }

    @Override
    public PmfmVO toPmfmVO(Pmfm source) {
        if (source == null) return null;

        PmfmVO target = new PmfmVO();

        Beans.copyProperties(source, target);

        // Parameter name
        Parameter parameter = source.getParameter();
        target.setName(parameter.getName());

        // Parameter type
        ParameterValueType type = ParameterValueType.fromPmfm(source);
        target.setType(type.name().toLowerCase());

        // Method: copy isEstimated, isCalculated
        Method method = source.getMethod();
        if (method != null) {
            target.setIsCalculated(method.getIsCalculated());
            target.setIsEstimated(method.getIsEstimated());
        }

        // Unit symbol
        if (source.getUnit() != null && source.getUnit().getId().intValue() != unitIdNone) {
            target.setUnit(source.getUnit().getLabel());
        }

        // Qualitative values
        if (CollectionUtils.isNotEmpty(parameter.getQualitativeValues())) {
            List<ReferentialVO> qualitativeValues = parameter.getQualitativeValues()
                    .stream()
                    .map(referentialDao::toReferentialVO)
                    .collect(Collectors.toList());
            target.setQualitativeValues(qualitativeValues);
        }

        return target;
    }
}

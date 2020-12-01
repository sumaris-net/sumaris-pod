/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.referential;

import net.sumaris.core.dao.referential.ReferentialExternalDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("ReferentialExternalService")
public class ReferentialExternalServiceImpl implements ReferentialExternalService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialExternalServiceImpl.class);

    @Autowired
    protected ReferentialExternalDao referentialExternalDao;

    @Override
    public List<ReferentialVO> findAnalyticReferencesByFilter(String urlStr, String authStr, ReferentialFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return referentialExternalDao.findAnalyticReferencesByFilter(urlStr, authStr, filter != null ? filter : new ReferentialFilterVO(), offset, size, sortAttribute,
                sortDirection);
    }

    @Override
    public List<ReferentialVO> findAnalyticReferencesByFilter(String urlStr, String authStr, ReferentialFilterVO filter, int offset, int size) {
        return findAnalyticReferencesByFilter(urlStr, authStr, filter != null ? filter : new ReferentialFilterVO(), offset, size,
                IItemReferentialEntity.Fields.LABEL,
                SortDirection.ASC);
    }
}

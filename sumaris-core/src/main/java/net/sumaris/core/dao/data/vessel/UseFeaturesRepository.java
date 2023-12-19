package net.sumaris.core.dao.data.vessel;

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

import net.sumaris.core.dao.data.DataRepository;
import net.sumaris.core.dao.technical.jpa.IFetchOptions;
import net.sumaris.core.model.data.GearUseFeatures;
import net.sumaris.core.model.data.IUseFeaturesEntity;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import net.sumaris.core.vo.data.IDataFetchOptions;
import net.sumaris.core.vo.data.IUseFeaturesVO;
import net.sumaris.core.vo.filter.GearUseFeaturesFilterVO;
import net.sumaris.core.vo.filter.IDataFilter;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface UseFeaturesRepository<E extends IUseFeaturesEntity, V extends IUseFeaturesVO, F extends IDataFilter, O extends IDataFetchOptions>
    extends DataRepository<E, V, F, O> {


}

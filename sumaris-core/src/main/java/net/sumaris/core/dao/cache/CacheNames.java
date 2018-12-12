package net.sumaris.core.dao.cache;

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

public interface CacheNames {

    String DEPARTMENT_BY_ID = "net.sumaris.core.dao.administration.user.departmentByIdCache";
    String DEPARTMENT_BY_LABEL = "net.sumaris.core.dao.administration.user.departmentByLabelCache";
    String PERSON_BY_ID = "net.sumaris.core.dao.administration.user.personByIdCache";
    String PERSON_BY_PUBKEY= "net.sumaris.core.dao.administration.user.personByPubkeyCache";
    String REFERENTIAL_TYPES = "net.sumaris.core.dao.referential.allTypesCache";

    String PROGRAM_BY_LABEL= "net.sumaris.core.dao.administration.programStrategy.programBylabelCache";

    String PMFM_BY_ID= "net.sumaris.core.dao.referential.pmfmByIdCache";
    String PMFM_BY_PROGRAM_ID= "net.sumaris.core.dao.administration.programStrategy.pmfmByProgramIdCache";

    String QUERY_CACHE_NAME = "org.hibernate.cache.spi.QueryResultsRegion";
    String TIMESTAMPS_REGION_CACHE_NAME = "org.hibernate.cache.spi.TimestampsRegion";
}

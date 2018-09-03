package net.sumaris.core.dao.cache;

public interface CacheNames {

    String DEPARTMENT_BY_ID = "net.sumaris.core.dao.administration.user.departmentByIdCache";
    String PERSON_BY_ID = "net.sumaris.core.dao.administration.user.personByIdCache";
    String REFERENTIAL_TYPES = "net.sumaris.core.dao.referential.allTypesCache";

    String PROGRAM_BY_LABEL= "net.sumaris.core.dao.administration.programStrategy.programBylabelCache";
    String PMFM_BY_PROGRAM_ID= "net.sumaris.core.dao.administration.programStrategy.pmfmByProgramIdCache";

    String PMFM_BY_ID= "net.sumaris.core.dao.referential.pmfmByIdCache";


    String QUERY_CACHE_NAME = "org.hibernate.cache.spi.QueryResultsRegion";
    String TIMESTAMPS_REGION_CACHE_NAME = "org.hibernate.cache.spi.TimestampsRegion";
}

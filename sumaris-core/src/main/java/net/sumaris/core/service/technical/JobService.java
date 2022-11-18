package net.sumaris.core.service.technical;



import io.leangen.graphql.annotations.GraphQLArgument;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface JobService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean updateProcessingTypes();

    JobVO save(JobVO source);

    @Transactional(readOnly = true)
    JobVO get(int id);

    @Transactional(readOnly = true)
    Optional<JobVO> findById(int id);

    @Transactional(readOnly = true)
    List<JobVO> findAll(@GraphQLArgument(name = "filter") JobFilterVO filter);

    @Transactional(readOnly = true)
    Page<JobVO> findAll(@GraphQLArgument(name = "filter") JobFilterVO filter, Pageable page);
}

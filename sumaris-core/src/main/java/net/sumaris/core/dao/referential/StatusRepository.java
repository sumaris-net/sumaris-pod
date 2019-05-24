package net.sumaris.core.dao.referential;

import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("statusRepository")
public interface StatusRepository extends JpaRepository<Status, Integer> {


    default Status getEnableStatus() {
        return getOne(StatusEnum.ENABLE.getId());
    }
}
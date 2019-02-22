package net.sumaris.core.dao.technical;

import net.sumaris.core.model.technical.Software;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository("softwareRepository")
public interface SoftwareRepository extends JpaRepository<Software, Integer> {

    @Query(value="FROM Software WHERE label = ?1")
    Software getSoftware(String label) ;
}
package net.sumaris.core.service.referential.taxon;

import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.schema.DatabaseSchemaService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.TaxonGroupVO;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service("taxonGroupService")
public class TaxonGroupServiceImpl implements TaxonGroupService {

    private static final Logger log = LoggerFactory.getLogger(TaxonGroupServiceImpl.class);

    @Autowired
    protected TaxonGroupRepository taxonGroupRepository;

    @Autowired
    protected TaxonGroupService self;

    @Autowired
    protected DatabaseSchemaService databaseSchemaService;

    @PostConstruct
    protected void afterPropertiesSet() {

        // Check version
        Version minVersion = VersionBuilder.create("0.15.0").build();
        if (minVersion.after(databaseSchemaService.getDbVersion())) {
            log.info("/!\\ Skipping taxon group hierarchy update, beacause database schema version < 0.15.0. Please restart after schema update.");
        }

        // Check fill hierarchy is need
        else {
            //if (taxonGroupRepository.countTaxonGroupHierarchy() == 0l) {
                self.updateTaxonGroupHierarchies();
            //}
        }
    }

    @Override
    public void updateTaxonGroupHierarchies() {

        taxonGroupRepository.updateTaxonGroupHierarchies();
    }

    @Override
    public List<TaxonGroupVO> findTargetSpeciesByFilter(ReferentialFilterVO filter,
                                           int offset,
                                           int size,
                                           String sortAttribute,
                                           SortDirection sortDirection) {
        return taxonGroupRepository.findTargetSpeciesByFilter(filter, offset, size, sortAttribute, sortDirection);
    }
}

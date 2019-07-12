package net.sumaris.core.service.referential.taxon;

import net.sumaris.core.dao.referential.taxon.TaxonGroupRepository;
import net.sumaris.core.service.schema.DatabaseSchemaService;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

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

        // Check fill hierarchy is need
        if (taxonGroupRepository.countTaxonGroupHierarchy() == 0l) {

            // Check version
            Version minVersion = VersionBuilder.create("0.15.0").build();
            if (!databaseSchemaService.getDbVersion().afterOrEquals(minVersion)) {
                log.info("Skipping taxon group hierarchy update: Database schema version < [0.15.0]");
            }
            else {
                // Make sure taxon group hierarchies are well init
                self.updateTaxonGroupHierarchies();
            }
        }
    }

    @Override
    public void updateTaxonGroupHierarchies() {

        taxonGroupRepository.updateTaxonGroupHierarchies();
    }

}

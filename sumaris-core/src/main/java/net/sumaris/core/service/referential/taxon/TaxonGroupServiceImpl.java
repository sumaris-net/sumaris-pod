package net.sumaris.core.service.referential.taxon;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
        Version dbVersion = databaseSchemaService.getDbVersion();

        if (dbVersion == null) {
            log.info("/!\\ Skipping taxon group hierarchy update, because database schema version is unknown. Please restart after schema update.");
        }

        else if (dbVersion.before(minVersion)) {
            log.info("/!\\ Skipping taxon group hierarchy update, because database schema version < 0.15.0. Please restart after schema update.");
        }

        // Fill taxon group hierarchy
        else {
            self.updateTaxonGroupHierarchies();
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

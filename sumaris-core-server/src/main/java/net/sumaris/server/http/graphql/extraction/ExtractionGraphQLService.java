package net.sumaris.server.http.graphql.data;

/*-
 * #%L
 * SUMARiS:: Server
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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.extraction.ExtractionService;
import net.sumaris.core.vo.extraction.ExtractionTypeVO;
import net.sumaris.core.vo.extraction.ExtractionFilterVO;
import net.sumaris.core.vo.extraction.ExtractionResultVO;
import net.sumaris.server.http.security.IsUser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ExtractionGraphQLService {

    private static final Log log = LogFactory.getLog(DataGraphQLService.class);

    @Autowired
    private ExtractionService extractionService;

    /* -- Extraction / table -- */

    @GraphQLQuery(name = "extractionTypes", description = "Get all available extraction types")
    @Transactional(readOnly = true)
    @IsUser
    public List<ExtractionTypeVO> getAllExtractionTypes() {
        return extractionService.getAllTypes();
    }

    @GraphQLQuery(name = "extraction", description = "Extract data")
    @Transactional(readOnly = true)
    @IsUser
    public ExtractionResultVO getExtractionRows(@GraphQLArgument(name = "type") ExtractionTypeVO type,
                                               @GraphQLArgument(name = "filter") ExtractionFilterVO filter,
                                               @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                               @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                               @GraphQLArgument(name = "sortBy") String sort,
                                               @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction
    ) {
        return extractionService.getRows(type, filter, offset, size, sort, SortDirection.valueOf(direction.toUpperCase()));
    }

}

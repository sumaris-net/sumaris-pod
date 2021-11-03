/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.server.http.graphql.technical;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.service.technical.SoftwareService;
import net.sumaris.core.vo.technical.SoftwareVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@GraphQLApi
@Slf4j
public class SoftwareGraphQLService {

    @Autowired
    private SoftwareService softwareService;

    @GraphQLQuery(name = "software", description = "A software config")
    @IsAdmin
    @Transactional(readOnly = true)
    public SoftwareVO getSoftware(
        @GraphQLArgument(name = "id") Integer id,
        @GraphQLArgument(name = "label") String label
    ) {
        Preconditions.checkArgument(id != null || label != null);

        if (id != null) {
            return softwareService.get(id);
        }

        return softwareService.getByLabel(label);
    }

    @GraphQLMutation(name = "saveSoftware", description = "Save a software configuration")
    @IsAdmin
    public SoftwareVO saveSoftware(@GraphQLArgument(name = "software") SoftwareVO software) {
        return softwareService.save(software);
    }
}

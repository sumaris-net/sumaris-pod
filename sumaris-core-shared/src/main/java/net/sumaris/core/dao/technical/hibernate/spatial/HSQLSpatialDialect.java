package net.sumaris.core.dao.technical.hibernate.spatial;

/*-
 * #%L
 * SUMARiS:: Core shared
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

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;

/**
 * @author peck7 on 16/10/2019.
 */
public class HSQLSpatialDialect extends HSQLDialect {

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        // Add spatial type bound to a string
        typeContributions.contributeType(new GeolatteGeometryType(LongVarcharTypeDescriptor.INSTANCE));
        typeContributions.contributeType(new JTSGeometryType(LongVarcharTypeDescriptor.INSTANCE));

        typeContributions.contributeJavaTypeDescriptor(GeolatteGeometryJavaTypeDescriptor.INSTANCE);
        typeContributions.contributeJavaTypeDescriptor(JTSGeometryJavaTypeDescriptor.INSTANCE);
    }
}

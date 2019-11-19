package net.sumaris.core.dao.technical.hibernate.spatial;

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

package net.sumaris.core.dao.technical.hibernate.spatial;

import com.vividsolutions.jts.geom.Geometry;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class GeometryStringType extends AbstractSingleColumnStandardBasicType<Geometry> {

    public static final String TYPE = "net.sumaris.core.dao.technical.hibernate.spatial.GeometryStringType";

    public static final GeometryStringType INSTANCE = new GeometryStringType();

    public GeometryStringType() {
        super(LongVarcharTypeDescriptor.INSTANCE, GeometryStringJavaDescriptor.INSTANCE);
    }

    @Override
    public String getName() {
        return "GeometryString";
    }
}
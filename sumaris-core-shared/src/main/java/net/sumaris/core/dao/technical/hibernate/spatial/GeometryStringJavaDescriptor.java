package net.sumaris.core.dao.technical.hibernate.spatial;

import com.vividsolutions.jts.geom.Geometry;
import net.sumaris.core.util.Geometries;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

public class GeometryStringJavaDescriptor extends AbstractTypeDescriptor<Geometry> {
 
    public static final GeometryStringJavaDescriptor INSTANCE =
      new GeometryStringJavaDescriptor();
 
    public GeometryStringJavaDescriptor() {
        super(Geometry.class, ImmutableMutabilityPlan.INSTANCE);
    }

    @Override
    public Geometry fromString(String wktString) {
        return Geometries.getGeometry(wktString);
    }

    @Override
    public <X> X unwrap(Geometry value, Class<X> type, WrapperOptions wrapperOptions) {
        if (value == null)
            return null;

        if (String.class.isAssignableFrom(type))
            return (X) Geometries.getWKTString(value);

        throw unknownUnwrap(type);
    }

    @Override
    public <X> Geometry wrap(X value, WrapperOptions wrapperOptions) {
        if (value == null)
            return null;

        if(String.class.isInstance(value))
            return Geometries.getGeometry((String) value);

        throw unknownWrap(value.getClass());
    }

    @Override
    public String toString(Geometry geometry) {
        return Geometries.getWKTString(geometry);
    }
}
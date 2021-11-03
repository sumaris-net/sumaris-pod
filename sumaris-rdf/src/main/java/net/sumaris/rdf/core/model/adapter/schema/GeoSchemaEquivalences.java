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

package net.sumaris.rdf.core.model.adapter.schema;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationArea;
import net.sumaris.core.model.referential.location.LocationLine;
import net.sumaris.core.model.referential.location.LocationPoint;
import net.sumaris.rdf.core.config.RdfConfiguration;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("geoSchemaEquivalences")
@ConditionalOnBean({RdfConfiguration.class})
@ConditionalOnProperty(
        prefix = "rdf.equivalences",
        name = {"geo.enabled"},
        matchIfMissing = true)
@Slf4j
public class GeoSchemaEquivalences extends AbstractSchemaVisitor {

    @Override
    public void visitModel(Model model, String ns, String schemaUri) {
        if (log.isDebugEnabled()) log.debug("Adding {{}} equivalences to {{}}...", org.w3.GEO.PREFIX, schemaUri);
    }

    @Override
    public void visitClass(Model model, Resource ontClass, Class clazz) {
        String classUri = ontClass.getURI();

        // Location
        if (clazz == Location.class) {
            // = SpatialThing
            ontClass.addProperty(equivalentClass, org.w3.GEO.WGS84Pos.SpatialThing);
        }

        // Location Area
        else if (clazz == LocationPoint.class) {
            // = Geometry
            model.getResource(classUri + "#" + LocationArea.Fields.POSITION)
                    .addProperty(equivalentClass, GEO.NAMESPACE + "Geometry");
        }
        else if (clazz == LocationLine.class) {
            // = Geometry + LineString
            model.getResource(classUri + "#" + LocationArea.Fields.POSITION)
                    .addProperty(equivalentClass, GEO.NAMESPACE + "Geometry")
                    .addProperty(equivalentClass, "http://www.opengis.net/ont/sf#LineString");
        }
        else if (clazz == LocationArea.class) {
            // = Geometry + Polygonal surface
            model.getResource(classUri + "#" + LocationArea.Fields.POSITION)
                    .addProperty(equivalentClass, GEO.NAMESPACE + "Geometry")
                    .addProperty(equivalentClass, "http://www.opengis.net/ont/sf#PolyhedralSurface");
        }
    }
}

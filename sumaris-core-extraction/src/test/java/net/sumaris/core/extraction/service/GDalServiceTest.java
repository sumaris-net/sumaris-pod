package net.sumaris.core.extraction.service;

import net.sumaris.core.extraction.dao.DatabaseResource;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Ignore
public class GDalServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void convertToShp() {

        File geoJsonFile = new File("/src/test/resources/geojson-test.json");

        // TODO: try to access to Gdal ogr

    }
}

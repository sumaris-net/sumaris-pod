package net.sumaris.core.util;

import com.vividsolutions.jts.util.Assert;
import org.junit.Test;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class StringUtilsTest {

    @Test
    public void underscoreToChangeCase() {
        //Assert.equals("expectedColumnName", StringUtils.underscoreToChangeCase("EXPECTED_COLUMN_NAME"));

        Assert.equals("aBC", StringUtils.underscoreToChangeCase("A_B_C"));

        Assert.equals("aB", StringUtils.underscoreToChangeCase("A_B_"));

        Assert.equals("abCd", StringUtils.underscoreToChangeCase("_AB_CD"));
    }
}

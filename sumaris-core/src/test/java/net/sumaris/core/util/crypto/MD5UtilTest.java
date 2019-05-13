package net.sumaris.core.util.crypto;

import org.junit.Assert;
import org.junit.Test;

public class MD5UtilTest {

    @Test
    public void md5Hex() {
        String emailMd5 = MD5Util.md5Hex("demo@sumaris.net");
        Assert.assertEquals("2c4f83386923812f818a88ede88ce334", emailMd5);

        emailMd5 = MD5Util.md5Hex("obs@sumaris.net");
        Assert.assertEquals("67655fe01f8693efeea27497939394d7", emailMd5);

        emailMd5 = MD5Util.md5Hex("disable@sumaris.net");
        Assert.assertEquals("cf8bdc002a99cbc2a5f1a891de7194cf", emailMd5);


    }




}

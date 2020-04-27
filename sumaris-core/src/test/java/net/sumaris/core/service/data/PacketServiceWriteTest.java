package net.sumaris.core.service.data;

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.PacketCompositionVO;
import net.sumaris.core.vo.data.PacketVO;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author peck7 on 09/04/2020.
 */
@TestPropertySource(locations = "classpath:sumaris-core-test-oracle.properties")
public class PacketServiceWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

    @Autowired
    private PacketService packetService;

    /**
     * Test only for SFA Oracle database
     * TODO write a test with test data
     */
    @Ignore
    @Test
    public void getAllByOperationId() {

        List<PacketVO> packets = packetService.getAllByOperationId(230229);
        assertNotNull(packets);
        assertEquals(2, packets.size());
        {
            PacketVO packet = packets.stream().filter(packetVO -> packetVO.getRankOrder() == 1).findFirst().orElse(null);
            assertNotNull(packet);
            assertNotNull(packet.getNumber());
            assertEquals(6, packet.getNumber().intValue());
            assertNotNull(packet.getWeight());
            assertEquals(20.4, packet.getWeight(), 0);
            assertNotNull(packet.getSampledWeights());
            assertArrayEquals(new Double[]{3.38, 3.42, 3.4}, packet.getSampledWeights().toArray());
            // composition
            assertNotNull(packet.getComposition());
            assertEquals(1, packet.getComposition().size());
            PacketCompositionVO composition = packet.getComposition().get(0);
            assertNotNull(composition);
            assertNotNull(composition.getTaxonGroup());
            assertEquals(414, composition.getTaxonGroup().getId().intValue());
            assertNotNull(composition.getRatios());
            assertArrayEquals(new Integer[]{100, 100, 100}, composition.getRatios().toArray());
        }
        {
            PacketVO packet = packets.stream().filter(packetVO -> packetVO.getRankOrder() == 2).findFirst().orElse(null);
            assertNotNull(packet);
            assertNotNull(packet.getNumber());
            assertEquals(9, packet.getNumber().intValue());
            assertNotNull(packet.getWeight());
            assertEquals(33.93, packet.getWeight(), 0);
            assertNotNull(packet.getSampledWeights());
            assertArrayEquals(new Double[]{3.74, 3.81, 3.76}, packet.getSampledWeights().toArray());
            // composition
            assertNotNull(packet.getComposition());
            assertEquals(2, packet.getComposition().size());
            {
                PacketCompositionVO composition = packet.getComposition().get(0);
                assertNotNull(composition);
                assertNotNull(composition.getTaxonGroup());
                assertEquals(274, composition.getTaxonGroup().getId().intValue());
                assertNotNull(composition.getRatios());
                assertArrayEquals(new Integer[]{80, 60, 75}, composition.getRatios().toArray());
            }
            {
                PacketCompositionVO composition = packet.getComposition().get(1);
                assertNotNull(composition);
                assertNotNull(composition.getTaxonGroup());
                assertEquals(277, composition.getTaxonGroup().getId().intValue());
                assertNotNull(composition.getRatios());
                assertArrayEquals(new Integer[]{20, 40, 25}, composition.getRatios().toArray());
            }
        }
    }
}
package net.sumaris.core.service.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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


import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.data.PacketCompositionVO;
import net.sumaris.core.vo.data.PacketVO;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author peck7 on 09/04/2020.
 */
@Ignore("Use only on SFA Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
@Slf4j
public class PacketServiceWriteTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

    @Autowired
    private PacketService packetService;

    /**
     * TODO write a test with test data
     */
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

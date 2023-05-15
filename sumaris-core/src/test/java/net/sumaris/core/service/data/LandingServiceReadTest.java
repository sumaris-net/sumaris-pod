package net.sumaris.core.service.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.LandingVO;
import net.sumaris.core.vo.filter.LandingFilterVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Slf4j
public class LandingServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private LandingService service;

    @Test
    public void get() {
        LandingVO vo = service.get(fixtures.getLandingId(0));
        Assert.assertNotNull(vo);

        // Check observers
        Assert.assertNotNull(vo.getObservers());
        Assert.assertTrue(vo.getObservers().size() > 0);
    }


    @Test
    public void findAll() {

        List<LandingVO> vos = service.findAll(null, Page.builder().size(100).build(), null);
        Assert.assertNotNull(vos);
        Assert.assertTrue(vos.size() > 0);
    }

    @Test
    public void findAllWithFilter() throws ParseException {

        LandingFilterVO filter = LandingFilterVO.builder()
                .programLabel("ADAP-CONTROLE")
                .startDate(Dates.parseDate("01/03/2018", "DD/MM/YYYY"))
                .endDate(Dates.parseDate("01/04/2018", "DD/MM/YYYY"))
                .locationId(30) // Auction Douarnenez
                .build();

        List<LandingVO> vos = service.findAll(filter, Page.builder().size(100).build(), null);
        Assert.assertNotNull(vos);
        Assert.assertTrue(vos.size() > 0);
    }


    @Test
    public void findAllByStrategyLabels() {

        // Get a existing strategy label (from a tag id)
        String tagId = fixtures.getSampleTagId(0);
        String[] tagIdParts = tagId.split("-", 2);
        Assume.assumeTrue(tagIdParts.length == 2);
        String strategyLabel = tagIdParts[0];

        log.debug("Search landing by strategy label");

        LandingFilterVO filter = LandingFilterVO.builder()
            .strategyLabels(new String[]{strategyLabel})
            .build();

        List<LandingVO> vos = service.findAll(filter, Page.builder().size(100).build(), null);
        Assert.assertNotNull(vos);
        Assert.assertEquals(3, vos.size());
    }

    @Test
    public void findAllBySampleLabels() {

        String sampleLabel = fixtures.getSampleLabel(0);
        LandingFilterVO filter = LandingFilterVO.builder()
            .sampleLabels(new String[]{sampleLabel})
            .build();

        List<LandingVO> vos = service.findAll(filter, Page.builder().size(100).build(), null);
        Assert.assertNotNull(vos);
        Assert.assertEquals(1, vos.size());
    }

    @Test
    public void findAllBySampleTagIds() {

        String tagId = fixtures.getSampleTagId(0);
        LandingFilterVO filter = LandingFilterVO.builder()
            .sampleTagIds(new String[]{tagId})
            .build();

        List<LandingVO> vos = service.findAll(filter, Page.builder().size(100).build(), null);
        Assert.assertNotNull(vos);
        Assert.assertEquals(1, vos.size());
    }

    /* -- Protected -- */

}

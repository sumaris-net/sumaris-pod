package net.sumaris.core.dao.data;

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
import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.data.trip.TripRepository;
import net.sumaris.core.vo.data.TripFetchOptions;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TripRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private TripRepository repository;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false); // this is need because of delete test
    }

    @Test
    public void findAll() {
        List<TripVO> trips = repository.findAll(0, 100, null, null, TripFetchOptions.DEFAULT)
                .stream().collect(Collectors.toList());
        Assert.assertNotNull(trips);
        Assert.assertTrue(trips.size() > 0);
    }

    @Test
    public void findAllByRecorderPerson() {
        TripFilterVO filter = TripFilterVO.builder()
                .recorderPersonId(fixtures.getPersonId(0))
                .build();
        List<TripVO> trips = repository.findAll(filter);
        Assert.assertNotNull(trips);
        Assert.assertTrue(trips.size() > 0);
    }

    @Test
    public void findAllByRecorderDepartment() {
        TripFilterVO filter = TripFilterVO.builder()
                .recorderDepartmentId(fixtures.getDepartmentId(0))
                .build();
        List<TripVO> trips = repository.findAll(filter);
        Assert.assertNotNull(trips);
        Assert.assertTrue(CollectionUtils.isNotEmpty(trips));
    }

    @Test
    public void getWithChildren() {

        Integer id = fixtures.getTripId(0);
        TripVO trip = repository.get(id, TripFetchOptions.builder()
            .withChildrenEntities(true).build());
        Assert.assertNotNull(trip);
    }

    @Test
    public void deleteById() {
        Integer id = fixtures.getTripId(0);
        repository.deleteById(id);
    }

}

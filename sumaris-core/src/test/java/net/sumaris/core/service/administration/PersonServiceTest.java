package net.sumaris.core.service.administration;

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

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfile;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.crypto.MD5Util;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PersonServiceTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private PersonService service;

    @Autowired
    private ReferentialService referentialService;


    @Test
    public void a_findPersons() {

        Integer observerProfileId =  fixtures.getUserProfileObserver();

        // Find by one profile
        PersonFilterVO filter = new PersonFilterVO();
        filter.setUserProfileId(observerProfileId);
        List<PersonVO> results = assertFindResult(filter, 2);
        PersonVO person = results.get(0);
        Assert.assertTrue(person.getProfiles().size() > 0);
        UserProfileEnum profile = UserProfileEnum.valueOf(person.getProfiles().get(0)) ;
        Assert.assertEquals(observerProfileId, new Integer(profile.id));

        // Find by many profile
        filter = new PersonFilterVO();
        filter.setUserProfileIds(new Integer[]{observerProfileId, fixtures.getUserProfileSupervisor()});
        assertFindResult(filter, 3);

        // Find by status (inactive person)
        filter = new PersonFilterVO();
        filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        assertFindResult(filter, 5);

        // Find by email
        filter = new PersonFilterVO();
        filter.setEmail(fixtures.getPersonEmail(0));
        assertFindResult(filter, 1);

        // Find by last name (case insensitive)
        filter = new PersonFilterVO();
        filter.setLastName("LaVEniER");
        assertFindResult(filter, 1);

    }

    private List<PersonVO> assertFindResult(PersonFilterVO filter, int expectedResult) {
        // Do findByFilter
        List<PersonVO> results = service.findByFilter(filter, 0, 100, null, null);
        Assert.assertNotNull(results);
        Assert.assertEquals(expectedResult, results.size());
        // Do also countByFilter
        long count = service.countByFilter(filter);
        Assert.assertEquals(expectedResult, count);
        return results;
    }

    @Test
    public void isExistsByEmailHash() {

        PersonVO person = service.getById(fixtures.getPersonId(0));
        Assume.assumeNotNull(person);
        String emailHash = MD5Util.md5Hex(person.getEmail());

        boolean isExists = service.isExistsByEmailHash(emailHash);
        Assert.assertTrue(isExists);
    }

    @Test
    public void save() {
        PersonVO vo = new PersonVO();
        vo.setFirstName("first name");
        vo.setLastName("last name");
        vo.setEmail("test@sumaris.net");
        vo.setStatusId(StatusEnum.ENABLE.getId());

        DepartmentVO department = new DepartmentVO();
        department.setId(fixtures.getDepartmentId(0));

        vo.setDepartment(department);

        service.save(vo);

        // keep updateDate
        Date updateDate1 = vo.getUpdateDate();
        Assert.assertNotNull(updateDate1);

        // save again and check update date has changed
        service.save(vo);
        Date updateDate2 = vo.getUpdateDate();
        Assert.assertNotNull(updateDate2);
        Assert.assertNotEquals(updateDate1, updateDate2);

    }

    @Test
    public void saveWithProfile() {
        PersonVO vo = new PersonVO();
        vo.setFirstName("first name with profiles");
        vo.setLastName("last name");
        vo.setEmail("test2@sumaris.net");
        vo.setStatusId(StatusEnum.ENABLE.getId());

        DepartmentVO department = new DepartmentVO();
        department.setId(fixtures.getDepartmentId(0));

        vo.setDepartment(department);

        vo.setProfiles(Arrays.asList("ADMIN", "USER"));

        service.save(vo);
        Assert.assertNotNull(vo.getId());

        // reload and check
        vo = service.getById(vo.getId());
        Assert.assertNotNull(vo);
        Assert.assertEquals("first name with profiles", vo.getFirstName());
        Assert.assertNotNull(vo.getProfiles());
        Assert.assertEquals(2, vo.getProfiles().size());
    }

    @Test
    @Ignore
    // FIXME: find a user without data and right on program
    public void z_delete() {

        long userProfileCountBefore = CollectionUtils.size(referentialService.findByFilter(UserProfile.class.getSimpleName(), null, 0, 1000));

        service.delete(fixtures.getPersonIdNoData());

        // Make there is no cascade on user profile !!
        long userProfileCountAfter = CollectionUtils.size(referentialService.findByFilter(UserProfile.class.getSimpleName(), null, 0, 1000));
        Assert.assertEquals(userProfileCountBefore, userProfileCountAfter);
    }

    @Test
    public void getEmailsByProfiles() {
        List<String> emails = service.getEmailsByProfiles(UserProfileEnum.ADMIN);
        Assert.assertNotNull(emails);
        Assert.assertTrue(emails.size() > 0);
    }

    @Test
    public void getByPubkey() {

        PersonVO person = service.getByPubkey(fixtures.getAdminPubkey());
        Assert.assertNotNull(person);
        Assert.assertEquals(5, person.getId().intValue());

        try {
            service.getByPubkey("____");
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void getAvatarByPubkey() {

        // Observer (has an avatar)
        ImageAttachmentVO avatar = service.getAvatarByPubkey(fixtures.getObserverPubkey(), ImageAttachmentFetchOptions.WITH_CONTENT);
        Assert.assertNotNull(avatar);
        Assert.assertNotNull(avatar.getContentType());
        Assert.assertNotNull(avatar.getContent());

        // Admin (no avatar)
        try {
            service.getAvatarByPubkey(fixtures.getAdminPubkey(), ImageAttachmentFetchOptions.WITH_CONTENT);
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }
}

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

package net.sumaris.server.service.administration;

import net.sumaris.core.vo.data.IRootDataVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Transactional
public interface DataAccessControlService {

    Integer NO_ACCESS_FAKE_ID = -999;
    Integer[] NO_ACCESS_FAKE_IDS = new Integer[]{NO_ACCESS_FAKE_ID};

    boolean canUserAccessNotSelfData();

    boolean canDepartmentAccessNotSelfData(Integer userDepartmentId);

    void checkIsAdmin(String message);

    @Transactional(readOnly = true)
    void checkCanRead(IRootDataVO data);

    @Transactional(readOnly = true)
    void checkCanWrite(IRootDataVO data);

    @Transactional(readOnly = true)
    <T extends IRootDataVO> void checkCanWriteAll(Collection<T> data);

    @Transactional(readOnly = true)
    Optional<Integer[]> getAuthorizedProgramIds(Integer[] programIds);

    @Transactional(readOnly = true)
    Optional<Integer[]> getAllAuthorizedProgramIds(Integer[] programIds);

    @Transactional(readOnly = true)
    List<Integer> getAuthorizedProgramIdsByUserId(int userId);

    @Transactional(readOnly = true)
    Optional<List<Integer>> getAuthorizedProgramIds();

    @Transactional(readOnly = true)
    Optional<Integer[]> getAuthorizedProgramIdsByUserId(int userId, Integer[] programIds);

    @Transactional(readOnly = true)
    Optional<Integer[]> getAuthorizedLocationIds(Integer[] programIds, Integer[] locationIds);

    @Transactional(readOnly = true)
    Optional<Integer[]> getAuthorizedLocationIdsByUserId(int userId, Integer[] programIds, Integer[] locationIds);

}

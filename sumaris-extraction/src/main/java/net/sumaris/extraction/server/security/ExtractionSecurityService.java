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

package net.sumaris.extraction.server.security;

import net.sumaris.core.exception.ForbiddenException;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(propagation = Propagation.SUPPORTS)
public interface ExtractionSecurityService {

    default void checkReadAccess(IExtractionType format) throws ForbiddenException {
        if (!canRead(format)) {
            throw new ForbiddenException("Access forbidden");
        }
    }

    default void checkReadAccess(int productId) throws ForbiddenException {
        if (!canRead(productId)) {
            throw new ForbiddenException("Access forbidden");
        }
    }

    default void checkWriteAccess() throws ForbiddenException {
        if (!canWrite()) {
            throw new ForbiddenException("Access forbidden");
        }
    }

    default void checkWriteAccess(int productId) throws ForbiddenException {
        if (!canWrite(productId)) {
            throw new ForbiddenException("Access forbidden");
        }
    }

    boolean canReadAll();

    boolean canRead(IExtractionType type) throws UnauthorizedException;

    boolean canRead(int productId) throws UnauthorizedException;

    boolean canWrite();

    boolean canWriteAll();

    boolean canWrite(ExtractionProductVO type) throws UnauthorizedException;

    boolean canWrite(int productId) throws UnauthorizedException;

    ExtractionTypeFilterVO sanitizeFilter(ExtractionTypeFilterVO filter);

    Optional<PersonVO> getAuthenticatedUser();
}

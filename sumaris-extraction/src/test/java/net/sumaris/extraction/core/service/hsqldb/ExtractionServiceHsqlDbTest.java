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

package net.sumaris.extraction.core.service.hsqldb;

import net.sumaris.core.service.data.TripService;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.service.ExtractionServiceTest;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro>
 */
public class ExtractionServiceHsqlDbTest extends ExtractionServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();



}
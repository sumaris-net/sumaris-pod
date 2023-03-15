package net.sumaris.core.service.referential.location;

/*
 * #%L
 * SIH-Adagio Core Shared
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2012 - 2013 Ifremer
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

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service used to access locations
 *
 * @author Benoit Lavenier
 */
@Transactional(readOnly = true)
public interface LocationByPositionService {

    /**
     * Return location label from a longitude and a latitude (in decimal degrees - WG84).
     *
     * @param latitude  a latitude (in decimal degrees - WG84)
     * @param longitude a longitude (in decimal degrees - WG84)
     * @return A location label (corresponding to a statistical rectangle), or empty if no statistical rectangle exists for this position
     */
    Optional<String> getStatisticalRectangleLabelByLatLong(Number latitude, Number longitude);

    /**
     * Return a location Id, from a longitude and a latitude (in decimal degrees - WG84).
     * This method typically use getLocationLabelByLatLong().
     *
     * @param latitude  a latitude (in decimal degrees - WG84)
     * @param longitude a longitude (in decimal degrees - WG84)
     * @return A location Id (corresponding to a statistical rectangle), or empty if no statistical rectangle exists for this position
     */
    Optional<Integer> getStatisticalRectangleIdByLatLong(Number latitude, Number longitude);
}

/*
 * Created on 29 juil. 2004
 */
package net.sumaris.core.extraction.dao.technical;

/*-
 * #%L
 * Quadrige3 Core :: Shared
 * %%
 * Copyright (C) 2017 - 2018 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import fr.ifremer.common.xmlquery.HSQLDBSingleXMLQuery;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author ludovic.pecquot@e-is.pro
 */
@Component
@Scope("prototype")
public class XMLQuery extends HSQLDBSingleXMLQuery {

    // let default values here for HSQLDB

}

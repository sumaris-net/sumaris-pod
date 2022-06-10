package net.sumaris.cli.action;

/*-
 * #%L
 * Quadrige3 Core :: Quadrige3 Server Core
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
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

/**
 * <p>
 * HelpAction class.
 * </p>
 * 
 */
public class HelpAction {

	/**
	 * <p>
	 * show.
	 * </p>
	 */
	public void show() {
		StringBuilder sb = new StringBuilder();

		sb.append("Usage:  <commands> <options>").append("\n")
				.append("with <commands>:").append("\n")
				.append(" -h --help                                  Display help").append("\n")
				.append("    --schema-create     --output <db_dir>   Create a new database schema").append("\n")
				.append("    --schema-create-sql --output <file> 	 Create the SQL schema file").append("\n")
				.append("    --schema-update                         Update database schema").append("\n")
				.append("    --schema-changelog  --output <file>     Generate a database changelog report (pending schema changes)").append("\n")
				//.append("    --schema-diff      --output <file>      Generate a database schema diff report (compare database and model)").append("\n")
				.append("\n")
				.append("with <options>:").append("\n")
				.append(" -u  --user <user>		           Database user").append("\n")
				.append(" -p  --password <pwd> 		       Database password").append("\n")
				.append(" -db --database <db_url> 	       Database JDBC URL ()").append("\n")
				.append(" -f                               Force the output directory overwrite, if exists").append("\n")
				.append(" -d                               Run as daemon service").append("\n")
				.append("\n");

		System.out.println(sb.toString());
	}
}

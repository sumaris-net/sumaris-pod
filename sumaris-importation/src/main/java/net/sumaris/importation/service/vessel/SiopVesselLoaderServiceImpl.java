package net.sumaris.importation.service.vessel;

/*-
 * #%L
 * SUMARiS:: Core Importation
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselOwner;
import net.sumaris.core.model.data.VesselPhysicalMeasurement;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.technical.extraction.rdb.*;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.util.Files;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.service.DataLoaderService;
import net.sumaris.importation.util.csv.CSVFileReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service("siopVesselLoaderService")
@Slf4j
public class SiopVesselLoaderServiceImpl implements SiopVesselLoaderService {

	/**
	 * Allow to override the default column headers array, on a given table
	 * Should return the new headers array
	 */
	public interface HeaderAdapter extends BiFunction<DatabaseTableEnum, String[], String[]> {

		@Override
		String[] apply(DatabaseTableEnum table, String[] headers);
	}

	protected static final char DEFAULT_SEPARATOR = ',';
	protected static final char SIOP_DEFAULT_SEPARATOR = ';';

	protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
		// SIOP synonyms (for LPDB)
		.put("Numéro CFR", VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)
		.put("Quart. mar.", VesselRegistrationPeriod.Fields.REGISTRATION_LOCATION)
		.put("Immatr.", VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
		.put("Nom", VesselFeatures.Fields.NAME)
		.put("Jauge", VesselFeatures.Fields.GROSS_TONNAGE_GRT) // TODO ou GT ?
		.put("Longueur HT", VesselFeatures.Fields.LENGTH_OVER_ALL)
		.put("Puissance motrice", VesselFeatures.Fields.ADMINISTRATIVE_POWER)
		.put("Date adh.", VesselFeatures.Fields.START_DATE)
		.put("Date Départ", VesselFeatures.Fields.END_DATE)
		.put("Comment. 1", VesselFeatures.Fields.COMMENTS)

		.build();

	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected SumarisDatabaseMetadata databaseMetadata;

	@Autowired
	protected DataLoaderService dataLoaderService;

	@Autowired
	protected TaxonNameService taxonNameService;

	@Override
	public void loadFromFile(File inputFile, String format, boolean validate, boolean appendData) throws IOException, FileValidationException {
		Files.checkExists(inputFile);

		// If not append : then remove old data
		if (!appendData) {
			// TODO
		}

		File tempFile = null;
		try {
			tempFile = prepareFile(inputFile, format);

			// Do load
			//dataLoaderService.load(tempFile, table, validate);
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		}
		finally {

			Files.deleteQuietly(tempFile);
			Files.deleteFiles(inputFile.getParentFile(), "^.*.tmp[0-9]+$");
		}
	}


	/* -- protected methods -- */

	protected File prepareFile(File inputFile, String format) {
		if ("SIOP".equals(format)) {
			return prepareFile(inputFile, format, SIOP_DEFAULT_SEPARATOR);
		}
		return prepareFile(inputFile, format, DEFAULT_SEPARATOR);
	}

	protected File prepareFile(File inputFile, String format, char separator) {

		try {
			File tempFile = Files.getNewTemporaryFile(inputFile);

			// Replace in headers (exact match
			Map<String, String> exactHeaderReplacements = Maps.newHashMap();
			for (String header: headerReplacements.keySet()) {
				String regexp = "(^|" + separator + ")\"?" + header + "\"?(" + separator + "|$)";
				String replacement = "$1" + headerReplacements.get(header) + "$2";
				exactHeaderReplacements.put(regexp, replacement);
			}
			Files.replaceAllInHeader(inputFile, tempFile, exactHeaderReplacements);

			// Replace in lines
			//Files.replaceAll(tempFile, linesReplacements, 1, -1/*all rows*/);

			return tempFile;
		} catch(IOException e) {
			throw new SumarisTechnicalException("Could not preparing file: " + inputFile.getPath(), e);
		}
	}

	/**
	 * Generate a new headers adapter, depending on the file origin. E.g. in GBR data, remove the fishing_activity column that is not present
	 * @param inputFile
	 * @param format
	 * @return
	 */
	public HeaderAdapter getHeadersAdapter(final File inputFile, final String format) {

		// Special case for GBR
		if ("SIOP".equals(format)) {
			return (table, headers) -> {
				if (table == ProductRdbStation.TABLE) {
					List<String> result = Lists.newArrayList(headers);

					// Remove missing columns in the GRB file
					result.remove(ProductRdbStation.COLUMN_FISHING_VALIDITY);

					return result.toArray(new String[result.size()]);
				}
				return headers;
			};
		}

		return null;
	}
}



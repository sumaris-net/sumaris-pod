package net.sumaris.importation.service.rdb;

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

@Service("rdbDataLoaderService")
@Slf4j
public class RdbDataLoaderServiceImpl implements RdbDataLoaderService {

	/**
	 * Allow to override the default column headers array, on a given table
	 * Should return the new headers array
	 */
	public interface HeaderAdapter extends BiFunction<DatabaseTableEnum, String[], String[]> {

		@Override
		String[] apply(DatabaseTableEnum table, String[] headers);
	}

	protected static final char DEFAULT_SEPARATOR = ',';

	protected static final String MIXED_COLUMN_RECORD_TYPE = "record_type";

	protected static List<DatabaseTableEnum> orderedTables = ImmutableList.of(
			DatabaseTableEnum.P01_RDB_TRIP,
			DatabaseTableEnum.P01_RDB_STATION,
			DatabaseTableEnum.P01_RDB_SPECIES_LIST,
			DatabaseTableEnum.P01_RDB_SPECIES_LENGTH,
			// TODO CA
			DatabaseTableEnum.P01_RDB_LANDING
			// TODO CE
	);

	protected static Map<String, DatabaseTableEnum> tableByRecordTypeMap = ImmutableMap.<String, DatabaseTableEnum>builder()
			.put("TR", DatabaseTableEnum.P01_RDB_TRIP)
			.put("HH", DatabaseTableEnum.P01_RDB_STATION)
			.put("SL", DatabaseTableEnum.P01_RDB_SPECIES_LIST)
			.put("HL", DatabaseTableEnum.P01_RDB_SPECIES_LENGTH)
			// TODO CA
			.put("CL", DatabaseTableEnum.P01_RDB_LANDING)
			// TODO CE
			.build();

	protected static Map<DatabaseTableEnum, String[]> headersByTableMap = ImmutableMap.<DatabaseTableEnum, String[]>builder()
			.put(ProductRdbTrip.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductRdbTrip.COLUMN_SAMPLING_TYPE,
					ProductRdbTrip.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductRdbTrip.COLUMN_LANDING_COUNTRY,
					ProductRdbTrip.COLUMN_YEAR,
					ProductRdbTrip.COLUMN_PROJECT,
					ProductRdbTrip.COLUMN_TRIP_CODE,
					ProductRdbTrip.COLUMN_VESSEL_LENGTH,
					ProductRdbTrip.COLUMN_VESSEL_POWER,
					ProductRdbTrip.COLUMN_VESSEL_SIZE,
					ProductRdbTrip.COLUMN_VESSEL_TYPE,
					ProductRdbTrip.COLUMN_HARBOUR,
					ProductRdbTrip.COLUMN_NUMBER_OF_SETS,
					ProductRdbTrip.COLUMN_DAYS_AT_SEA,
					ProductRdbTrip.COLUMN_VESSEL_IDENTIFIER,
					ProductRdbTrip.COLUMN_SAMPLING_COUNTRY,
					ProductRdbTrip.COLUMN_SAMPLING_METHOD
			})
			.put(ProductRdbStation.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductRdbStation.COLUMN_SAMPLING_TYPE,
					ProductRdbStation.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductRdbStation.COLUMN_LANDING_COUNTRY,
					ProductRdbStation.COLUMN_YEAR,
					ProductRdbStation.COLUMN_PROJECT,
					ProductRdbStation.COLUMN_TRIP_CODE,
					ProductRdbStation.COLUMN_STATION_NUMBER,
					ProductRdbStation.COLUMN_FISHING_VALIDITY,
					ProductRdbStation.COLUMN_AGGREGATION_LEVEL,
					ProductRdbStation.COLUMN_CATCH_REGISTRATION,
					ProductRdbStation.COLUMN_SPECIES_REGISTRATION,
					ProductRdbStation.COLUMN_DATE,
					ProductRdbStation.COLUMN_TIME,
					ProductRdbStation.COLUMN_FISHING_TIME,
					ProductRdbStation.COLUMN_POS_START_LAT,
					ProductRdbStation.COLUMN_POS_START_LON,
					ProductRdbStation.COLUMN_POS_END_LAT,
					ProductRdbStation.COLUMN_POS_END_LON,
					ProductRdbStation.COLUMN_AREA,
					ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE,
					ProductRdbStation.COLUMN_SUB_POLYGON,
					ProductRdbStation.COLUMN_MAIN_FISHING_DEPTH,
					ProductRdbStation.COLUMN_MAIN_WATER_DEPTH,
					ProductRdbStation.COLUMN_NATIONAL_METIER,
					ProductRdbStation.COLUMN_EU_METIER_LEVEL5,
					ProductRdbStation.COLUMN_EU_METIER_LEVEL6,
					ProductRdbStation.COLUMN_GEAR_TYPE,
					ProductRdbStation.COLUMN_MESH_SIZE,
					ProductRdbStation.COLUMN_SELECTION_DEVICE,
					ProductRdbStation.COLUMN_MESH_SIZE_SELECTION_DEVICE
			})
			.put(ProductRdbSpeciesList.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductRdbSpeciesList.COLUMN_SAMPLING_TYPE,
					ProductRdbSpeciesList.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductRdbSpeciesList.COLUMN_LANDING_COUNTRY,
					ProductRdbSpeciesList.COLUMN_YEAR,
					ProductRdbSpeciesList.COLUMN_PROJECT,
					ProductRdbSpeciesList.COLUMN_TRIP_CODE,
					ProductRdbSpeciesList.COLUMN_STATION_NUMBER,
					ProductRdbSpeciesList.COLUMN_SPECIES,
					ProductRdbSpeciesList.COLUMN_SEX,
					ProductRdbSpeciesList.COLUMN_CATCH_CATEGORY,
					ProductRdbSpeciesList.COLUMN_LANDING_CATEGORY,
					ProductRdbSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					ProductRdbSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					ProductRdbSpeciesList.COLUMN_SUBSAMPLING_CATEGORY,
					ProductRdbSpeciesList.COLUMN_WEIGHT,
					ProductRdbSpeciesList.COLUMN_SUBSAMPLING_WEIGHT,
					ProductRdbSpeciesList.COLUMN_LENGTH_CODE
			})
			.put(ProductRdbSpeciesLength.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductRdbSpeciesLength.COLUMN_SAMPLING_TYPE,
					ProductRdbSpeciesLength.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductRdbSpeciesLength.COLUMN_LANDING_COUNTRY,
					ProductRdbSpeciesLength.COLUMN_YEAR,
					ProductRdbSpeciesLength.COLUMN_PROJECT,
					ProductRdbSpeciesLength.COLUMN_TRIP_CODE,
					ProductRdbSpeciesLength.COLUMN_STATION_NUMBER,
					ProductRdbSpeciesLength.COLUMN_SPECIES,
					ProductRdbSpeciesLength.COLUMN_CATCH_CATEGORY,
					ProductRdbSpeciesLength.COLUMN_LANDING_CATEGORY,
					ProductRdbSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					ProductRdbSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					ProductRdbSpeciesLength.COLUMN_SUBSAMPLING_CATEGORY,
					ProductRdbSpeciesLength.COLUMN_SEX,
					ProductRdbSpeciesLength.COLUMN_INDIVIDUAL_SEX,
					ProductRdbSpeciesLength.COLUMN_LENGTH_CLASS,
					ProductRdbSpeciesLength.COLUMN_NUMBER_AT_LENGTH
			})
			// TODO CA
			.put(ProductRdbLanding.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductRdbLanding.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductRdbLanding.COLUMN_LANDING_COUNTRY,
					ProductRdbLanding.COLUMN_YEAR,
					ProductRdbLanding.COLUMN_QUARTER,
					ProductRdbLanding.COLUMN_MONTH,
					ProductRdbLanding.COLUMN_AREA,
					ProductRdbLanding.COLUMN_STATISTICAL_RECTANGLE,
					ProductRdbLanding.COLUMN_SUB_POLYGON,
					ProductRdbLanding.COLUMN_SPECIES,
					ProductRdbLanding.COLUMN_LANDING_CATEGORY,
					ProductRdbLanding.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					ProductRdbLanding.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					ProductRdbLanding.COLUMN_NATIONAL_METIER,
					ProductRdbLanding.COLUMN_EU_METIER_LEVEL5,
					ProductRdbLanding.COLUMN_EU_METIER_LEVEL6,
					ProductRdbLanding.COLUMN_HARBOUR,
					ProductRdbLanding.COLUMN_VESSEL_LENGTH_CATEGORY,
					ProductRdbLanding.COLUMN_UNALLOCATED_CATCH_WEIGHT,
					ProductRdbLanding.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT,
					ProductRdbLanding.COLUMN_OFFICIAL_LANDINGS_WEIGHT,
					ProductRdbLanding.COLUMN_LANDINGS_MULTIPLIER,
					ProductRdbLanding.COLUMN_OFFICIAL_LANDINGS_VALUE
			})
			// TODO CE
			.build();


	protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
			// FRA synonyms
			.put("landCtry", ProductRdbLanding.COLUMN_LANDING_COUNTRY)
			.put("vslFlgCtry", ProductRdbLanding.COLUMN_VESSEL_FLAG_COUNTRY)
			.put("year", ProductRdbLanding.COLUMN_YEAR)
			.put("quarter", ProductRdbLanding.COLUMN_QUARTER)
			.put("month", ProductRdbLanding.COLUMN_MONTH)
			.put("area", ProductRdbLanding.COLUMN_AREA)
			.put("rect", ProductRdbLanding.COLUMN_STATISTICAL_RECTANGLE)
			.put("subRect", ProductRdbLanding.COLUMN_SUB_POLYGON)
			.put("taxon", ProductRdbLanding.COLUMN_SPECIES)
			.put("landCat", ProductRdbLanding.COLUMN_LANDING_CATEGORY)
			.put("commCatScl", ProductRdbLanding.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE)
			.put("commCat", ProductRdbLanding.COLUMN_COMMERCIAL_SIZE_CATEGORY)
			.put("foCatNat", ProductRdbLanding.COLUMN_NATIONAL_METIER)
			.put("foCatEu5", ProductRdbLanding.COLUMN_EU_METIER_LEVEL5)
			.put("foCatEu6", ProductRdbLanding.COLUMN_EU_METIER_LEVEL6)
			.put("harbour", ProductRdbLanding.COLUMN_HARBOUR)
			.put("vslLenCat", ProductRdbLanding.COLUMN_VESSEL_LENGTH_CATEGORY)
			.put("unallocCatchWt", ProductRdbLanding.COLUMN_UNALLOCATED_CATCH_WEIGHT)
			.put("misRepCatchWt", ProductRdbLanding.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT)
			.put("landWt", ProductRdbLanding.COLUMN_OFFICIAL_LANDINGS_WEIGHT)
			.put("landMult", ProductRdbLanding.COLUMN_LANDINGS_MULTIPLIER)
			.put("landValue", ProductRdbLanding.COLUMN_OFFICIAL_LANDINGS_VALUE)

			// BEL synonyms
			.put("FAC_National", ProductRdbLanding.COLUMN_NATIONAL_METIER)
			.put("FAC_EC_lvl5", ProductRdbLanding.COLUMN_EU_METIER_LEVEL5)
			.put("FAC_EC_lvl6", ProductRdbLanding.COLUMN_EU_METIER_LEVEL6)

			// GBR synonyms
			.put("number_hauls", ProductRdbTrip.COLUMN_NUMBER_OF_SETS)
			.put("haul_count", ProductRdbTrip.COLUMN_NUMBER_OF_SETS)
			.put("comm_size_cat_scale", ProductRdbSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE)
			.put("comm_size_cat", ProductRdbSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY)

			// All synonyms
			.put("vessel_length_cat", ProductRdbLanding.COLUMN_VESSEL_LENGTH_CATEGORY)

			.build();

	protected static final Map<String, String> linesReplacements = ImmutableMap.<String, String>builder()
			.put("\"NA\"", "")
			.put(",NA,NA,", ",,,")
			.put(",NA,", ",,")

			// -- FRA Data
			//.put(",u10,", ",0-10,")
			.put(",o40,", ",>40,")
			.put(",([0-9]{1,2})-([0-9]{1,2}),", ",$1-<$2,")

			// -- BEL Data
			.put(",([0-9]{1,2})-<([0-9]{1,2}),", ",$1-$2,")

			// --- GBR data
			// Country code ISO3166-2:GB -> ISO3166-1 alpha-3
			.put("GB[_-]?(ENG|NIR|SCT|WLS)", "GBR")
			// Date format from DD/MM/YYYY -> YYYY-MM-DD
			.put(",([0-9]{2})/([0-9]{2})/([0-9]{4}),", ",$3-$2-$1,")
			// Country code ISO3166-1 alpha-2 -> ISO3166-1 alpha-3
			.put(",BE,BE,", ",BEL,BEL,")
			.put(",FR,FR,", ",FRA,FRA,")
			.put(",DK,DK,", ",DNK,DNK,")
			.put(",NL,NL,", ",NLD,NLD,")
			// Replace unknown metier
			.put("XXX_XXX", "UNK_MZZ")

			// GBR + BEL
			// - If metier level5 is empty, fill using metier level 6
			.put(",,([A-Z]{2,3})_([A-Z]{2,3})_0_0_0,", ",$1_$2,$1_$2_0_0_0,")
			// - If gear is empty, fill using metier level 6
			.put(",([A-Z]{2,3})_([A-Z]{2,3})_0_0_0,,", ",$1_$2_0_0_0,$1,")

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
	public void loadLanding(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		loadTable(inputFile, DatabaseTableEnum.P01_RDB_LANDING, country, validate, appendData);
	}

	@Override
	public void loadTrip(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		loadTable(inputFile, DatabaseTableEnum.P01_RDB_TRIP, country, validate, appendData);
	}

	@Override
	public void detectFormatAndLoad(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		Files.checkExists(inputFile);

		File tempFile = prepareFile(inputFile);

		// Detect file format
		CSVFileReader reader = new CSVFileReader(tempFile, true);
		String[] headers = reader.getHeaders();
		String[] secondRow = reader.readNext();
		reader.close();

		// Empty file
		if (headers == null && secondRow == null) {
			log.warn("File is empty. No data to load.");
			return;
		}

		// If only one row (no headers), then mixed format
		if (headers != null && secondRow == null) {
			loadMixed(inputFile, country, validate, appendData);
			return;
		}

		// More than one row: detected if mixed file
		for(String recordType: tableByRecordTypeMap.keySet()) {
			DatabaseTableEnum table = tableByRecordTypeMap.get(recordType);
			String[] expectedHeaders = headersByTableMap.get(table);

			boolean isMixedFile =
							// First column value should by the record type
							recordType.equals(headers[0]) &&
							// Second column value should NOT be an header name
							!Arrays.asList(expectedHeaders).contains(headers[1].toLowerCase());
			if (isMixedFile) {
				log.info(String.format("[%s] Detected format: mixed record types", inputFile.getName()));
				loadMixed(inputFile, country, validate, appendData);
				return;
			}
		}

		// Detected record type, from the second row
		for(String recordType: tableByRecordTypeMap.keySet()) {
			DatabaseTableEnum table = tableByRecordTypeMap.get(recordType);
			String[] expectedHeaders = headersByTableMap.get(table);

			boolean isOneRecordTypeFile =
							// At least the second expected column header
							Arrays.asList(expectedHeaders).contains(headers[1].toLowerCase()) &&
							// Second line start with expected record type
							recordType.equals(secondRow[0]);
			if (isOneRecordTypeFile) {
				log.info(String.format("[%s] Detected format: unique record type {%s}", inputFile.getName(), table.name().toLowerCase()));
				loadTable(inputFile, table, country, validate, appendData);
				return;
			}
		}

		throw new SumarisTechnicalException("Unable to detect file format");
	}

	@Override
	public void loadMixed(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		Files.checkExists(inputFile);

		// If not append : then remove old data
		if (!appendData) {
			String[] filterCols = StringUtils.isNotBlank(country) ? new String[] { ProductRdbTrip.COLUMN_VESSEL_FLAG_COUNTRY } : null;
			String[] filterVals = StringUtils.isNotBlank(country) ? new String[] { country } : null;
			List<DatabaseTableEnum> deleteOrderedTables = orderedTables.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
			for (DatabaseTableEnum table: deleteOrderedTables) {
				dataLoaderService.remove(table, filterCols, filterVals);
			}
		}

		Collection<File> tempFiles = null;
		try {
			// Split file in many, by table
			Map<DatabaseTableEnum, File> files = prepareMixedFile(inputFile, getHeadersAdapter(inputFile, country));

			tempFiles = files.values();

			for (DatabaseTableEnum table: orderedTables) {
				File file = files.get(table);
				if (file != null && file.exists()) {
					log.info("-----------------------------------------------");
					dataLoaderService.load(file, table, validate);
				}
			}
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		}
		finally {
			if (CollectionUtils.isNotEmpty(tempFiles)) {
				tempFiles.forEach(FileUtils::deleteQuietly);
			}
		}

	}


	/* -- protected methods -- */

	protected void loadTable(File inputFile, DatabaseTableEnum table, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		Files.checkExists(inputFile);

		// If not append : then remove old data
		if (!appendData) {

			if (StringUtils.isNotBlank(country)) {
				dataLoaderService.remove(table, new String[] { ProductRdbTrip.COLUMN_VESSEL_FLAG_COUNTRY }, new String[] { country });
			} else {
				dataLoaderService.remove(table, null, null);
			}
		}

		File tempFile = null;
		try {
			tempFile = prepareFileForTable(inputFile, table, DEFAULT_SEPARATOR);

			// Do load
			dataLoaderService.load(tempFile, table, validate);
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

	protected File prepareFileForTable(File inputFile, DatabaseTableEnum table, char separator){

		File tempFile = prepareFile(inputFile, separator);

		try {

			// If species list table
			if (table != null
				&& (table == DatabaseTableEnum.P01_RDB_SPECIES_LIST || table == DatabaseTableEnum.P01_RDB_SPECIES_LENGTH)) {

				// Get taxon name, to create a replacement map
				Map<String, String> taxonNameReplacements = Maps.newHashMap();
				for (TaxonNameVO t: taxonNameService.findAllSpeciesAndSubSpecies(true,
					(Page)null, null)) {
					if (StringUtils.isNotBlank(t.getLabel())) {
						String regexp = t.getName() + "[^,;\t\"]*" + separator;
						String replacement = t.getLabel() + separator;
						taxonNameReplacements.put(regexp, replacement);
					}
				}

				// Replace in lines
				Files.replaceAll(tempFile, taxonNameReplacements, 1, -1/*all rows*/);
			}

			return tempFile;

		} catch(IOException e) {
			throw new SumarisTechnicalException("Could not preparing file: " + inputFile.getPath(), e);
		}


	}

	protected File prepareFile(File inputFile) {
		return prepareFile(inputFile, DEFAULT_SEPARATOR);
	}

	protected File prepareFile(File inputFile, char separator) {

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
			Files.replaceAll(tempFile, linesReplacements, 1, -1/*all rows*/);

			return tempFile;
		} catch(IOException e) {
			throw new SumarisTechnicalException("Could not preparing file: " + inputFile.getPath(), e);
		}
	}


	protected Map<DatabaseTableEnum, File> prepareMixedFile(File inputFile, HeaderAdapter headersAdapter) throws IOException {

		CSVFileReader reader = new CSVFileReader(inputFile, true);
		char separator = reader.getSeparator();
		reader.close();

		// Delete previous temp files
		Files.deleteTemporaryFiles(inputFile);

		Map<DatabaseTableEnum, File> result = Maps.newHashMap();
		for(String recordType: tableByRecordTypeMap.keySet()) {
			DatabaseTableEnum table = tableByRecordTypeMap.get(recordType);
			File tempFile = Files.getNewTemporaryFile(inputFile);
			List<String> prefixes = ImmutableList.of(recordType+separator, "\""+recordType+"\""+separator);
			Files.filter(inputFile, tempFile, (line) ->
				prefixes.stream().filter(prefix -> line.startsWith(prefix)).findFirst().isPresent()
			);

			if (!Files.isEmpty(tempFile)) {
				tempFile = insertHeader(tempFile, table, separator, headersAdapter);

				tempFile = prepareFileForTable(tempFile, table, separator);

				String recordBasename = String.format("%s-%s.%s%s", Files.getNameWithoutExtension(inputFile), recordType,
						Files.getExtension(inputFile).orElse(""), Files.TEMPORARY_FILE_DEFAULT_EXTENSION);
				File recordFile = new File(inputFile.getParentFile(), recordBasename);
				Files.copyFile(tempFile, recordFile);

				Files.deleteTemporaryFiles(inputFile);

				result.put(table, recordFile);
			}
		}

		return result;
	}

	public File insertHeader(File inputFile, DatabaseTableEnum table, char separator, HeaderAdapter headersAdapter) throws IOException {

		String[] headers = headersByTableMap.get(table);
		if (ArrayUtils.isEmpty(headers)) {
			throw new SumarisTechnicalException(String.format("No default headers defined, for table {%s}", table.name()));
		}

		// Adapt headers, if need (e.g. remove/add specific columns)
		if (headersAdapter != null) {
			headers = headersAdapter.apply(table, headers);
			if (ArrayUtils.isEmpty(headers)) {
				throw new SumarisTechnicalException(String.format("No headers returned by the adapter, for table {%s}", table.name()));
			}
		}

		File tempFile = Files.getNewTemporaryFile(inputFile);
		String headerLine = Joiner.on(separator).join(headers);
		Files.prependLines(inputFile, tempFile, headerLine);
		return tempFile;
	}

	/**
	 * Generate a new headers adapter, depending on the file origin. E.g. in GBR data, remove the fishing_activity column that is not present
	 * @param inputFile
	 * @param country
	 * @return
	 */
	public HeaderAdapter getHeadersAdapter(final File inputFile, final String country) {

		// Special case for GBR
		if ("GBR".equals(country)) {
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



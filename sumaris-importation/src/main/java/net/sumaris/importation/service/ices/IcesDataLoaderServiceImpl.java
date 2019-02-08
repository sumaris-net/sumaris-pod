package net.sumaris.importation.service.ices;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.schema.*;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.file.ices.*;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.util.file.FileUtils;
import net.sumaris.core.vo.referential.TaxonNameVO;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.service.DataLoaderService;
import net.sumaris.importation.util.csv.CSVFileReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service("icesDataLoaderService")
public class IcesDataLoaderServiceImpl implements IcesDataLoaderService {

	protected static final Logger log = LoggerFactory.getLogger(IcesDataLoaderServiceImpl.class);

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
			DatabaseTableEnum.FILE_ICES_TRIP,
			DatabaseTableEnum.FILE_ICES_STATION,
			DatabaseTableEnum.FILE_ICES_SPECIES_LIST,
			DatabaseTableEnum.FILE_ICES_SPECIES_LENGTH,
			// TODO CA
			DatabaseTableEnum.FILE_ICES_LANDING
			// TODO CE
	);

	protected static Map<String, DatabaseTableEnum> tableByRecordTypeMap = ImmutableMap.<String, DatabaseTableEnum>builder()
			.put("TR", DatabaseTableEnum.FILE_ICES_TRIP)
			.put("HH", DatabaseTableEnum.FILE_ICES_STATION)
			.put("SL", DatabaseTableEnum.FILE_ICES_SPECIES_LIST)
			.put("HL", DatabaseTableEnum.FILE_ICES_SPECIES_LENGTH)
			// TODO CA
			.put("CL", DatabaseTableEnum.FILE_ICES_LANDING)
			// TODO CE
			.build();

	protected static Map<DatabaseTableEnum, String[]> headersByTableMap = ImmutableMap.<DatabaseTableEnum, String[]>builder()
			.put(FileIcesTrip.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					FileIcesTrip.COLUMN_SAMPLING_TYPE,
					FileIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY,
					FileIcesTrip.COLUMN_LANDING_COUNTRY,
					FileIcesTrip.COLUMN_YEAR,
					FileIcesTrip.COLUMN_PROJECT,
					FileIcesTrip.COLUMN_TRIP_CODE,
					FileIcesTrip.COLUMN_VESSEL_LENGTH,
					FileIcesTrip.COLUMN_VESSEL_POWER,
					FileIcesTrip.COLUMN_VESSEL_SIZE,
					FileIcesTrip.COLUMN_VESSEL_TYPE,
					FileIcesTrip.COLUMN_HARBOUR,
					FileIcesTrip.COLUMN_OPERATION_COUNT,
					FileIcesTrip.COLUMN_DAYS_AT_SEA,
					FileIcesTrip.COLUMN_VESSEL_IDENTIFIER,
					FileIcesTrip.COLUMN_SAMPLING_COUNTRY,
					FileIcesTrip.COLUMN_SAMPLING_METHOD
			})
			.put(FileIcesStation.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					FileIcesStation.COLUMN_SAMPLING_TYPE,
					FileIcesStation.COLUMN_VESSEL_FLAG_COUNTRY,
					FileIcesStation.COLUMN_LANDING_COUNTRY,
					FileIcesStation.COLUMN_YEAR,
					FileIcesStation.COLUMN_PROJECT,
					FileIcesStation.COLUMN_TRIP_CODE,
					FileIcesStation.COLUMN_STATION_NUMBER,
					FileIcesStation.COLUMN_FISHING_VALIDITY,
					FileIcesStation.COLUMN_AGGREGATION_LEVEL,
					FileIcesStation.COLUMN_CATCH_REGISTRATION,
					FileIcesStation.COLUMN_SPECIES_REGISTRATION,
					FileIcesStation.COLUMN_DATE,
					FileIcesStation.COLUMN_TIME,
					FileIcesStation.COLUMN_FISHING_DURATION,
					FileIcesStation.COLUMN_POS_START_LAT,
					FileIcesStation.COLUMN_POS_START_LON,
					FileIcesStation.COLUMN_POS_END_LAT,
					FileIcesStation.COLUMN_POS_END_LON,
					FileIcesStation.COLUMN_AREA,
					FileIcesStation.COLUMN_STATISTICAL_RECTANGLE,
					FileIcesStation.COLUMN_SUB_POLYGON,
					FileIcesStation.COLUMN_MAIN_FISHING_DEPTH,
					FileIcesStation.COLUMN_MAIN_WATER_DEPTH,
					FileIcesStation.COLUMN_NATIONAL_METIER,
					FileIcesStation.COLUMN_EU_METIER_LEVEL5,
					FileIcesStation.COLUMN_EU_METIER_LEVEL6,
					FileIcesStation.COLUMN_GEAR_TYPE,
					FileIcesStation.COLUMN_MESH_SIZE,
					FileIcesStation.COLUMN_SELECTION_DEVICE,
					FileIcesStation.COLUMN_MESH_SIZE_SELECTION_DEVICE
			})
			.put(FileIcesSpeciesList.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					FileIcesSpeciesList.COLUMN_SAMPLING_TYPE,
					FileIcesSpeciesList.COLUMN_VESSEL_FLAG_COUNTRY,
					FileIcesSpeciesList.COLUMN_LANDING_COUNTRY,
					FileIcesSpeciesList.COLUMN_YEAR,
					FileIcesSpeciesList.COLUMN_PROJECT,
					FileIcesSpeciesList.COLUMN_TRIP_CODE,
					FileIcesSpeciesList.COLUMN_STATION_NUMBER,
					FileIcesSpeciesList.COLUMN_SPECIES,
					FileIcesSpeciesList.COLUMN_SEX,
					FileIcesSpeciesList.COLUMN_CATCH_CATEGORY,
					FileIcesSpeciesList.COLUMN_LANDING_CATEGORY,
					FileIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					FileIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					FileIcesSpeciesList.COLUMN_SUBSAMPLING_CATEGORY,
					FileIcesSpeciesList.COLUMN_WEIGHT,
					FileIcesSpeciesList.COLUMN_SUBSAMPLING_WEIGHT,
					FileIcesSpeciesList.COLUMN_LENGTH_CODE
			})
			.put(FileIcesSpeciesLength.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					FileIcesSpeciesLength.COLUMN_SAMPLING_TYPE,
					FileIcesSpeciesLength.COLUMN_VESSEL_FLAG_COUNTRY,
					FileIcesSpeciesLength.COLUMN_LANDING_COUNTRY,
					FileIcesSpeciesLength.COLUMN_YEAR,
					FileIcesSpeciesLength.COLUMN_PROJECT,
					FileIcesSpeciesLength.COLUMN_TRIP_CODE,
					FileIcesSpeciesLength.COLUMN_STATION_NUMBER,
					FileIcesSpeciesLength.COLUMN_SPECIES,
					FileIcesSpeciesLength.COLUMN_CATCH_CATEGORY,
					FileIcesSpeciesLength.COLUMN_LANDING_CATEGORY,
					FileIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					FileIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					FileIcesSpeciesLength.COLUMN_SUBSAMPLING_CATEGORY,
					FileIcesSpeciesLength.COLUMN_SEX,
					FileIcesSpeciesLength.COLUMN_INDIVIDUAL_SEX,
					FileIcesSpeciesLength.COLUMN_LENGTH_CLASS,
					FileIcesSpeciesLength.COLUMN_NUMBER_AT_LENGTH
			})
			// TODO CA
			.put(FileIcesLandingStatistics.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					FileIcesLandingStatistics.COLUMN_VESSEL_FLAG_COUNTRY,
					FileIcesLandingStatistics.COLUMN_LANDING_COUNTRY,
					FileIcesLandingStatistics.COLUMN_YEAR,
					FileIcesLandingStatistics.COLUMN_QUARTER,
					FileIcesLandingStatistics.COLUMN_MONTH,
					FileIcesLandingStatistics.COLUMN_AREA,
					FileIcesLandingStatistics.COLUMN_STATISTICAL_RECTANGLE,
					FileIcesLandingStatistics.COLUMN_SUB_POLYGON,
					FileIcesLandingStatistics.COLUMN_SPECIES,
					FileIcesLandingStatistics.COLUMN_LANDING_CATEGORY,
					FileIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					FileIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					FileIcesLandingStatistics.COLUMN_NATIONAL_METIER,
					FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5,
					FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6,
					FileIcesLandingStatistics.COLUMN_HARBOUR,
					FileIcesLandingStatistics.COLUMN_VESSEL_LENGTH_CATEGORY,
					FileIcesLandingStatistics.COLUMN_UNALLOCATED_CATCH_WEIGHT,
					FileIcesLandingStatistics.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT,
					FileIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_WEIGHT,
					FileIcesLandingStatistics.COLUMN_LANDINGS_MULTIPLIER,
					FileIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_VALUE
			})
			// TODO CE
			.build();


	protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
			// FRA synonyms
			.put("landCtry", FileIcesLandingStatistics.COLUMN_LANDING_COUNTRY)
			.put("vslFlgCtry", FileIcesLandingStatistics.COLUMN_VESSEL_FLAG_COUNTRY)
			.put("year", FileIcesLandingStatistics.COLUMN_YEAR)
			.put("quarter", FileIcesLandingStatistics.COLUMN_QUARTER)
			.put("month", FileIcesLandingStatistics.COLUMN_MONTH)
			.put("area", FileIcesLandingStatistics.COLUMN_AREA)
			.put("rect", FileIcesLandingStatistics.COLUMN_STATISTICAL_RECTANGLE)
			.put("subRect", FileIcesLandingStatistics.COLUMN_SUB_POLYGON)
			.put("taxon", FileIcesLandingStatistics.COLUMN_SPECIES)
			.put("landCat", FileIcesLandingStatistics.COLUMN_LANDING_CATEGORY)
			.put("commCatScl", FileIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE)
			.put("commCat", FileIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY)
			.put("foCatNat", FileIcesLandingStatistics.COLUMN_NATIONAL_METIER)
			.put("foCatEu5", FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5)
			.put("foCatEu6", FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6)
			.put("harbour", FileIcesLandingStatistics.COLUMN_HARBOUR)
			.put("vslLenCat", FileIcesLandingStatistics.COLUMN_VESSEL_LENGTH_CATEGORY)
			.put("unallocCatchWt", FileIcesLandingStatistics.COLUMN_UNALLOCATED_CATCH_WEIGHT)
			.put("misRepCatchWt", FileIcesLandingStatistics.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT)
			.put("landWt", FileIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_WEIGHT)
			.put("landMult", FileIcesLandingStatistics.COLUMN_LANDINGS_MULTIPLIER)
			.put("landValue", FileIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_VALUE)

			// BEL synonyms
			.put("FAC_National", FileIcesLandingStatistics.COLUMN_NATIONAL_METIER)
			.put("FAC_EC_lvl5", FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5)
			.put("FAC_EC_lvl6", FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6)


			.build();

	protected static final Map<String, String> linesReplacements = ImmutableMap.<String, String>builder()
			.put("\"NA\"", "")
			.put(",NA,", ",,")

			// -- BEL Data
			.put(",([0-9]{1,2})-<([0-9]{1,2}),", "$1-$2,")

			// --- GBR data
			// Country code ISO3166-2:GB -> ISO3166-1 alpha-3
			.put("GB[_-]?(ENG|NIR|SCT|WLS)", "GBR")
			// Date format from DD/MM//YYYY -> YYYY-MM-DD
			.put(",([0-9]{2})/([0-9]{2})/([0-9]{4}),", ",$3-$2-$1,")
			// Country code ISO3166-1 alpha-2 -> ISO3166-1 alpha-3
			.put(",BE,BE,", ",BEL,BEL,")
			.put(",FR,FR,", ",FRA,FRA,")
			.put(",DK,DK,", ",DNK,DNK,")
			.put(",NL,NL,", ",NLD,NLD,")
			// Fill metier level5 and gear_type, using metier level 6
			.put(",([A-Z]{2,3})_([A-Z]{2,3})_0_0_0,", "$1_$2,$1_$2_0_0_0,$1,")
			// Replace unkown metier
			.put("XXX_XXX", "UNK_MZZ")

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
		loadTable(inputFile, DatabaseTableEnum.FILE_ICES_LANDING, country, validate, appendData);
	}

	@Override
	public void loadTrip(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		loadTable(inputFile, DatabaseTableEnum.FILE_ICES_TRIP, country, validate, appendData);
	}

	@Override
	public void detectFormatAndLoad(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		FileUtils.checkExists(inputFile);

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
		FileUtils.checkExists(inputFile);

		// If not append : then remove old data
		if (!appendData) {
			String[] filterCols = StringUtils.isNotBlank(country) ? new String[] { FileIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY } : null;
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
				//tempFiles.forEach(FileUtils::deleteQuietly);
			}
		}

	}


	/* -- protected methods -- */

	protected void loadTable(File inputFile, DatabaseTableEnum table, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		FileUtils.checkExists(inputFile);

		// If not append : then remove old data
		if (!appendData) {

			if (StringUtils.isNotBlank(country)) {
				dataLoaderService.remove(table, new String[] { FileIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY }, new String[] { country });
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
			FileUtils.deleteQuietly(tempFile);

			FileUtils.deleteFiles(inputFile.getParentFile(), "^.*.tmp[0-9]+$");
		}
	}

	protected File prepareFileForTable(File inputFile, DatabaseTableEnum table, char separator){

		File tempFile = prepareFile(inputFile, separator);

		try {

			// If species list table
			if (table != null && (
					table == DatabaseTableEnum.FILE_ICES_SPECIES_LIST
				|| table == DatabaseTableEnum.FILE_ICES_SPECIES_LENGTH)
			) {

				// Get taxon name, to create a replacement map
				Map<String, String> taxonNameReplacements = Maps.newHashMap();
				for (TaxonNameVO t: taxonNameService.getAll(true)) {
					if (StringUtils.isNotBlank(t.getLabel())) {
						String regexp = t.getName() + "[^,;\t\"]*" + separator;
						String replacement = t.getLabel() + separator;
						taxonNameReplacements.put(regexp, replacement);
					}
				}

				// Replace in lines
				FileUtils.replaceAll(tempFile, taxonNameReplacements, 1, -1/*all rows*/);
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
			File tempFile = FileUtils.getNewTemporaryFile(inputFile);

			// Replace in headers (exact match
			Map<String, String> exactHeaderReplacements = Maps.newHashMap();
			for (String header: headerReplacements.keySet()) {
				String regexp = "(^|" + separator + ")\"?" + header + "\"?(" + separator + "|$)";
				String replacement = separator + headerReplacements.get(header) + separator;
				exactHeaderReplacements.put(regexp, replacement);
			}
			FileUtils.replaceAllInHeader(inputFile, tempFile, exactHeaderReplacements);

			// Replace in lines
			FileUtils.replaceAll(tempFile, linesReplacements, 1, -1/*all rows*/);

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
		FileUtils.deleteTemporaryFiles(inputFile);

		Map<DatabaseTableEnum, File> result = Maps.newHashMap();
		for(String recordType: tableByRecordTypeMap.keySet()) {
			DatabaseTableEnum table = tableByRecordTypeMap.get(recordType);
			File tempFile = FileUtils.getNewTemporaryFile(inputFile);
			List<String> prefixes = ImmutableList.of(recordType+separator, "\""+recordType+"\""+separator);
			FileUtils.filter(inputFile, tempFile, (line) ->
				prefixes.stream().filter(prefix -> line.startsWith(prefix)).findFirst().isPresent()
			);

			if (!FileUtils.isEmpty(tempFile)) {
				tempFile = insertHeader(tempFile, table, separator, headersAdapter);

				tempFile = prepareFileForTable(tempFile, table, separator);

				String recordBasename = String.format("%s-%s.%s%s", FileUtils.getNameWithoutExtension(inputFile), recordType, FileUtils.getExtension(inputFile), FileUtils.TEMPORARY_FILE_EXTENSION);
				File recordFile = new File(inputFile.getParentFile(), recordBasename);
				FileUtils.copyFile(tempFile, recordFile);

				FileUtils.deleteTemporaryFiles(inputFile);

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

		File tempFile = FileUtils.getNewTemporaryFile(inputFile);
		String headerLine = Joiner.on(separator).join(headers);
		FileUtils.prependLines(inputFile, tempFile, headerLine);
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
				if (table == FileIcesStation.TABLE) {
					List<String> result = Lists.newArrayList(headers);

					// Remove missing columns in the GRB file
					result.remove(FileIcesStation.COLUMN_FISHING_VALIDITY);

					return result.toArray(new String[result.size()]);
				}
				return headers;
			};
		}

		return null;
	}
}



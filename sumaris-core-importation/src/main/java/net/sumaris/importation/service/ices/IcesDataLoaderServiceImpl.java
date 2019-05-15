package net.sumaris.importation.service.ices;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.schema.*;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.product.ices.*;
import net.sumaris.core.service.referential.taxon.TaxonNameService;
import net.sumaris.core.util.Files;
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
			DatabaseTableEnum.P01_ICES_TRIP,
			DatabaseTableEnum.P01_ICES_STATION,
			DatabaseTableEnum.P01_ICES_SPECIES_LIST,
			DatabaseTableEnum.P01_ICES_SPECIES_LENGTH,
			// TODO CA
			DatabaseTableEnum.P01_ICES_LANDING
			// TODO CE
	);

	protected static Map<String, DatabaseTableEnum> tableByRecordTypeMap = ImmutableMap.<String, DatabaseTableEnum>builder()
			.put("TR", DatabaseTableEnum.P01_ICES_TRIP)
			.put("HH", DatabaseTableEnum.P01_ICES_STATION)
			.put("SL", DatabaseTableEnum.P01_ICES_SPECIES_LIST)
			.put("HL", DatabaseTableEnum.P01_ICES_SPECIES_LENGTH)
			// TODO CA
			.put("CL", DatabaseTableEnum.P01_ICES_LANDING)
			// TODO CE
			.build();

	protected static Map<DatabaseTableEnum, String[]> headersByTableMap = ImmutableMap.<DatabaseTableEnum, String[]>builder()
			.put(ProductIcesTrip.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductIcesTrip.COLUMN_SAMPLING_TYPE,
					ProductIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductIcesTrip.COLUMN_LANDING_COUNTRY,
					ProductIcesTrip.COLUMN_YEAR,
					ProductIcesTrip.COLUMN_PROJECT,
					ProductIcesTrip.COLUMN_TRIP_CODE,
					ProductIcesTrip.COLUMN_VESSEL_LENGTH,
					ProductIcesTrip.COLUMN_VESSEL_POWER,
					ProductIcesTrip.COLUMN_VESSEL_SIZE,
					ProductIcesTrip.COLUMN_VESSEL_TYPE,
					ProductIcesTrip.COLUMN_HARBOUR,
					ProductIcesTrip.COLUMN_NUMBER_OF_SETS,
					ProductIcesTrip.COLUMN_DAYS_AT_SEA,
					ProductIcesTrip.COLUMN_VESSEL_IDENTIFIER,
					ProductIcesTrip.COLUMN_SAMPLING_COUNTRY,
					ProductIcesTrip.COLUMN_SAMPLING_METHOD
			})
			.put(ProductIcesStation.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductIcesStation.COLUMN_SAMPLING_TYPE,
					ProductIcesStation.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductIcesStation.COLUMN_LANDING_COUNTRY,
					ProductIcesStation.COLUMN_YEAR,
					ProductIcesStation.COLUMN_PROJECT,
					ProductIcesStation.COLUMN_TRIP_CODE,
					ProductIcesStation.COLUMN_STATION_NUMBER,
					ProductIcesStation.COLUMN_FISHING_VALIDITY,
					ProductIcesStation.COLUMN_AGGREGATION_LEVEL,
					ProductIcesStation.COLUMN_CATCH_REGISTRATION,
					ProductIcesStation.COLUMN_SPECIES_REGISTRATION,
					ProductIcesStation.COLUMN_DATE,
					ProductIcesStation.COLUMN_TIME,
					ProductIcesStation.COLUMN_FISHING_DURATION,
					ProductIcesStation.COLUMN_POS_START_LAT,
					ProductIcesStation.COLUMN_POS_START_LON,
					ProductIcesStation.COLUMN_POS_END_LAT,
					ProductIcesStation.COLUMN_POS_END_LON,
					ProductIcesStation.COLUMN_AREA,
					ProductIcesStation.COLUMN_STATISTICAL_RECTANGLE,
					ProductIcesStation.COLUMN_SUB_POLYGON,
					ProductIcesStation.COLUMN_MAIN_FISHING_DEPTH,
					ProductIcesStation.COLUMN_MAIN_WATER_DEPTH,
					ProductIcesStation.COLUMN_NATIONAL_METIER,
					ProductIcesStation.COLUMN_EU_METIER_LEVEL5,
					ProductIcesStation.COLUMN_EU_METIER_LEVEL6,
					ProductIcesStation.COLUMN_GEAR_TYPE,
					ProductIcesStation.COLUMN_MESH_SIZE,
					ProductIcesStation.COLUMN_SELECTION_DEVICE,
					ProductIcesStation.COLUMN_MESH_SIZE_SELECTION_DEVICE
			})
			.put(ProductIcesSpeciesList.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductIcesSpeciesList.COLUMN_SAMPLING_TYPE,
					ProductIcesSpeciesList.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductIcesSpeciesList.COLUMN_LANDING_COUNTRY,
					ProductIcesSpeciesList.COLUMN_YEAR,
					ProductIcesSpeciesList.COLUMN_PROJECT,
					ProductIcesSpeciesList.COLUMN_TRIP_CODE,
					ProductIcesSpeciesList.COLUMN_STATION_NUMBER,
					ProductIcesSpeciesList.COLUMN_SPECIES,
					ProductIcesSpeciesList.COLUMN_SEX,
					ProductIcesSpeciesList.COLUMN_CATCH_CATEGORY,
					ProductIcesSpeciesList.COLUMN_LANDING_CATEGORY,
					ProductIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					ProductIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					ProductIcesSpeciesList.COLUMN_SUBSAMPLING_CATEGORY,
					ProductIcesSpeciesList.COLUMN_WEIGHT,
					ProductIcesSpeciesList.COLUMN_SUBSAMPLING_WEIGHT,
					ProductIcesSpeciesList.COLUMN_LENGTH_CODE
			})
			.put(ProductIcesSpeciesLength.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductIcesSpeciesLength.COLUMN_SAMPLING_TYPE,
					ProductIcesSpeciesLength.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductIcesSpeciesLength.COLUMN_LANDING_COUNTRY,
					ProductIcesSpeciesLength.COLUMN_YEAR,
					ProductIcesSpeciesLength.COLUMN_PROJECT,
					ProductIcesSpeciesLength.COLUMN_TRIP_CODE,
					ProductIcesSpeciesLength.COLUMN_STATION_NUMBER,
					ProductIcesSpeciesLength.COLUMN_SPECIES,
					ProductIcesSpeciesLength.COLUMN_CATCH_CATEGORY,
					ProductIcesSpeciesLength.COLUMN_LANDING_CATEGORY,
					ProductIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					ProductIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					ProductIcesSpeciesLength.COLUMN_SUBSAMPLING_CATEGORY,
					ProductIcesSpeciesLength.COLUMN_SEX,
					ProductIcesSpeciesLength.COLUMN_INDIVIDUAL_SEX,
					ProductIcesSpeciesLength.COLUMN_LENGTH_CLASS,
					ProductIcesSpeciesLength.COLUMN_NUMBER_AT_LENGTH
			})
			// TODO CA
			.put(ProductIcesLandingStatistics.TABLE, new String[]{
					MIXED_COLUMN_RECORD_TYPE,
					ProductIcesLandingStatistics.COLUMN_VESSEL_FLAG_COUNTRY,
					ProductIcesLandingStatistics.COLUMN_LANDING_COUNTRY,
					ProductIcesLandingStatistics.COLUMN_YEAR,
					ProductIcesLandingStatistics.COLUMN_QUARTER,
					ProductIcesLandingStatistics.COLUMN_MONTH,
					ProductIcesLandingStatistics.COLUMN_AREA,
					ProductIcesLandingStatistics.COLUMN_STATISTICAL_RECTANGLE,
					ProductIcesLandingStatistics.COLUMN_SUB_POLYGON,
					ProductIcesLandingStatistics.COLUMN_SPECIES,
					ProductIcesLandingStatistics.COLUMN_LANDING_CATEGORY,
					ProductIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
					ProductIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY,
					ProductIcesLandingStatistics.COLUMN_NATIONAL_METIER,
					ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5,
					ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6,
					ProductIcesLandingStatistics.COLUMN_HARBOUR,
					ProductIcesLandingStatistics.COLUMN_VESSEL_LENGTH_CATEGORY,
					ProductIcesLandingStatistics.COLUMN_UNALLOCATED_CATCH_WEIGHT,
					ProductIcesLandingStatistics.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT,
					ProductIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_WEIGHT,
					ProductIcesLandingStatistics.COLUMN_LANDINGS_MULTIPLIER,
					ProductIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_VALUE
			})
			// TODO CE
			.build();


	protected static final Map<String, String> headerReplacements = ImmutableMap.<String, String>builder()
			// FRA synonyms
			.put("landCtry", ProductIcesLandingStatistics.COLUMN_LANDING_COUNTRY)
			.put("vslFlgCtry", ProductIcesLandingStatistics.COLUMN_VESSEL_FLAG_COUNTRY)
			.put("year", ProductIcesLandingStatistics.COLUMN_YEAR)
			.put("quarter", ProductIcesLandingStatistics.COLUMN_QUARTER)
			.put("month", ProductIcesLandingStatistics.COLUMN_MONTH)
			.put("area", ProductIcesLandingStatistics.COLUMN_AREA)
			.put("rect", ProductIcesLandingStatistics.COLUMN_STATISTICAL_RECTANGLE)
			.put("subRect", ProductIcesLandingStatistics.COLUMN_SUB_POLYGON)
			.put("taxon", ProductIcesLandingStatistics.COLUMN_SPECIES)
			.put("landCat", ProductIcesLandingStatistics.COLUMN_LANDING_CATEGORY)
			.put("commCatScl", ProductIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE)
			.put("commCat", ProductIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY)
			.put("foCatNat", ProductIcesLandingStatistics.COLUMN_NATIONAL_METIER)
			.put("foCatEu5", ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5)
			.put("foCatEu6", ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6)
			.put("harbour", ProductIcesLandingStatistics.COLUMN_HARBOUR)
			.put("vslLenCat", ProductIcesLandingStatistics.COLUMN_VESSEL_LENGTH_CATEGORY)
			.put("unallocCatchWt", ProductIcesLandingStatistics.COLUMN_UNALLOCATED_CATCH_WEIGHT)
			.put("misRepCatchWt", ProductIcesLandingStatistics.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT)
			.put("landWt", ProductIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_WEIGHT)
			.put("landMult", ProductIcesLandingStatistics.COLUMN_LANDINGS_MULTIPLIER)
			.put("landValue", ProductIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_VALUE)

			// BEL synonyms
			.put("FAC_National", ProductIcesLandingStatistics.COLUMN_NATIONAL_METIER)
			.put("FAC_EC_lvl5", ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5)
			.put("FAC_EC_lvl6", ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6)


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
		loadTable(inputFile, DatabaseTableEnum.P01_ICES_LANDING, country, validate, appendData);
	}

	@Override
	public void loadTrip(File inputFile, String country, boolean validate, boolean appendData) throws IOException, FileValidationException {
		loadTable(inputFile, DatabaseTableEnum.P01_ICES_TRIP, country, validate, appendData);
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
			String[] filterCols = StringUtils.isNotBlank(country) ? new String[] { ProductIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY } : null;
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
		Files.checkExists(inputFile);

		// If not append : then remove old data
		if (!appendData) {

			if (StringUtils.isNotBlank(country)) {
				dataLoaderService.remove(table, new String[] { ProductIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY }, new String[] { country });
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
			if (table != null && (
					table == DatabaseTableEnum.P01_ICES_SPECIES_LIST
				|| table == DatabaseTableEnum.P01_ICES_SPECIES_LENGTH)
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
				String replacement = separator + headerReplacements.get(header) + separator;
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

				String recordBasename = String.format("%s-%s.%s%s", Files.getNameWithoutExtension(inputFile), recordType, Files.getExtension(inputFile), Files.TEMPORARY_FILE_DEFAULT_EXTENSION);
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
				if (table == ProductIcesStation.TABLE) {
					List<String> result = Lists.newArrayList(headers);

					// Remove missing columns in the GRB file
					result.remove(ProductIcesStation.COLUMN_FISHING_VALIDITY);

					return result.toArray(new String[result.size()]);
				}
				return headers;
			};
		}

		return null;
	}
}



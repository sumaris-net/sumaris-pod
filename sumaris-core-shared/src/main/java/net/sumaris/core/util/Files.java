package net.sumaris.core.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Files {

	private static final Logger log = LoggerFactory.getLogger(Files.class);

	public static final String TEMPORARY_FILE_DEFAULT_EXTENSION =".tmp";

	protected static NumberFormat numberFormat = NumberFormat.getInstance();
	protected static char decimalSeparator = '\0';
	protected static char inverseDecimalSeparator = ',';
	protected static Pattern scientificExpressionPattern = Pattern.compile("([0-9.]+)([Ee][-+])([0-9]+)");
	protected static DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance();

	static {
		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
		decimalSeparator = decimalFormatSymbols.getDecimalSeparator();
		if (decimalSeparator == ',') {
			inverseDecimalSeparator = '.';
		}
		decimalFormat.applyPattern("#0.#########");
	}

	public static void checkExists(File file) throws FileNotFoundException{
		Preconditions.checkNotNull(file);
		if (file.exists() == false) {
			throw new FileNotFoundException("File not exists: " + file.getAbsolutePath());
		}
	}

	public static void convert(File sourceFile, String sourceCharset, File destFile, String destCharset) throws IOException {

		log.debug(String.format("Converting file to encoding %s: %s", destCharset, sourceFile.getPath()));
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));

		byte[] buf = new byte[2048];
		int len = 0;
		while ((len = bis.read(buf)) != -1) {
			String ustf8String = new String(buf, 0, len, sourceCharset);

			bos.write(ustf8String.getBytes(destCharset));
		}

		bos.close();
		bis.close();
	}

	public static void replaceAll(File sourceFile, String regex, File destFile, String replacement) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.write(line.replaceAll(regex, replacement));
			writer.newLine();
		}

		reader.close();
		writer.close();
	}

	public static void replaceAllInHeader(File sourceFile, File destFile, Map<String, String> regexReplacementMap) throws IOException {
		replaceAll(sourceFile, destFile, regexReplacementMap, 1, 1);
	}

	public static void replaceAll(File sourceFile, Map<String, String> regexReplacementMap, int startRowNumber, int endRowNumber) throws IOException {
		// Generate a temp file name
		File destFile = getNewTemporaryFile(sourceFile);

		// Do the replace all
		replaceAll(sourceFile, destFile, regexReplacementMap, startRowNumber, endRowNumber);

		// Override the source file content
		sourceFile.delete();
		destFile.renameTo(sourceFile);
	}

	public static void replaceAll(File sourceFile, File destFile, Map<String, String> regexReplacementMap, int startRowNumber, int endRowNumber) throws IOException {
		Preconditions.checkArgument(startRowNumber >= 0);
		Preconditions.checkArgument(endRowNumber == -1 || endRowNumber >= 1);

		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

		String line;
		int counter = 0;
		while ((line = reader.readLine()) != null) {
			counter++;
			boolean apply = (counter >= startRowNumber) && (endRowNumber == -1 || counter <= endRowNumber);

			// Replace line content
			if (apply) {
				line = regexReplacementMap.keySet().stream().reduce(line, (res, regexp) -> {
						try {
							return res.replaceAll(regexp, regexReplacementMap.get(regexp));
						} catch(Throwable t) {
							throw new SumarisTechnicalException(
									String.format("Error on replacement {%s}->{%s}: %s\n", regexp, regexReplacementMap.get(regexp), t.getMessage()),
									t);
						}
				});
			}
			writer.write(line);
			writer.newLine();
		}

		reader.close();
		writer.close();
	}

	public static void filter(File sourceFile, File destFile, Predicate<String> filter) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

		String line;
		while ((line = reader.readLine()) != null) {
			if (filter.test(line)) {
				writer.write(line);
				writer.newLine();
			}
		}

		reader.close();
		writer.close();
	}

	public static void replaceAll(File sourceFile, String regex, String replacement) throws IOException {

		// Generate a temp file name
		File destFile = getNewTemporaryFile(sourceFile);

		// Do the replace all
		replaceAll(sourceFile, regex, destFile, replacement);

		// Override the source file content
		sourceFile.delete();
		destFile.renameTo(sourceFile);
	}

	public static void replaceAllExcelScientificExpression(File sourceFile, File destFile) throws IOException, ParseException {

		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

		String line = null;
		while ((line = reader.readLine()) != null) {
			line = replaceAllExcelScientificExpression(line);
			writer.write(line);
			writer.newLine();
		}

		reader.close();
		writer.close();
	}

	public static void replaceAllExcelScientificExpression(File sourceFile) throws IOException, ParseException {
		File destFile = getNewTemporaryFile(sourceFile);

		// Do the replacement
		replaceAllExcelScientificExpression(sourceFile, destFile);

		// Override the source file content
		sourceFile.delete();
		destFile.renameTo(sourceFile);
	}

	private static String replaceAllExcelScientificExpression(String line) throws ParseException {
		Matcher matcher = scientificExpressionPattern.matcher(line);

		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			// Get the first part
			String firstPartString = matcher.group(1);
			boolean hasInverseDecimalSeparator = false;
			if (firstPartString.indexOf(inverseDecimalSeparator) != -1) {
				firstPartString = firstPartString.replace(inverseDecimalSeparator, decimalSeparator);
				hasInverseDecimalSeparator = true;
			}
			double value = numberFormat.parse(firstPartString).doubleValue();

			String signString = matcher.group(2);
			int sign = 1;
			if ("e-".equalsIgnoreCase(signString)) {
				sign = -1;
			}

			// Get the exposant value
			String exposantString = matcher.group(3);
			int exposant = Integer.parseInt(exposantString);

			// Compute the value
			String valueString = decimalFormat.format(Math.pow(10, sign * exposant) * value);
			//valueString = String.valueOf(value);
			if (hasInverseDecimalSeparator) {
				valueString = valueString.replace(decimalSeparator, inverseDecimalSeparator);
			}

			matcher.appendReplacement(sb, valueString);
		}
		matcher.appendTail(sb);

		return sb.toString();
	}


	public static File getNewTemporaryFile(File sourceFile) {
		// Generate a temp file name
		File destFile = null;
		boolean tmpFileExists = true;
		int tempIndex = 0;
		while (tmpFileExists) {
			destFile = new File(sourceFile.getParent(), getNameWithoutExtension(sourceFile) + ".tmp" + tempIndex++);
			tmpFileExists = destFile.exists();
		}

		return destFile;
	}
	
	public static void removeEmptyLines(File sourceFile) throws IOException {
		// Generate a temp file name
		File destFile = getNewTemporaryFile(sourceFile);

		// Do the replace all
		removeEmptyLines(sourceFile, destFile);

		// Override the source file content
		sourceFile.delete();
		destFile.renameTo(sourceFile);
	}
	
	public static void removeEmptyLines(File sourceFile, File destFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

		String line = null;
		while ((line = reader.readLine()) != null) {
			if (StringUtils.isBlank(line) == false) {
				writer.write(line);
				writer.newLine();
			}
		}

		reader.close();
		writer.close();
	}


	public static void appendLines(File sourceFile, File destFile, String... lines) throws IOException {
		checkExists(sourceFile);

		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

		// Copy lines from file
		String line;
		while ((line = reader.readLine()) != null) {
			writer.write(line);
			writer.newLine();
		}

		// Insert lines at end
		for (String endLine : lines) {
			writer.write(endLine);
			writer.newLine();
		}

		reader.close();
		writer.close();
	}

	public static void prependLines(File sourceFile, File destFile, String... lines) throws IOException {
		checkExists(sourceFile);

		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

		// Insert lines at beginning
		for (String prependLine : lines) {
			writer.write(prependLine);
			writer.newLine();
		}

		// Copy lines from file
		String line;
		while ((line = reader.readLine()) != null) {
			writer.write(line);
			writer.newLine();
		}

		reader.close();
		writer.close();
	}

	public static Collection<String> readLines(File sourceFile, int lineCount) throws IOException {
		checkExists(sourceFile);

		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));

		List<String> result = Lists.newArrayList();

		// Copy lines from file
		String line;
		int counter = 0;
		while ((line = reader.readLine()) != null && (counter < lineCount || lineCount == -1)) {
			result.add(line);
			counter++;
		}

		reader.close();
		return result;
	}

	public static boolean isEmpty(File file) {
		return file == null || !file.exists() || file.length() == 0;
	}


	public static void deleteFiles(File folder, String fileNameRegex) {
		for (String filename : folder.list()) {
			if (filename.matches(fileNameRegex)) {
				File fileTodelete = new File(folder, filename);
				fileTodelete.delete();
			}
		}
	}

	public static boolean deleteQuietly(File file) {
		return org.apache.commons.io.FileUtils.deleteQuietly(file);
	}

	public static void deleteTemporaryFiles(File file) {
		deleteTemporaryFiles(file, TEMPORARY_FILE_DEFAULT_EXTENSION);
	}

	public static void deleteTemporaryFiles(File file, String suffix) {
		Preconditions.checkNotNull(file);

		File folder = file.getParentFile();
		String regexp = (getNameWithoutExtension(file) + suffix)
				// Protect '.' char
				.replaceAll("[.]", "[.]")
				// add counter parttern
				+ "[0-9]+";
		deleteFiles(folder, regexp);
	}

	public static void copyFile(File srcFile, File dstFile) throws IOException {
		org.apache.commons.io.FileUtils.copyFile(srcFile, dstFile);
	}

	public static String getNameWithoutExtension(File srcFile) {
		return com.google.common.io.Files.getNameWithoutExtension(srcFile.getName());
	}

	public static String getExtension(File srcFile) {
		return com.google.common.io.Files.getFileExtension(srcFile.getName());
	}

	/**
	 * <p>cleanDirectory.</p>
	 *
	 * @param directory   a {@link File} object.
	 * @param failMessage a {@link String} object.
	 */
	public static void cleanDirectory(File directory, String failMessage) {

		Path path = directory.toPath();
		int nbAttempt = 0;
		IOException lastException = null;
		while (isDirectoryNotEmpty(path) && nbAttempt < 10) {
			nbAttempt++;
			try {
				java.nio.file.Files.walkFileTree(path, new RecursiveDeleteFileVisitor(path));
				lastException = null;
			} catch (NoSuchFileException ignored) {
			} catch (AccessDeniedException ade) {
				if (java.nio.file.Files.exists(Paths.get(ade.getFile()))) {
					lastException = ade;
					break;
				}
			} catch (IOException e) {
				lastException = e;
				// wait a while
				try {
					Thread.sleep(500);
				} catch (InterruptedException ignored) {
				}
			}
		}
		if (lastException != null) {
			throw new SumarisTechnicalException(failMessage, lastException);
		}
		if (log.isWarnEnabled() && nbAttempt > 1) {
			log.warn(String.format("cleaning the directory '%s' successful after %d attempts", directory.getAbsolutePath(), nbAttempt));
		}
	}

	private static boolean isDirectoryNotEmpty(final Path path) {
		if (!java.nio.file.Files.isDirectory(path)) {
			return false;
		}
		try {
			try (DirectoryStream<Path> dirStream = java.nio.file.Files.newDirectoryStream(path)) {
				return dirStream.iterator().hasNext();
			}
		} catch (IOException e) {
			return false;
		}
	}

	private static class RecursiveDeleteFileVisitor extends SimpleFileVisitor<Path> {

		private final Path root;

		private RecursiveDeleteFileVisitor(Path root) {
			this.root = root;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

			// delete the file
			java.nio.file.Files.deleteIfExists(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

			// don't delete the root
			if (dir == root) {
				return FileVisitResult.TERMINATE;
			}

			// delete the directory
			java.nio.file.Files.deleteIfExists(dir);
			return FileVisitResult.CONTINUE;
		}

	}

	/**
	 * Charge le fichier spécifié par le chemin physique ou comme resource classpath.
	 * <br/>Cette méthode regarde d'abord si le chemin spécifié correspond à un chemin physique de fichier sur le disque dur,
	 * <br/>et dans le cas contraire tente de charger la ressource correspondante à partir du resource loader spécifié.
	 *
	 * @param path        	 Le chemin physique ou de package du fichier recherché.
	 * @param resourceLoader La resourceLoader Spring à utiliser pour charger le fichier si le chemin
	 *                    	 correspond à une resource de classpath.
	 * @return Un objet InputStream sur le fichier recherché.
	 * @throws SumarisTechnicalException if error
	 */
	public static InputStream getInputStream(String path, ResourceLoader resourceLoader) throws SumarisTechnicalException {
		InputStream is = null;

		File f = new File(path);
		if (f.exists()) {
			try {
				is = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				is = null;
			}
		} else if (resourceLoader != null) {
			Resource r = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + path);
			if (r.exists()) {
				try {
					is = r.getInputStream();
				} catch (IOException e) {
					is = null;
				}
			}
		}
		if (is == null) {
			throw new SumarisTechnicalException(String.format("Resource not found '%s'", path));
		}

		return is;
	}
	/**
	 * Charge le fichier spécifié par le chemin physique ou de package en paramètre.
	 * <br/>Cette méthode regarde d'abord si le chemin spécifié correspond à un chemin physique de fichier sur le disque dur,
	 * <br/>et dans le cas contraire tente de charger la ressource correspondante à partir su classloader spécifié.
	 *
	 * @param path        Le chemin physique ou de package du fichier recherché.
	 * @param classLoader La classloader utilisé pour charger le fichier si le chemin
	 *                    correspond à un chemin de package.
	 * @return Un objet InputStream sur le fichier recherché.
	 * @throws SumarisTechnicalException if error
	 */
	public static InputStream getInputStream(String path, ClassLoader classLoader) throws SumarisTechnicalException {
		InputStream is = null;

		File f = new File(path);
		if (f.exists()) {
			try {
				is = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				is = null;
			}
		} else if (classLoader != null) {
			is = classLoader.getResourceAsStream(path);
		}
		if (is == null) {
			throw new SumarisTechnicalException(String.format("File not found '%s'", path));
		}

		return is;
	}

	/**
	 * Crée un fichier temporaire, avec l'extension par défaut
	 * @param prefix
	 * @param directory
	 * @return
	 * @throws IOException
	 */
	public static File createTempFile(String prefix, File directory) throws IOException {
		if (directory != null && !directory.exists()) {
			directory.mkdirs();
		}

		return File.createTempFile(
				prefix,
				TEMPORARY_FILE_DEFAULT_EXTENSION,
				directory);
	}
}

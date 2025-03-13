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

package net.sumaris.core.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NonNull;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Images {

    private static final String JPG_EXTENSION = "jpg";
    private static final String JPEG_EXTENSION = "jpeg";
    private static final String PNG_EXTENSION = "png";
    public static final List<String> AVAILABLE_EXTENSION_LIST = ImmutableList.of(JPG_EXTENSION, JPEG_EXTENSION, PNG_EXTENSION);
    public static final Map<String, String> AVAILABLE_EXTENSION_BY_CONTENT_TYPE = AVAILABLE_EXTENSION_LIST.stream()
        .collect(Collectors.toMap(extension -> String.format("image/%s", extension), Function.identity()));
    public static final String AVAILABLE_EXTENSIONS = String.join("|", AVAILABLE_EXTENSION_LIST);
    public static final String AVAILABLE_CONTENT_TYPES = String.join("|", AVAILABLE_EXTENSION_BY_CONTENT_TYPE.values());
    public static final List<String> AVAILABLE_FILE_SUFFIX_LIST = ImmutableList.of(ImageType.DIAPO.suffix, ImageType.THUMBNAIL.suffix);
    public static final String AVAILABLE_FILE_SUFFIXES = String.join("|", AVAILABLE_FILE_SUFFIX_LIST);

    // Regexp to parse an image
    public static final String FILENAME_REGEXP = "^([A-Z_]+)-OBJ([0-9]+)-([0-9]+)(:?[$](:?" + AVAILABLE_FILE_SUFFIXES + "))?[.](" + AVAILABLE_EXTENSIONS + ")$";
    public static final Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_REGEXP);

    /**
     * @param imageBytes
     * @param imageContentType
     * @param objectTypeCode
     * @param objectId
     * @param imageAttachmentId
     * @param targetDir
     * @return the relative path of the image
     */
    public static String saveImage(byte[] imageBytes, String imageContentType, String objectTypeCode, Number objectId, Number imageAttachmentId, File targetDir) {

        final String fileExtension = getImageFileExtensionByContentType(imageContentType);
        final String filePath = computePath(objectTypeCode, objectId, imageAttachmentId, fileExtension);
        final File file = new File(targetDir, filePath);

        try {
            // read input image
            BufferedImage bigImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            // create folder
            FileUtils.forceMkdir(file.getParentFile());

            // original size
            ImageIO.write(bigImage, fileExtension, file);

            // resize to medium
            ImageIO.write(Scalr.resize(bigImage, ImageType.DIAPO.getMaxSize()), fileExtension, getImageFile(file, ImageType.DIAPO));

            // resize to small
            ImageIO.write(Scalr.resize(bigImage, ImageType.THUMBNAIL.getMaxSize()), fileExtension, getImageFile(file, ImageType.THUMBNAIL));
        } catch (IOException ioe) {
            throw new SumarisTechnicalException("Error while saving image", ioe);
        }

        return filePath;
    }


    public static List<File> getAllImageFiles(File baseFile) {
        return Stream.of(
            baseFile,
            getImageFile(baseFile, ImageType.DIAPO),
            getImageFile(baseFile, ImageType.THUMBNAIL)
        ).toList();
    }

    public static List<File> getAllImageFiles(File baseFile, Predicate<File> predicate) {
        return Stream.of(
            baseFile,
            getImageFile(baseFile, ImageType.DIAPO),
            getImageFile(baseFile, ImageType.THUMBNAIL)
        ).filter(predicate).toList();
    }

    public static File getImageFile(File baseImageFile, ImageType imageType) {
        return new File(baseImageFile.getParentFile(),
            String.format("%s%s.%s", FilenameUtils.getBaseName(baseImageFile.getName()), imageType.getSuffix(), getImageFileExtension(baseImageFile)));
    }

    private static String getImageFileExtensionByContentType(String contentType) {
        // Find extension
        final String fileExtension = AVAILABLE_EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase());
        if (fileExtension == null) {
            throw new SumarisTechnicalException(String.format("Invalid image content type. Actual: '%s' - Expected: '%s'", contentType, AVAILABLE_CONTENT_TYPES));
        }
        return fileExtension;
    }

    private static String getImageFileExtension(File inputFile) {
        String fileExtension = FilenameUtils.getExtension(inputFile.getName());
        if (!AVAILABLE_EXTENSION_LIST.contains(fileExtension.toLowerCase())) {
            throw new SumarisTechnicalException(String.format("Invalid image extension. Actual: '%s' - Expected: '%s'", fileExtension.toLowerCase(), AVAILABLE_EXTENSIONS));
        }
        return fileExtension;
    }

    public static String computePath(String objectTypeCode, Number objectId, Number imageAttachmentId, String fileExtension) {
        Preconditions.checkArgument(AVAILABLE_EXTENSION_LIST.contains(fileExtension));
        return String.format("%1$s/%2$s%3$s/%1$s-%2$s%3$s-%4$s.%5$s", objectTypeCode, "OBJ", objectId, imageAttachmentId, fileExtension);
    }

    /**
     * Use the filename, to recompute the full relative image path.
     * For instance: 'SAMPLE-OBJ1001-1001.png' => 'SAMPLE/OBJ1001/SAMPLE-OBJ1001-1001.png'
     *
     * @param filename a file name (with or without path)
     * @return the relative path
     */
    public static String computePath(@NonNull String filename) {
        // Remove directory (e.g. SAMPLE/OBJ60/SAMPLE-OBJ60-1001.png => SAMPLE-OBJ60-1001.png)
        filename = FilenameUtils.getName(filename);

        // Parse filename
        Matcher matches = FILENAME_PATTERN.matcher(filename);
        Preconditions.checkArgument(matches.matches(), String.format("Invalid image filename. Actual: %s - Expected: %s", filename, "<OBJECT_TYPE>-OBJ<OBJ_ID>-<ID><" + AVAILABLE_FILE_SUFFIXES + ">.<" + AVAILABLE_EXTENSIONS + ">"));

        String objectTypeCode = matches.group(1);
        int objectId = Integer.parseInt(matches.group(2));
        int imageAttachmentId = Integer.parseInt(matches.group(3));

        // Compute 'SAMPLE', 60, 1001, 'png' => SAMPLE/OBJ60/SAMPLE-OBJ60-1001.png
        return computePath(objectTypeCode, objectId, imageAttachmentId, FilenameUtils.getExtension(filename));
    }


    public static void deleteImages(Collection<File> imageFiles) {
        Beans.getStream(imageFiles).forEach(Images::deleteImage);
    }

    public static void deleteImage(@NonNull File imageFile) {
        FileUtils.deleteQuietly(imageFile);
        deleteOtherImage(imageFile);
    }

    public static void deleteOtherImage(File inputFile) {
        FileUtils.deleteQuietly(getImageFile(inputFile, ImageType.DIAPO));
        FileUtils.deleteQuietly(getImageFile(inputFile, ImageType.THUMBNAIL));
    }

    @Getter
    public enum ImageType {
        BASE("", null),
        DIAPO("$MID", 500),
        THUMBNAIL("$LOW", 200);

        private final String suffix;
        private final Integer maxSize;

        ImageType(String suffix, Integer maxSize) {
            this.suffix = suffix;
            this.maxSize = maxSize;
        }

        public static Optional<ImageType> find(String type) {
            if (type == null) return Optional.empty();
            return switch (type.toUpperCase()) {
                case "FULL", "$FULL" -> Optional.of(BASE);
                case "MID", "$MID", "MEDIUM" -> Optional.of(DIAPO);
                case "LOW", "$LOW" -> Optional.of(THUMBNAIL);
                default -> Optional.empty();
            };
        }

    }

}
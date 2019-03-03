package net.sumaris.core.util;

/*-
 * #%L
 * Quadrige3 Core :: Quadrige3 Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
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


import org.apache.commons.io.IOUtils;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * <p>ZipUtils class.</p>
 */
public class ZipUtils {

    /**
     * <p>Constructor for ZipUtils.</p>
     */
    protected ZipUtils() {
        // helper class
    }

    /**
     * <p>compressFilesInPath.</p>
     *
     * @param path a {@link String} object.
     * @param compressedFile a {@link File} object.
     * @param deleteAfterCompress a boolean.
     * @throws IOException if any.
     */
    public static void compressFilesInPath(String path, File compressedFile, boolean deleteAfterCompress) throws IOException {
        compressFilesInPath(new File(path), compressedFile, deleteAfterCompress);
    }

    /**
     * <p>compressFilesInPath.</p>
     *
     * @param fileSource a {@link File} object.
     * @param compressedFile a {@link File} object.
     * @param deleteAfterCompress a boolean.
     * @throws IOException if any.
     */
    public static void compressFilesInPath(File fileSource, File compressedFile, boolean deleteAfterCompress) throws IOException {
        ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(compressedFile)));
        compressDirectory(fileSource, fileSource, compressedFile, zout, deleteAfterCompress);
        zout.close();
    }


    /**
     * <p>uncompressFileToPath.</p>
     *
     * @param compressedFile a {@link File} object.
     * @param path a {@link String} object.
     * @param deleteAfterUncompress a boolean.
     * @throws IOException if any.
     */
    public static void uncompressFileToPath(File compressedFile, String path, boolean deleteAfterUncompress) throws IOException {
        File destDir;
        if (path != null) {
            destDir = new File(path);
        }
        else {
            destDir = new File(compressedFile.getParent());
        }
        destDir.mkdirs();

        ZipFile zipFile = new ZipFile(compressedFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory()) {
                File destFile = new File(destDir, zipEntry.getName());
                destFile.getParentFile().mkdirs();
                OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                StreamUtils.copy(zipFile.getInputStream(zipEntry), out);
                out.flush();
                out.close();
            }
        }
        zipFile.close();
        if (deleteAfterUncompress) {
            compressedFile.delete();
        }
    }

    /* -- Internal methods -- */


    private static void compressDirectory(File rootFileSource, File fileSource, File compressedFile, ZipOutputStream zout, boolean deleteAfterCompress) throws IOException {
        File[] files = fileSource.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (file.equals(compressedFile)) {
                continue;
            }
            if (file.isDirectory()) {
                String zipEntryName = toZipEntryName(rootFileSource, file);
                zout.putNextEntry(new ZipEntry(zipEntryName));
                compressDirectory(rootFileSource, file, compressedFile, zout, deleteAfterCompress);
                zout.closeEntry();
                if (deleteAfterCompress) {
                    file.delete();
                }
                continue;
            }
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            String zipEntryName = toZipEntryName(rootFileSource, file);
            zout.putNextEntry(new ZipEntry(zipEntryName));
            IOUtils.copy(in, zout);
            zout.closeEntry();
            in.close();
            if (deleteAfterCompress) {
                file.delete();
            }
        }
    }

    private static String toZipEntryName(File root, File file) {
        String result = file.getPath();
        if(root != null) {
            String rootPath = root.getPath();
            if(result.startsWith(rootPath)) {
                result = result.substring(rootPath.length());
            }
        }

        result = result.replace('\\', '/');
        if(file.isDirectory()) {
            result = result + '/';
        }

        while(result.startsWith("/")) {
            result = result.substring(1);
        }

        return result;
    }
}

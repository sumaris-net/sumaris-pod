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

package net.sumaris.core.util.file;

import java.io.*;
import java.util.regex.Pattern;

public class FileContentReplacer {

    private final Pattern pattern;
    private final String replaceMent;

    public FileContentReplacer(String pattern, String replaceMent) {
        this.pattern = Pattern.compile(pattern);
        this.replaceMent = replaceMent;
    }

    public String matchAndReplace(String line) {
        return pattern.matcher(line).replaceAll(replaceMent);
    }

    public void matchAndReplace(File inFile, File outFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inFile));
        if (!outFile.exists()) {
            outFile.createNewFile();
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile));

        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(matchAndReplace(line));
                bufferedWriter.newLine();
            }
        } finally {
            bufferedReader.close();
            bufferedWriter.flush();
            bufferedWriter.close();
        }
    }
}
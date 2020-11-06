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

package net.sumaris.core.extraction.service;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import lombok.NonNull;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.format.IExtractionFormat;
import net.sumaris.core.extraction.vo.ExtractionCategoryEnum;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.ResourceUtils;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component("extractionDocumentationService")
public class ExtractionDocumentationServiceImpl implements ExtractionDocumentationService {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ExtractionDocumentationServiceImpl.class);

    protected static final String MANUAL_CLASSPATH_DIR = ResourceLoader.CLASSPATH_URL_PREFIX + "static/manual/md/";


    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ExtractionProductService productService;

    @Autowired
    private SumarisConfiguration configuration;

    @Override
    public Optional<Resource> find(@NonNull IExtractionFormat format, @NonNull Locale locale) {

        ExtractionTypeVO type = extractionService.getByFormat(format);

        // Try to get a generic localized file
        {
            String localizedFileName = String.format("%s-v%s-%s.md",
                    StringUtils.underscoreToChangeCase(type.getRawFormatLabel()),
                    type.getVersion(),
                    locale
            );
            Resource result = getResourceOrNull(MANUAL_CLASSPATH_DIR + localizedFileName);
            if (result != null) return Optional.of(result);
        }

        // Retry without the locale
        {
            String fileName = String.format("%s-v%s.md",
                    StringUtils.underscoreToChangeCase(type.getRawFormatLabel()),
                    type.getVersion()
            );
            Resource result = getResourceOrNull(MANUAL_CLASSPATH_DIR + fileName);
            if (result != null) return Optional.of(result);
        }

        // If product: try to create doc file
        if (ExtractionCategoryEnum.PRODUCT == type.getCategory()) {

            // Computed a specific file name, for the product
            String productFileName = String.format("%s-v%s-%s.md",
                    StringUtils.underscoreToChangeCase(type.getLabel()), // Keep the full label
                    type.getVersion(),
                    locale
            );
            File productFile = new File(configuration.getTempDirectory(), productFileName);
            boolean fileExists = productFile.exists();

            ExtractionProductVO product = productService.get(type.getId(), ExtractionProductFetchOptions.MINIMAL);

            // Remove old file if update need
            long lastUpdateDate = product.getUpdateDate() != null ? product.getUpdateDate().getTime() : 0l;
            if (fileExists && lastUpdateDate > productFile.lastModified()) {
                Files.deleteQuietly(productFile);
                fileExists = false;
            }

            // Create the doc file
            if (!fileExists) {
                // If exists, use comments, or generate new documentation
                String content = StringUtils.isNotBlank(product.getComments())
                        ? product.getComments()
                        : generate(product.getId(), locale);
                try {
                    FileUtils.write(productFile, content, Charsets.UTF_8);
                } catch (IOException e) {
                    throw new SumarisTechnicalException("Unable to write manual file at:" + productFile.getAbsolutePath(), e);
                }
            }

            return ResourceUtils.findResource("file:" + productFile.getAbsolutePath());
        }

        return Optional.empty();
    }

    @Override
    public String generate(int productId, @NonNull Locale locale) {
        ExtractionProductVO source = productService.get(productId, ExtractionProductFetchOptions.builder()
            .withTables(true)
            .withColumns(true)
            .withColumnValues(true)
            .build());
        return generate(source, locale);
    }

    @Override
    public String generate(@NonNull ExtractionProductVO source, @NonNull Locale locale) {

        log.debug("Generating documentation for product {id: {}, label: '{}'}", source.getId(), source.getLabel());
        StringBuilder sb = new StringBuilder();

        Joiner columnJoiner = Joiner.on("|").useForNull("");
        Joiner valuesJoiner = Joiner.on("`, `").skipNulls();

        // Prepare header rows for table
        String headersRow = "|" + columnJoiner.join(new String[]{
                //"",
                I18n.l(locale, "sumaris.extraction.documentation.header.fieldName"),
                I18n.l(locale, "sumaris.extraction.documentation.header.type"),
                I18n.l(locale, "sumaris.extraction.documentation.header.comments")
        }) + "|\n|-- |-- |-- |";

        // Title
        sb.append("# ").append(source.getName()).append("\n\n");

        Beans.getStream(source.getTables()).forEach(table -> {

            // Add sub title
            String sectionName = getI18nTable(locale, source.getFormat(), table);
            sb.append("## ").append(sectionName).append("\n\n");

            if (StringUtils.isNotBlank(table.getDescription())) {
                sb.append(table.getDescription()).append("\n\n");
            }
            if (StringUtils.isNotBlank(table.getComments())) {
                sb.append(table.getComments()).append("\n\n");
            }

            // If not loaded of not exists
            List<ExtractionTableColumnVO> columns = table.getColumns();
            if (columns == null) {
                columns = productService.getColumnsBySheetName(source.getId(), table.getLabel());
            }

            // Add columns
            sb.append(headersRow).append("\n");
            Beans.getStream(columns)
                .filter(c -> !"hidden".equalsIgnoreCase(c.getType()))
                .sorted(Comparator.comparingInt(ExtractionTableColumnVO::getRankOrder))
                .forEach(c -> sb.append("|")
                        .append(columnJoiner.join(new String[]{
                                StringUtils.capitalize(c.getColumnName().replaceAll("_", " ")),
                                c.getType(),
                                c.getValues() != null && CollectionUtils.size(c.getValues()) < 10 ?
                                        ("`" + valuesJoiner.join(c.getValues()) + "`") : ""
                        }))
                        .append("|")
                        .append("\n"));

            // End of columns
            sb.append("\n\n");
        });

        return sb.toString();
    }

    /* -- protected methods -- */

    protected String getI18nTable(Locale locale, String format, ExtractionTableVO table) {

        String sheetName = table.getLabel().toUpperCase();
        String i18nkey = String.format("sumaris.extraction.%s.%s", format.toUpperCase(), sheetName);
        String result = I18n.l(locale, i18nkey);
        if (i18nkey.equals(result)) {
            result = I18n.l(locale, table.getName());
        }
        return result;
    }
    protected String getI18nSheetName(Locale locale, String format, String sheetName) {
        String i18key = String.format("sumaris.extraction.%s.%s", format.toUpperCase(), sheetName.toUpperCase());

        return locale != null ? I18n.l(locale, i18key) : I18n.t(i18key);
    }

    protected Resource getResourceOrNull(String location) {
        return ResourceUtils.findResource(location).orElse(null);
    }
}

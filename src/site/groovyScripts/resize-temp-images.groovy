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

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.resizers.configurations.ScalingMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Logger initialization
Logger logger = LoggerFactory.getLogger(this.class)

// Input parameters
def tempDirectory = "${project.build.directory}/site/temp"
def maxWidth = 980

// Get the list of PNG files in the temporary directory
def pngFiles = new File(tempDirectory).listFiles().findAll { it.name.endsWith('.png') }

logger.info("Resizing images at {} (max width: {}px)", tempDirectory, maxWidth)
def hasError = false
def counter = 0

// Iterate through the list of PNG files
for (File file: pngFiles) {
    try {
        // Get dimensions of the original image
        def originalImage = Thumbnails.of(file).scale(1).asBufferedImage()
        def originalWidth = originalImage.getWidth()
        def originalHeight = originalImage.getHeight()

        // Check if resizing is needed
        if (originalWidth > maxWidth) {

            println "    " + file.getAbsolutePath()

            def newHeight = ((maxWidth / originalWidth) * originalHeight).round() as int

            // Resize the image
            Thumbnails.of(file)
                    .scalingMode(ScalingMode.BICUBIC)
                    .size(maxWidth, newHeight)
                    .outputQuality(1.0)
                    .keepAspectRatio(true)
                    .toFile(file)

            counter ++
        }
    } catch (Exception e) {
        logger.error("Error processing image: {}. Reason: {}", file.getAbsolutePath(), e.message)
        hasError = true
    }
}

if (hasError) {
    throw new RuntimeException("Errors occurred while processing the images.")
}
else if (counter > 0) {
    logger.info("Resized {} images", counter)
}
else {
    logger.info("No images resized")
}

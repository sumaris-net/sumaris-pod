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

package net.sumaris.core.util.sound;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

@Slf4j
public class SoundUtils {

    public static float SAMPLE_RATE = 8000f;

    public static void tone(int hz, int msecs)
        throws LineUnavailableException
    {
        tone(hz, msecs, 1.0);
    }

    public static void tone(int hz, int msecs, double vol)
        throws LineUnavailableException
    {
        byte[] buf = new byte[1];
        AudioFormat af =
            new AudioFormat(
                SAMPLE_RATE, // sampleRate
                8,           // sampleSizeInBits
                1,           // channels
                true,        // signed
                false);      // bigEndian
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        for (int i=0; i < msecs*8; i++) {
            double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
            buf[0] = (byte)(Math.sin(angle) * 127.0 * vol);
            sdl.write(buf,0,1);
        }
        sdl.drain();
        sdl.stop();
        sdl.close();
    }

    public static void playError(int count) {

        int counter = 0;
        try {
            while (counter < count) {
                SoundUtils.tone(450, 800, 0.7);
                Thread.sleep(800);
                SoundUtils.tone(400, 800, 0.7);
                Thread.sleep(800);
                SoundUtils.tone(400, 2000);
                Thread.sleep(3000);
                counter++;
            }
        }
        catch (Exception e) {
            log.debug("Cannot play error sound: " + e.getMessage());
        }
    }

    public static void playWaiting(int count) {
        int counter = 0;
        try {
            while (counter < count) {
                SoundUtils.tone(100, 100);
                Thread.sleep(200);
                SoundUtils.tone(500, 700);
                Thread.sleep(800);
                SoundUtils.tone(500, 1000, 0.4);
                Thread.sleep(3000);
                counter++;
            }
        }
        catch (Exception e) {
            log.debug("Cannot play error sound: " + e.getMessage());
        }
    }

    public static void playFinished() {
        try {
            SoundUtils.tone(800, 100);
            Thread.sleep(200);
            SoundUtils.tone(800, 700, 0.7);
            Thread.sleep(350);
            SoundUtils.tone(800, 1000);
            Thread.sleep(3000);
        }
        catch (Exception e) {
            log.debug("Cannot play error sound: " + e.getMessage());
        }
    }

    private void playSound(File f) {
        Preconditions.checkArgument(f.exists());

        Runnable r = new Runnable() {
            private File f;

            public void run() {
                playSoundInternal(this.f);
            }

            public Runnable setFile(File f) {
                this.f = f;
                return this;
            }
        }.setFile(f);

        new Thread(r).start();

    }

    private void playSoundInternal(File f) {

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
            try {
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                try {
                    clip.start();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    clip.drain();
                } finally {
                    clip.close();
                }
            } catch (LineUnavailableException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                audioInputStream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
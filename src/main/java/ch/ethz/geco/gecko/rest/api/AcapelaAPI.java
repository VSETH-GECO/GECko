/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package ch.ethz.geco.gecko.rest.api;

import ch.ethz.geco.gecko.ErrorHandler;
import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.rest.RequestBuilder;
import ch.ethz.geco.gecko.rest.api.exception.APIException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A wrapper for the unofficial Acapela API
 */
public class AcapelaAPI {
    private static final String API_URL = "http://dmbx.acapela-group.com/DemoHTML5Form_V2.php";
    private static final String DEFAULT_LANG = "sonid10";
    private static final String DEFAULT_VOICE = "WillLittleCreature (emotive voice)";
    private static final String BG_SAMPLE_PATH = "BG_SAMPLE.mp3";

    private static final Pattern langPattern = Pattern.compile("id\\s*=\\s*\"[^\"]+\"\\s*value\\s*=\\s*\"([^\"]+)\"[^>]+>\\s*([^<]+\\s?[^\\s<])");
    private static final Pattern voicePattern = Pattern.compile("class\\s*=\\s*\"allvoices\\s*([^\\s]+)\\s*[^\"]*\"\\s*value\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern soundPattern = Pattern.compile("var myPhpVar\\s*=\\s*['\"]([^'\"]+)['\"];");

    private static Map<String, Language> languages;

    /**
     * Loads the sound of the given message from Acapela, cleans it up and saves it into a wave file.
     *
     * @param message the message to save
     * @return if the operation was successful
     */
    public static boolean soundToWav(String message) {
        AudioInputStream bgSample = null;
        AudioInputStream msgSample = null;
        try {
            bgSample = getSoundSamples(new File(BG_SAMPLE_PATH).toURI().toURL());
            msgSample = getSoundSamples(new URL(getSoundURL(message)));
        } catch (MalformedURLException e) {
            ErrorHandler.handleError(e);
        }

        if (msgSample != null && bgSample != null) {
            try {
                int frameSize = msgSample.getFormat().getFrameSize();
                ByteArrayOutputStream cleanBytes = new ByteArrayOutputStream();

                byte[] msgBytes = new byte[frameSize];
                byte[] bgBytes = new byte[frameSize];

                int samples = 0;
                while (msgSample.read(msgBytes) != -1) {
                    if (bgSample.read(bgBytes) == -1) {
                        ErrorHandler.handleError(new APIException("Message sound is longer than background sample."));
                    }

                    int msgInt = ByteBuffer.wrap(msgBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
                    int bgInt = ByteBuffer.wrap(bgBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    // Cap under/overflows
                    int diff = msgInt - bgInt;
                    if (diff > Short.MAX_VALUE) {
                        diff = Short.MAX_VALUE;
                    }

                    if (diff < Short.MIN_VALUE) {
                        diff = Short.MIN_VALUE;
                    }

                    // Copy clean bytes to target buffer
                    cleanBytes.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) diff).array());

                    samples++;
                }

                GECkO.logger.debug("[AcapelaAPI] Processed " + samples + " samples.");

                return writeWav(cleanBytes.toByteArray(), msgSample.getFormat());
            } catch (IOException e) {
                ErrorHandler.handleError(e);
            }
        }

        return false;
    }

    /**
     * Writes the given samples into a wave file with the given format.
     *
     * @param samples the samples to write
     * @param format  the format to use
     * @return if the operation was successful
     */
    private static boolean writeWav(byte[] samples, AudioFormat format) {
        try {
            DataOutputStream wavFile = new DataOutputStream(new FileOutputStream("temp.wav"));
            int sampleRate = (int) format.getSampleRate();
            short bitsPerSample = (short) format.getSampleSizeInBits();
            short bytesPerSample = (short) (bitsPerSample / 8);
            int byteRate = sampleRate * bytesPerSample;

            // Write RIFF chunk descriptor
            wavFile.writeBytes("RIFF");
            wavFile.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36 + samples.length).array());
            wavFile.writeBytes("WAVE");

            // Write fmt chunk
            wavFile.writeBytes("fmt ");
            wavFile.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array());                // Chunk size 16 for PCM
            wavFile.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 1).array());       // Audio format 1=PCM
            wavFile.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 1).array());       // # of channels
            wavFile.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array());        // Sample rate
            wavFile.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate).array());          // Byte rate
            wavFile.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(bytesPerSample).array());  // Block align
            wavFile.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(bitsPerSample).array());   // Bits per sample

            // Write data chunk
            wavFile.writeBytes("data");
            wavFile.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(samples.length).array());    // Chunk size of the data
            wavFile.write(samples);

            wavFile.flush();
            GECkO.logger.debug("[AcapelaAPI] Written " + wavFile.size() + " bytes into wave file.");
            wavFile.close();
            return true;
        } catch (IOException e) {
            ErrorHandler.handleError(e);
        }

        return false;
    }

    /**
     * Returns an audio stream from an mp3 file or url.
     *
     * @param path the file url
     * @return the audio stream of the given file
     */
    @Nullable
    private static AudioInputStream getSoundSamples(URL path) {
        try {
            AudioInputStream in = AudioSystem.getAudioInputStream(path);
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            return AudioSystem.getAudioInputStream(decodedFormat, in);
        } catch (UnsupportedAudioFileException | IOException e) {
            ErrorHandler.handleError(e);
        }

        return null;
    }

    /**
     * Loads the possible languages and voices supported by Acapela.
     *
     * @return if the operation was successful
     */
    private static boolean loadData() {
        try {
            languages = new HashMap<>();
            HttpResponse response = new RequestBuilder(API_URL).addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setPayload(Collections.singletonMap("SendToVaaS", "")).post();
            StatusLine statusLine = response.getStatusLine();

            switch (statusLine.getStatusCode()) {
                case 200:
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                    String text = writer.toString();

                    Matcher langMatcher = langPattern.matcher(text);
                    while (langMatcher.find()) {
                        languages.put(langMatcher.group(1), new Language(langMatcher.group(1), langMatcher.group(2)));
                    }

                    Matcher voiceMatcher = voicePattern.matcher(text);
                    while (voiceMatcher.find()) {
                        languages.get(voiceMatcher.group(1)).getVoices().add(voiceMatcher.group(2));
                    }

                    return true;
                default:
                    ErrorHandler.handleError(new APIException(statusLine.getStatusCode() + statusLine.getReasonPhrase()));
                    break;
            }
        } catch (IOException e) {
            ErrorHandler.handleError(e);
        }

        return false;
    }

    /**
     * Converts the given message to a sound and returns the sound url using the given language and voice.
     *
     * @param langID  the language to use
     * @param voiceID the voice to use
     * @param message the message to convert
     * @return the sound url or null if something went wrong
     */
    @Nullable
    private static String getSoundURL(String langID, String voiceID, String message) {
        if (languages == null) {
            loadData();
        }

        if (languages != null && languages.get(langID).getVoices().contains(voiceID)) {
            HttpResponse response;
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("MyLanguages", langID);
                payload.put("MySelectedVoice", voiceID);
                payload.put("MyTextForTTS", message);
                payload.put("SendToVaaS", "");

                response = new RequestBuilder(API_URL).addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setPayload(payload).post();
                StatusLine statusLine = response.getStatusLine();

                switch (statusLine.getStatusCode()) {
                    case 200:
                        HttpEntity entity = response.getEntity();
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                        String text = writer.toString();

                        Matcher soundMatcher = soundPattern.matcher(text);

                        if (soundMatcher.find()) {
                            return soundMatcher.group(1);
                        }

                        break;
                    default:
                        ErrorHandler.handleError(new APIException(statusLine.getStatusCode() + statusLine.getReasonPhrase()));
                        break;
                }
            } catch (IOException e) {
                ErrorHandler.handleError(e);
            }
        } else {
            ErrorHandler.handleError(new APIException("Unknown language or voice."));
        }

        return null;
    }

    /**
     * Converts the given message to a sound and returns the sound url using the default language and voice.
     *
     * @param message the message to convert
     * @return the sound url
     */
    private static String getSoundURL(String message) {
        return getSoundURL(DEFAULT_LANG, DEFAULT_VOICE, message);
    }

    /**
     * A class representing a language in the Acapela API.
     */
    public static class Language {
        /**
         * The language id.
         */
        private String id;

        /**
         * The name of the language.
         */
        private String name;

        /**
         * The available voices.
         */
        private List<String> voices;

        /**
         * Creates a new Language object.
         *
         * @param id   the id of the language
         * @param name the name of the language
         */
        public Language(String id, String name) {
            this.id = id;
            this.name = name;
            this.voices = new ArrayList<>();
        }

        /**
         * Returns the ID of this language.
         *
         * @return the ID of this language
         */
        public String getID() {
            return id;
        }

        /**
         * Returns the name of this language.
         *
         * @return the name of this language
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the available voices of this language.
         *
         * @return the voices of this language
         */
        public List<String> getVoices() {
            return voices;
        }
    }

    /**
     * Returns all supported languages.
     *
     * @return all supported languages
     */
    public static Map<String, Language> getLanguages() {
        if (languages == null) {
            loadData();
        }

        return languages;
    }
}

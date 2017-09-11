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
import ch.ethz.geco.gecko.rest.RequestBuilder;
import ch.ethz.geco.gecko.rest.api.exception.APIException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
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

    private static final Pattern langPattern = Pattern.compile("id\\s*=\\s*\"[^\"]+\"\\s*value\\s*=\\s*\"([^\"]+)\"[^>]+>\\s*([^<]+\\s?[^\\s<])");
    private static final Pattern voicePattern = Pattern.compile("class\\s*=\\s*\"allvoices\\s*([^\\s]+)\\s*\"\\s*value\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern soundPattern = Pattern.compile("var myPhpVar\\s*=\\s*['\"]([^'\"]+)['\"];");

    private static Map<String, Language> languages;

    /**
     * Loads the possible languages and voices supported by Acapela.
     *
     * @return if the operation was successful
     */
    public static boolean loadData() {
        try {
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
                        languages.get(langMatcher.group(1)).getVoices().add(langMatcher.group(2));
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
    public static String getSoundURL(String langID, String voiceID, String message) {
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
        }

        return null;
    }

    /**
     * Converts the given message to a sound and returns the sound url using the default language and voice.
     *
     * @param message the message to convert
     * @return the sound url
     */
    public static String getSoundURL(String message) {
        return getSoundURL(DEFAULT_LANG, DEFAULT_VOICE, message);
    }

    /**
     * A class representing a language in the Acapela API.
     */
    private static class Language {
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
}

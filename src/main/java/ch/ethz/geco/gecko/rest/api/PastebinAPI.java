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
 * For more information, please refer to <http://unlicense.org>
 */

package ch.ethz.geco.gecko.rest.api;

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.rest.RequestBuilder;
import ch.ethz.geco.gecko.rest.api.exception.APIException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PastebinAPI {
    private static final String API_URL = "http://pastebin.com/api/";

    /**
     * Tries to create a new paste.
     *
     * @param title the title of the paste
     * @param text  the content of the paste
     * @return a link to the newly created paste
     */
    public static String createPaste(String title, String text, String expire, boolean isUnlisted) throws APIException {
        // Preparing payload, see: http://pastebin.com/api
        Map<String, String> payload = new HashMap<>();

        // Required fields
        payload.put("api_dev_key", ConfigManager.getProperties().getProperty("pastebin_apiKey"));
        payload.put("api_option", "paste");
        payload.put("api_paste_code", text);

        // Optional fields
        payload.put("api_paste_name", title);
        payload.put("api_paste_private", String.valueOf(isUnlisted ? 1 : 0));
        payload.put("api_paste_expire_date", expire);

        try {
            HttpResponse response = new RequestBuilder(API_URL + "api_post.php").setPayload(payload).post();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case 200:
                    // Success
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                    String content = writer.toString();

                    if (content.startsWith("Bad API request")) {
                        throw new APIException(content);
                    } else {
                        // Everything seems to be ok
                        return content;
                    }
                default:
                    // HTTP errors
                    GECkO.logger.error("[PasteBin] An HTTP error occurred: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
                    break;
            }
        } catch (IOException e) {
            GECkO.logger.error("[PastebinAPI] A connection error occurred: ");
            e.printStackTrace();
        }

        return null;
    }
}

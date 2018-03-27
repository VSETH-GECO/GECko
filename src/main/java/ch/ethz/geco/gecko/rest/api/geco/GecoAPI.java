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

package ch.ethz.geco.gecko.rest.api.geco;

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.rest.RequestBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A wrapper for the GECO Web API.
 */
public class GecoAPI {
    private static final String API_URL = "https://geco.ethz.ch/api/v2/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tries to get the website user info of the user with the given discord user ID via the API.
     *
     * @param discordID the discord user ID we want to query
     * @return the user info of the website user with the given discord ID, null otherwise
     * @throws NoSuchElementException if there is no user linked to the given discord ID
     */
    @Nullable
    public static UserInfo getUserInfoByDiscordID(long discordID) throws NoSuchElementException {
        try {
            HttpResponse response = new RequestBuilder(API_URL + "user/discord/" + discordID)
                    .addHeader("X-API-KEY", ConfigManager.getProperties().getProperty("geco_apiKey"))
                    .ignoreSSL().get();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case 200:
                    // Success
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                    String json = writer.toString();

                    JsonNode jsonNode = objectMapper.readTree(json);

                    if (jsonNode != null) {
                        if (!jsonNode.has("id")) {
                            GECkO.logger.error("[GecoAPI] Missing field: id");
                            return null;
                        }

                        if (!jsonNode.has("name")) {
                            GECkO.logger.error("[GecoAPI] Missing field: name");
                            return null;
                        }

                        if (!jsonNode.has("usergroup")) {
                            GECkO.logger.error("[GecoAPI] Missing field: usergroup");
                            return null;
                        }

                        if (!jsonNode.has("lol")) {
                            GECkO.logger.error("[GecoAPI] Missing field: lol");
                            return null;
                        }

                        if (!jsonNode.has("steam")) {
                            GECkO.logger.error("[GecoAPI] Missing field: steam");
                            return null;
                        }

                        if (!jsonNode.has("bnet")) {
                            GECkO.logger.error("[GecoAPI] Missing field: bnet");
                            return null;
                        }

                        if (!jsonNode.has("discord")) {
                            GECkO.logger.error("[GecoAPI] Missing field: discord");
                            return null;
                        }

                        Map<UserInfo.AccountType, String> accounts = new HashMap<>();
                        accounts.put(UserInfo.AccountType.RIOT, jsonNode.get("lol").asText());
                        accounts.put(UserInfo.AccountType.STEAM, jsonNode.get("steam").asText());
                        accounts.put(UserInfo.AccountType.BLIZZARD, jsonNode.get("bnet").asText());
                        accounts.put(UserInfo.AccountType.DISCORD, jsonNode.get("discord").asText());

                        return new UserInfo(jsonNode.get("id").asInt(), jsonNode.get("name").asText(), jsonNode.get("usergroup").asText(), accounts);
                    } else {
                        GECkO.logger.error("[GecoAPI] Could not parse json response.");
                    }

                    break;
                case 404:
                    // User not found
                    throw new NoSuchElementException();
                default:
                    // Other API errors
                    GECkO.logger.error("[GecoAPI] An API error occurred: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
                    break;
            }
        } catch (IOException e) {
            GECkO.logger.error("[GecoAPI] A Connection error occurred: ");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Tries to load lan user information for the given user.
     *
     * @param info the info of the user to load
     * @return true on success, false otherwise
     */
    public static boolean loadLanUser(UserInfo info) {
        try {
            HttpResponse response = new RequestBuilder(API_URL + "lan/search/user/" + info.getUsername())
                    .addHeader("X-API-KEY", ConfigManager.getProperties().getProperty("geco_apiKey"))
                    .ignoreSSL().get();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case 200:
                    // Success
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                    String json = writer.toString();

                    JsonNode jsonNode = objectMapper.readTree(json);

                    if (jsonNode != null) {
                        if (!jsonNode.has("id")) {
                            GECkO.logger.error("[GecoAPI] Missing field: id");
                            return false;
                        }

                        if (!jsonNode.has("seat")) {
                            GECkO.logger.error("[GecoAPI] Missing field: seat");
                            return false;
                        }

                        if (!jsonNode.has("first_name")) {
                            GECkO.logger.error("[GecoAPI] Missing field: first_name");
                            return false;
                        }

                        if (!jsonNode.has("last_name")) {
                            GECkO.logger.error("[GecoAPI] Missing field: last_name");
                            return false;
                        }

                        if (!jsonNode.has("birthday")) {
                            GECkO.logger.error("[GecoAPI] Missing field: birthday");
                            return false;
                        }

                        info.addLanUser(jsonNode.get("id").asInt(), jsonNode.get("seat").asText(), jsonNode.get("first_name").asText(), jsonNode.get("last_name").asText(), jsonNode.get("birthday").asLong());
                        return true;
                    } else {
                        GECkO.logger.error("[GecoAPI] Could not parse json response.");
                    }

                    break;
                case 404:
                    // User not found
                    throw new NoSuchElementException();
                default:
                    // Other API errors
                    GECkO.logger.error("[GecoAPI] An API error occurred: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
                    break;
            }
        } catch (IOException e) {
            GECkO.logger.error("[GecoAPI] A Connection error occurred: ");
            e.printStackTrace();
        }

        return false;
    }
}

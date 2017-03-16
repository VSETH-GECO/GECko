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
import ch.ethz.geco.gecko.rest.gson.GsonManager;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class GecoAPI {
    private static final String API_URL = "https://geco.ethz.ch/api/v1/";

    /**
     * Tries to get the website user info of the user with the given discord user ID via the API.
     *
     * @param discordID the discord user ID we want to query
     * @return the user info of the website user with the given discord ID, null otherwise
     * @throws NoSuchElementException if there is no user linked to the given discord ID
     */
    public static UserInfo getUserInfoByDiscordID(String discordID) throws NoSuchElementException {
        try {
            HttpResponse response = new RequestBuilder(API_URL + "user/discord/" + discordID)
                    .addHeader("Authorization", "Token token=" + ConfigManager.getProperties().getProperty("gecoAPIKey"))
                    .get();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case 200:
                    // Success
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                    String json = writer.toString();

                    return GsonManager.getGson().fromJson(json, UserInfo.class);
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
     * A subclass representing a user on the website
     */
    public static class UserInfo {
        int userID;
        String username;
        int posts;
        int threads;
        Map<String, String> accounts;

        public UserInfo(int userID, String username, int posts, int threads, Map<String, String> accounts) {
            this.userID = userID;
            this.username = username;
            this.posts = posts;
            this.threads = threads;
            this.accounts = accounts;
        }

        public int getUserID() {
            return userID;
        }

        public String getUsername() {
            return username;
        }

        public int getPosts() {
            return posts;
        }

        public int getThreads() {
            return threads;
        }

        public Map<String, String> getAccounts() {
            return accounts;
        }
    }

    /**
     * Tries to get the lan user information.
     *
     * @return a list of lan users if successful, null otherwise
     */
    public static List<LanUser> getLanUsers() {
        try {
            HttpResponse response = new RequestBuilder(API_URL + "lan/seats/").get();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case 200:
                    // Success
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                    String json = writer.toString();

                    return GsonManager.getGson().fromJson(json, new TypeToken<List<GecoAPI.LanUser>>() {
                    }.getType());
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
     * A subclass representing a lan user on the website
     */
    public static class LanUser {
        int seatID;
        String seatName;
        int lanUserID;
        int status;
        String userName;

        public LanUser(int seatID, String seatName, int lanUserID, int status, String userName) {
            this.seatID = seatID;
            this.seatName = seatName;
            this.lanUserID = lanUserID;
            this.status = status;
            this.userName = userName;
        }

        public int getSeatID() {
            return seatID;
        }

        public String getSeatName() {
            return seatName;
        }

        public int getLanUserID() {
            return lanUserID;
        }

        public int getStatus() {
            return status;
        }

        public String getUserName() {
            return userName;
        }
    }
}

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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A wrapper for the GECO Web API.
 */
public class GecoAPI {
    private static final String API_URL = "https://geco.ethz.ch/api/v1/";
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
                    .addHeader("Authorization", "Token token=" + ConfigManager.getProperties().getProperty("geco_apiKey"))
                    .get();
            StatusLine statusLine = response.getStatusLine();
            switch (statusLine.getStatusCode()) {
                case 200:
                    // Success
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, StandardCharsets.UTF_8);
                    String json = writer.toString();

                    // Deserialize
                    return objectMapper.readValue(json, UserInfo.class);
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

        public UserInfo(@JsonProperty("id") int userID, @JsonProperty("name") String username, @JsonProperty("posts") int posts, @JsonProperty("threads") int threads) {
            this.userID = userID;
            this.username = username;
            this.posts = posts;
            this.threads = threads;
        }

        @JsonProperty("accounts")
        private void loadAccounts(List<Map<String, String>> accounts) {
            for (Map<String, String> account : accounts) {
                this.accounts.put(account.get("type"), account.get("id"));
            }
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
    @Nullable
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

                    return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, LanUser.class));
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
        private Integer seatID;
        private String seatName;
        private Integer webUserID;
        private Integer lanUserID;
        private Integer status;
        private String userName;

        public LanUser(@JsonProperty("id") Integer seatID, @JsonProperty("seatNumber") String seatName, @JsonProperty("web_user_id") Integer webUserID,
                       @JsonProperty("lan_user_id") Integer lanUserID, @JsonProperty("status") Integer status, @JsonProperty("username") String userName) {
            this.seatID = seatID;
            this.seatName = seatName;
            this.webUserID = webUserID;
            this.lanUserID = lanUserID;
            this.status = status;
            this.userName = userName;
        }

        /**
         * Returns the seat ID of this lan user.
         *
         * @return the seat ID
         */
        public Integer getSeatID() {
            return seatID;
        }

        /**
         * Returns the seat name of this lan user.
         *
         * @return
         */
        public String getSeatName() {
            return seatName;
        }

        /**
         * Returns the web user ID of this lan user.
         *
         * @return the web user ID
         */
        public Integer getWebUserID() {
            return webUserID;
        }

        /**
         * Returns the lan user ID.
         *
         * @return the lan user ID
         */
        public Integer getLanUserID() {
            return lanUserID;
        }

        /**
         * Returns the payment status of this lan user.
         *
         * @return the payment status
         */
        public Integer getStatus() {
            return status;
        }

        /**
         * Returns the user name of this lan user.
         *
         * @return the user name
         */
        public String getUserName() {
            return userName;
        }
    }
}

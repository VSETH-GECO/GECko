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

package ch.ethz.geco.gecko.rest;

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.ErrorHandler;
import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.MediaSynchronizer;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.io.IOUtils;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

import java.io.IOException;
import java.io.InputStream;

/* Web API v2 Endpoints:
 * - GET /api/v2/web/news
 * - GET /api/v2/web/events
 * - GET /api/v2/web/news/:id
 * - GET /api/v2/web/events/:id
 *
 * GECkO API Endpoints:
 * - POST, DELETE /news/:id
 * - POST, DELETE /events/:id
 * - POST, DELETE /news/raw/:id
 * - POST, DELETE /events/raw/:id
 */

/**
 * A lightweight web server to be able to handle HTTP request for web hooks.
 * <p>
 * Media Sync Endpoints:
 * <ul>
 * <li> POST, DELETE /news/:id
 * <li> POST, DELETE /events/:id
 * </ul>
 * See <a href="https://discordapp.com/developers/docs/resources/channel#embed-object-embed-structure">Embed Object Structure</a> for more details on how to structure the payload.
 */
public class WebHookServer extends NanoHTTPD {
    public WebHookServer(int port) {
        super(port);

        try {
            //makeSecure(makeSSLSocketFactory("stammgruppe.jks", "123321".toCharArray()), null);
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            GECkO.logger.info("Started Web Hook Server on port: " + port);
        } catch (IOException e) {
            ErrorHandler.handleError(e);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String authHeader = session.getHeaders().get("authorization");
        System.out.println(authHeader);
        if (authHeader == null || !authHeader.equals("Token token="+ ConfigManager.getProperties().getProperty("geco_apiKey"))) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Unauthorized!");
        }

        // Read request body into string
        int len = 0;
        try {
            len = Integer.valueOf(session.getHeaders().get("content-length"));
        } catch (NumberFormatException ignored) {
        }
        byte[] byteContent = new byte[len];
        InputStream inputStream = session.getInputStream();
        String content = "";
        try {
            int bytesRead = inputStream.read(byteContent);
            content = IOUtils.toString(byteContent, "UTF-8");
        } catch (IOException e) {
            ErrorHandler.handleError(e);
        }

        // Determine endpoint
        String[] endpoints = session.getUri().substring(1).split("/");

        Method method = session.getMethod();
        switch (method) {
            case GET:
                return handleGet(endpoints);
            case DELETE:
                return handleDelete(endpoints);
            case POST:
                return handlePost(endpoints, content);
            case PATCH:
                return handlePatch(endpoints, content);
            default:
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Unsupported method");
        }
    }

    private Response handleGet(String[] endpoints) {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - Nope");
    }

    private Response handleDelete(String[] endpoints) {
        if (endpoints.length > 1) {
            switch (endpoints[0]) {
                case "news":
                    MediaSynchronizer.deleteNews(Integer.valueOf(endpoints[1]));
                    break;
                case "events":
                    MediaSynchronizer.deleteEvent(Integer.valueOf(endpoints[1]));
                    break;
                default:
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - Nope");
            }

            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Success!");
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - Nope");
    }

    private Response handlePost(String[] endpoints, String content) {
        if (endpoints.length > 1) {
            switch (endpoints[0]) {
                case "news":
                    try {
                        EmbedObject embed = DiscordUtils.MAPPER.readValue(content, EmbedObject.class);
                        MediaSynchronizer.setNews(Integer.valueOf(endpoints[1]), embed);
                    } catch (JsonParseException | JsonMappingException e) {
                        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", e.getMessage());
                    } catch (IOException e) {
                        ErrorHandler.handleError(e);
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error. Try again or call Dave.");
                    }
                    break;
                case "events":
                    try {
                        EmbedObject embed = DiscordUtils.MAPPER.readValue(content, EmbedObject.class);
                        MediaSynchronizer.setEvent(Integer.valueOf(endpoints[1]), embed);
                    } catch (JsonParseException | JsonMappingException e) {
                        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", e.getMessage());
                    } catch (IOException e) {
                        ErrorHandler.handleError(e);
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error. Try again or call Dave.");
                    }
                    break;
                default:
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - Nope");
            }

            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Success!");
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 - Nope");
    }

    private Response handlePatch(String[] endpoints, String content) {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "Success!");
    }
}

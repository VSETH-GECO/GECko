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

package ch.ethz.geco.gecko.rest.gson.deserializer;

import ch.ethz.geco.gecko.rest.api.GecoAPI;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LanUserDeserializer implements JsonDeserializer<List<GecoAPI.LanUser>> {
    @Override
    public List<GecoAPI.LanUser> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonLanUsers = json.getAsJsonArray();

        // TODO: maybe save it in a map with key = id or lanid for faster and easier lookup
        List<GecoAPI.LanUser> lanUsers = new ArrayList<>();
        for (JsonElement jsonLanUser : jsonLanUsers) {
            JsonObject jsonLanUserObject = jsonLanUser.getAsJsonObject();
            lanUsers.add(new GecoAPI.LanUser(jsonLanUserObject.get("id").getAsInt(), jsonLanUserObject.get("lan_user2017_id").getAsInt(), jsonLanUserObject.get("status").getAsInt(), jsonLanUserObject.get("username").getAsString()));
        }

        return lanUsers;
    }
}

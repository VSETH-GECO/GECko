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

package ch.ethz.geco.gecko.rest.gson.deserializer;

import ch.ethz.geco.gecko.rest.api.GecoAPI;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class UserInfoDeserializer implements JsonDeserializer<GecoAPI.UserInfo> {
    @Override
    public GecoAPI.UserInfo deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonUserInfo = (JsonObject) jsonElement;

        // Parse accounts
        Map<String, String> accounts = new HashMap<>();
        JsonArray jsonAccounts = jsonUserInfo.getAsJsonArray("accounts");
        for (JsonElement jsonAccount : jsonAccounts) {
            JsonObject jsonAccountObject = jsonAccount.getAsJsonObject();
            accounts.put(jsonAccountObject.get("type").getAsString(), jsonAccountObject.get("id").getAsString());
        }

        return new GecoAPI.UserInfo(jsonUserInfo.get("id").getAsInt(), jsonUserInfo.get("name").getAsString(), jsonUserInfo.get("posts").getAsInt(), jsonUserInfo.get("threads").getAsInt(), accounts);
    }
}

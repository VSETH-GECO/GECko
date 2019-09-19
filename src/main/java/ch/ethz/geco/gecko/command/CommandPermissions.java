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

package ch.ethz.geco.gecko.command;

import ch.ethz.geco.gecko.GECko;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages permissions for commands.
 * TODO: somehow add method to check for discord permissions in addition to roles
 */
public class CommandPermissions {
    private final Set<Snowflake> permittedRoleIDs = new HashSet<>();

    /**
     * Returns a Set of permitted role ID's.
     * @return the permitted role ID's
     */
    public Set<Snowflake> getPermittedRoleIDs() {
        return permittedRoleIDs;
    }

    /**
     * Checks if a member is permitted (has one the roles specified as permitted in the command)
     * @param member The member to check
     * @return Whether or not a member is permitted.
     */
    public boolean isMemberPermitted(Member member) {
        if (permittedRoleIDs.isEmpty()) {
            return true;
        }

        for (Snowflake userRole : member.getRoleIds()) {
            if (permittedRoleIDs.contains(userRole)) {
                return true;
            }
        }

        return false;
    }

    public boolean isUserPermitted(User user) {
        return isMemberPermitted(GECko.mainGuild.getMemberById(user.getId()).block());
    }
}

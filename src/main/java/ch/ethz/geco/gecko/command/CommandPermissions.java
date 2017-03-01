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

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages permissions for commands.
 * TODO: somehow add method to check for discord permissions in addition to roles
 */
public class CommandPermissions {
    private Set<String> permittedRoleIDs = new HashSet<>();

    /**
     * Returns a Set of permitted role ID's.
     * @return the permitted role ID's
     */
    public Set<String> getPermittedRoleIDs() {
        return permittedRoleIDs;
    }

    /**
     * Return whether or not a user is permitted to execute a command.
     *
     * @param guild the guild where to check the roles
     * @param user  the user to check
     * @return whether or not a user is permitted
     */
    public boolean isUserPermitted(IGuild guild, IUser user) {
        if (permittedRoleIDs.isEmpty()) {
            return true;
        }

        for (IRole userRole : user.getRolesForGuild(guild)) {
            if (permittedRoleIDs.contains(userRole.getID())) {
                return true;
            }
        }

        return false;
    }
}

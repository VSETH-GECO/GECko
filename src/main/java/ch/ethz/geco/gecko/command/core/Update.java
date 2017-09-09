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

package ch.ethz.geco.gecko.command.core;

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.ErrorHandler;
import ch.ethz.geco.gecko.Updater;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.rest.api.PastebinAPI;
import ch.ethz.geco.gecko.rest.api.exception.APIException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class Update extends Command {
    public Update() {
        this.setName("update");
        this.setParams("[branch]");
        this.setDescription("Updates the bot to the given branch or to master if no branch is given.");
    }

    /**
     * If the bot is currently updating.
     */
    private static volatile boolean isUpdating = false;

    /**
     * The build path.
     */
    private static final String buildPath = "build";

    /**
     * Builds the update status embed.
     *
     * @param gitStatus    the status of git
     * @param mvnStatus    the status of mvn
     * @param updateStatus the general status of the update
     * @return the embed, ready to be sent
     */
    private static EmbedObject buildEmbed(String gitStatus, String mvnStatus, String updateStatus) {
        EmbedBuilder embedBuilder = new EmbedBuilder().withColor(new Color(80, 100, 255))
                .withAuthorName("Update Status").withFooterText(updateStatus);

        String description = "**Git:** " + gitStatus + "\n**Mvn:** " + mvnStatus;
        return embedBuilder.withDescription(description).build();
    }

    // TODO: maybe check if bot is already up-to-date
    @Override
    public void execute(IMessage msg, List<String> args) {
        // Synchronize this so we don't have threading issues
        synchronized (this) {
            if (!isUpdating) {
                isUpdating = true;
                // Decide target branch
                String targetBranch = (args.get(0).length() > 0 ? args.get(0) : "master");

                // Status messages
                String gitStatus = "Updating to `" + targetBranch + "`...";
                String mvnStatus = "Waiting...";
                String updateStatus = "";

                // Send inital message
                IMessage statusMessage = CommandUtils.respond(msg, buildEmbed(gitStatus, mvnStatus, updateStatus));

                // Update process
                try {
                    String newHead = Updater.updateToBranch(Updater.getReadyGit(buildPath, ConfigManager.getProperties().getProperty("git_botRepo"), true), targetBranch, true);
                    if (newHead != null) {
                        gitStatus = "Updated to [`" + newHead.substring(0, 7) + "`](https://github.com/davue/Stammbot/commit/" + newHead + ") on `" + targetBranch + "`.";
                        mvnStatus = "Building...";

                        CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));

                        if (Updater.mavenInvoke(buildPath, Collections.singletonList("install"))) {
                            mvnStatus = "Installing...";

                            CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));

                            if (Updater.installBuild(buildPath)) {
                                String buildLog = Updater.getLastBuildLog();
                                if (buildLog != null) {
                                    try {
                                        mvnStatus = "[Success!](" + PastebinAPI.createPaste("Build Log", buildLog, "10M", true) + ")";
                                    } catch (APIException e) {
                                        ErrorHandler.handleError(e);
                                        mvnStatus = "Success!";
                                        System.out.println(buildLog);
                                    }
                                } else {
                                    mvnStatus = "Success!";
                                }

                                updateStatus = "Restart the bot to apply the update.";

                                CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));
                            } else {
                                mvnStatus = "Installation failed.";
                                updateStatus = "Update canceled.";

                                CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));
                            }
                        } else {
                            mvnStatus = "Build failed.";
                            updateStatus = "Update canceled.";

                            CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));
                        }
                    } else {
                        gitStatus = "Could not pull changes.";
                        mvnStatus = "Canceled.";
                        updateStatus = "Update canceled.";

                        CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));
                    }
                } catch (RefNotFoundException e) {
                    gitStatus = "There is no branch `" + targetBranch + "` on remote.";
                    mvnStatus = "-";
                    updateStatus = "Update canceled.";

                    CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));
                } catch (GitAPIException e) {
                    ErrorHandler.handleError(e);

                    gitStatus = "An error occurred while updating.";
                    mvnStatus = "-";
                    updateStatus = "Update canceled.";

                    CommandUtils.editMessage(statusMessage, buildEmbed(gitStatus, mvnStatus, updateStatus));
                }

                isUpdating = false;
            } else {
                CommandUtils.respond(msg, "**I'm currently updating, please wait for the update to finish.**");
            }
        }
    }
}
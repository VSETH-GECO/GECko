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

import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.rest.api.PastebinAPI;
import ch.ethz.geco.gecko.rest.api.exception.APIException;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.FS;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class Update extends Command {
    public Update() {
        this.setName("update");
        this.setParams("[branch]");
        this.setDescription("Fetches the latest changes and compiles them. It will try to update to the given branch or to master if no branch is given.");
        this.setRemoveAfterCall(true);
    }

    /**
     * URL of the remote git repository
     */
    private static final String REMOTE_URL = "https://github.com/VSETH-GECO/GECkO.git";
    /**
     * Path to the local git repository
     */
    private static final String LOCAL_PATH = "build/";
    /**
     * Path to the binary folder containing needed binaries like maven
     */
    private static final String BIN_PATH = "bin/";
    /**
     * The backup path
     */
    private static final String BACKUP_PATH = "backup/";

    /**
     * The update message
     */
    private static volatile IMessage statusMessage;
    private static String gitStatus;
    private static String mavenStatus;
    private static String updateStatus;

    @Override
    public void execute(IMessage msg, List<String> args) {
        // Don't update if there is already another update running
        if (statusMessage == null) {
            gitStatus = "Pulling and Updating...";
            mavenStatus = "Waiting...";
            updateStatus = "";
            flushStatusMessage(msg.getChannel());

            // Check if directories exists
            File repoDir = new File(LOCAL_PATH);
            if (!repoDir.isDirectory()) {
                repoDir.mkdir();
            }
            File backupDir = new File(BACKUP_PATH);
            if (!backupDir.isDirectory()) {
                backupDir.mkdir();
            }

            if (repoDir.isDirectory()) {
                // The local repository
                Repository repo;

                // Set target branch
                String targetBranch;
                if (args.size() > 0) {
                    targetBranch = args.get(0);
                } else {
                    targetBranch = "master";
                }

                // Try to open the local repository
                try {
                    repo = new FileRepositoryBuilder().setGitDir(new File(LOCAL_PATH + ".git/")).readEnvironment().findGitDir().build();
                } catch (IOException e) {
                    gitStatus = "Could not open local repository.";
                    mavenStatus = "-";
                    updateStatus = "Update canceled.";
                    flushStatusMessage(msg.getChannel());

                    e.printStackTrace();
                    statusMessage = null;
                    return;
                }

                // Create git object of the local repository
                Git git = new Git(repo);

                // Clone if non-existent or invalid
                if (!RepositoryCache.FileKey.isGitRepository(new File(LOCAL_PATH + ".git/"), FS.detect()) || !hasAtLeastOneReference(repo)) {
                    try {
                        Git.cloneRepository().setURI(REMOTE_URL).setDirectory(repoDir).call();
                    } catch (GitAPIException e) {
                        gitStatus = "Could not clone remote repository.";
                        mavenStatus = "-";
                        updateStatus = "Update canceled.";
                        flushStatusMessage(msg.getChannel());

                        e.printStackTrace();
                        statusMessage = null;
                        return;
                    }
                }

                // Fetch changes and branches
                try {
                    git.fetch().setRefSpecs(new RefSpec("refs/heads/" + targetBranch + ":refs/heads/" + targetBranch)).call();
                    git.pull().call();
                } catch (GitAPIException e) {
                    if (e instanceof TransportException) {
                        gitStatus = "There is no branch called <" + targetBranch + ">.";
                    } else {
                        gitStatus = "Could not fetch changes from remote repository.";
                        e.printStackTrace();
                    }
                    mavenStatus = "-";
                    updateStatus = "Update canceled.";
                    flushStatusMessage(msg.getChannel());

                    e.printStackTrace();
                    statusMessage = null;
                    return;
                }

                Ref checkoutResult;
                try {
                    checkoutResult = git.checkout().setName(targetBranch).call();
                } catch (GitAPIException e) {
                    gitStatus = "Could not switch branches.";
                    mavenStatus = "-";
                    updateStatus = "Update canceled.";
                    flushStatusMessage(msg.getChannel());

                    e.printStackTrace();
                    statusMessage = null;
                    return;
                }

                gitStatus = "Updated to ``" + checkoutResult.getObjectId().getName().substring(0, 7) + "`` on ``" + checkoutResult.getName() + "``";

                // Move current binary to backup folder
                try {
                    Files.move(Paths.get("GECkO.jar"), Paths.get(BACKUP_PATH + "GECkO.jar"));
                    mavenStatus = "Building...";
                    flushStatusMessage(msg.getChannel());
                } catch (IOException e) {
                    mavenStatus = "Failed to move binary.";
                    updateStatus = "Update canceled.";
                    flushStatusMessage(msg.getChannel());
                    return;
                }

                // The local repo should be up-to-date now, setting up maven
                InvocationRequest request = new DefaultInvocationRequest();
                request.setPomFile(new File("build/pom.xml"));
                request.setGoals(Collections.singletonList("install"));

                StringWriter writer = new StringWriter();
                InvocationOutputHandler outputHandler = s -> writer.write(s + "\n");
                request.setErrorHandler(outputHandler);
                request.setOutputHandler(outputHandler);


                // Build
                Invoker invoker = new DefaultInvoker().setMavenHome(new File(BIN_PATH + "maven/"));
                InvocationResult result;
                try {
                    result = invoker.execute(request);
                } catch (MavenInvocationException e) {
                    e.printStackTrace();
                    restoreBackup();
                    statusMessage = null;
                    return;
                }

                if (result != null && result.getExitCode() == 0) {
                    mavenStatus = "Success!";
                    updateStatus = "Restart the bot to apply update.";
                    flushStatusMessage(msg.getChannel());
                } else {
                    if (result == null) {
                        mavenStatus = "An internal maven error occurred.";
                    } else {
                        mavenStatus = "Build failed with error code: " + result.getExitCode();
                    }

                    flushStatusMessage(msg.getChannel());
                    restoreBackup();
                    statusMessage = null;
                    return;
                }

                // Post build log on pastebin
                String pastebinLink;
                try {
                    pastebinLink = PastebinAPI.createPaste("Build log", writer.toString(), "10M", true);

                    updateStatus = "**Build log:** ``" + pastebinLink + "``\n" + updateStatus;
                    flushStatusMessage(msg.getChannel());
                } catch (APIException e) {
                    GECkO.logger.error("[Update] A Pastebin API error occurred, writing output to stdout.");
                    System.out.println(writer.toString());
                }
            } else {
                gitStatus = "Could not create new dir for local git repository.";
                mavenStatus = "-";
                updateStatus = "Update canceled.";
                flushStatusMessage(msg.getChannel());

                GECkO.logger.error("[Update] Could not create new directory for local git repo.");
            }

            statusMessage = null;
        } else {
            CommandUtils.respond(msg, "There is currently another update process running.\nPlease wait for it to finish before starting another update process.");
        }
    }

    /**
     * Checks if a git repo has at least one reference. Can be used as a validity check.
     *
     * @param repo the repo to check
     * @return whether or not it has at least one valid reference
     */
    private static boolean hasAtLeastOneReference(Repository repo) {
        for (Ref ref : repo.getAllRefs().values()) {
            if (ref.getObjectId() == null)
                continue;
            return true;
        }
        return false;
    }

    /**
     * Updates or creates the update status message.
     *
     * @param channel the channel of the message
     */
    private static void flushStatusMessage(IChannel channel) {
        String pattern = "**Update Status:**\n- Git Status: " + gitStatus + "\n- Mvn Status: " + mavenStatus + "\n\n" + updateStatus;
        if (statusMessage == null) {
            statusMessage = CommandUtils.respond(channel, pattern);
        } else {
            CommandUtils.editMessage(statusMessage, pattern);
        }
    }

    /**
     * Restores the backup
     */
    private static void restoreBackup() {
        File backup = new File(BACKUP_PATH + "GECkO.jar");
        if (backup.isFile()) {
            try {
                Files.move(backup.toPath(), Paths.get("GECkO.jar"));
            } catch (IOException e) {
                GECkO.logger.error("[Update] Could not restore backup.");
                e.printStackTrace();
            }
        } else {
            GECkO.logger.error("[Update] There is no backup to restore.");
        }
    }
}

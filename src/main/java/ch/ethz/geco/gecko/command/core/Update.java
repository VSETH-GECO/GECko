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
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import sx.blah.discord.handle.obj.IMessage;

import java.io.File;
import java.io.IOException;
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
     * Set some default paths
     */
    private static final String REMOTE_URL = "https://github.com/VSETH-GECO/GECkO.git";
    private static final String LOCAL_PATH = "build/";
    private static final String BACKUP_PATH = "backup/";
    private static final String BIN_PATH = "bin/";

    /**
     * True if there is currently another build running
     */
    private static volatile boolean buildLock = false;

    @Override
    public void execute(IMessage msg, List<String> args) {
        // Don't update if there is already another update running
        if (!buildLock) {
            buildLock = true;

            String gitStatus = "Pulling and Updating...";
            String mavenStatus = "Waiting...";
            String updateStatus = "";
            IMessage updateMessage = CommandUtils.respond(msg, "**Update Status:**\n- Git Status: " + gitStatus + "\n- Mvn Status:" + mavenStatus + updateStatus);

            // Check if directory exists
            File repoDir = new File(LOCAL_PATH);
            if (!repoDir.isDirectory()) {
                repoDir.mkdir();
            }

            if (repoDir.isDirectory()) {
                // The local repository
                Repository repo;

                try {
                    repo = new FileRepositoryBuilder().setGitDir(new File(LOCAL_PATH + ".git/")).readEnvironment().findGitDir().build();
                } catch (IOException e) {
                    updateMessage(updateMessage, "Could not open local repository", "-", "\n\nUpdate canceled.");
                    GECkO.logger.error("[UPDATE] Could not open local repository.");
                    e.printStackTrace();
                    buildLock = false;
                    return;
                }

                if (RepositoryCache.FileKey.isGitRepository(new File(LOCAL_PATH + ".git/"), FS.detect()) && hasAtLeastOneReference(repo)) {
                    // Valid repo found, pull
                    Git git = new Git(repo);
                    try {
                        git.pull().call();
                    } catch (GitAPIException e) {
                        updateMessage(updateMessage, "Could not pull changes from remote repository", "-", "\n\nUpdate canceled.");
                        GECkO.logger.error("[UPDATE] Could not pull changes from remote repository.");
                        e.printStackTrace();
                        buildLock = false;
                        return;
                    }
                } else {
                    // Clone if non-existent or invalid
                    try {
                        Git.cloneRepository().setURI(REMOTE_URL).setDirectory(repoDir).call();
                    } catch (GitAPIException e) {
                        updateMessage(updateMessage, "Could not clone remote repository", "-", "\n\nUpdate canceled.");
                        GECkO.logger.error("[UPDATE] Could not clone remote repository.");
                        e.printStackTrace();
                        buildLock = false;
                        return;
                    }
                }

                // Switch branch
                Git git = new Git(repo);
                String targetBranch;
                if (args.size() > 0) {
                    targetBranch = args.get(0);
                } else {
                    targetBranch = "master";
                }

                Ref checkoutResult;
                try {
                    checkoutResult = git.checkout().setName(targetBranch).call();
                } catch (GitAPIException e) {
                    // If it was an invalid reference
                    if (e instanceof RefNotFoundException) {
                        updateMessage(updateMessage, "There is no branch called <" + targetBranch + ">", "-", "\n\nUpdate canceled.");
                    } else {
                        updateMessage(updateMessage, "Could not switch branch", "-", "\n\nUpdate canceled.");
                        e.printStackTrace();
                    }

                    GECkO.logger.error("[UPDATE] Could not switch branches.");
                    buildLock = false;
                    return;
                }

                gitStatus = "Updated to ``" + checkoutResult.getObjectId().getName().substring(0, 7) + "`` on ``" + checkoutResult.getName() + "``";
                updateMessage(updateMessage, gitStatus, "Building...", updateStatus);

                // The local repo should be up-to-date now, trying to build
                InvocationRequest request = new DefaultInvocationRequest();
                request.setPomFile(new File("build/pom.xml"));
                request.setGoals(Collections.singletonList("package"));

                Invoker invoker = new DefaultInvoker().setMavenHome(new File(BIN_PATH + "maven/"));
                InvocationResult result;
                try {
                    result = invoker.execute(request);
                } catch (MavenInvocationException e) {
                    GECkO.logger.error("[UPDATE] An error occurred while trying to build maven.");
                    e.printStackTrace();
                    buildLock = false;
                    return;
                }

                if (result != null && result.getExitCode() == 0) {
                    GECkO.logger.info("[UPDATE] Maven build successful!");
                    mavenStatus = "Success!";
                } else {
                    GECkO.logger.error("[UPDATE] Maven build failed!");
                    if (result == null) {
                        updateMessage(updateMessage, gitStatus, "An internal maven error occurred", updateStatus);
                    } else {
                        updateMessage(updateMessage, gitStatus, "Build failed with error code: " + result.getExitCode(), updateStatus);
                    }
                    buildLock = false;
                    return;
                }

                File backupDir = new File(BACKUP_PATH);
                File oldBackup = new File(BACKUP_PATH + "GECkO.jar");
                File oldBin = new File("GECkO.jar");
                File newBin = new File("build/target/GECkO-dev-SNAPSHOT.jar");
                try {
                    if (oldBackup.isFile()) {
                        oldBackup.delete();
                    }

                    FileUtils.moveFileToDirectory(oldBin, backupDir, true);

                    try {
                        FileUtils.moveFile(newBin, oldBin);
                        updateStatus = "\n\nUpdate successful, restart to apply.";
                    } catch (IOException e) {
                        // Restore backup on error
                        FileUtils.moveFile(oldBackup, oldBin);
                        updateStatus = "\n\nCould not replace old binary with new one, restoring backup.";
                    }
                } catch (IOException e) {
                    updateStatus = "\n\nAn error occurred while moving the binaries.";
                    GECkO.logger.error("[UPDATE] An error occurred while moving file.");
                    e.printStackTrace();
                }

                updateMessage(updateMessage, gitStatus, mavenStatus, updateStatus);
            } else {
                updateMessage(updateMessage, "Could not create new dir for local git repository", "-", "\n\nUpdate canceled.");
                GECkO.logger.error("[UPDATE] Could not create new directory for local git repo.");
            }

            buildLock = false;
        } else {
            CommandUtils.respond(msg, "There is currently another update process running.\nPlease wait for it to finish before starting another update process.");
        }
    }

    /**
     * Updates the status message.
     *
     * @param msg          the message to update
     * @param gitStatus    the new git status
     * @param mavenStatus  the new maven status
     * @param updateStatus the new update status
     * @return the updated message
     */
    private static IMessage updateMessage(IMessage msg, String gitStatus, String mavenStatus, String updateStatus) {
        return CommandUtils.editMessage(msg, "**Update Status:**\n- Git Status: " + gitStatus + "\n- Mvn Status: " + mavenStatus + updateStatus);
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
}

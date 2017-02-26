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

package ch.ethz.geco.gecko.command.core;

import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
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
import java.util.List;

public class Update2 extends Command {
    public Update2() {
        this.setName("gittest");
    }

    private static final String REMOTE_URL = "https://github.com/VSETH-GECO/GECkO.git";
    private static final String LOCAL_PATH = "build/";

    @Override
    public void execute(IMessage msg, List<String> args) {
        // Check if directory exists
        File repoDir = new File(LOCAL_PATH);
        if (!repoDir.isDirectory()) {
            repoDir.mkdir();
        }

        if (repoDir.isDirectory()) {
            // The local repository
            Repository repo;

            // Try to open the local repository
            try {
                repo = new FileRepositoryBuilder().setGitDir(new File(LOCAL_PATH + ".git/")).readEnvironment().findGitDir().build();
            } catch (IOException e) {
                CommandUtils.respond(msg, "**Error:** " + e.getMessage());
                e.printStackTrace();
                return;
            }

            if (RepositoryCache.FileKey.isGitRepository(new File(LOCAL_PATH + ".git/"), FS.detect()) && hasAtLeastOneReference(repo)) {
                // Valid repo found, pull
                Git git = new Git(repo);
                try {
                    PullResult result = git.pull().call();
                    CommandUtils.respond(msg, "**Fetch result:** " + result.getFetchResult() + "\n**Merge result:** " + result.getMergeResult());
                } catch (GitAPIException e) {
                    CommandUtils.respond(msg, "**Error:** " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } else {
                // Clone if non-existent or invalid
                try {
                    Git.cloneRepository().setURI(REMOTE_URL).setDirectory(repoDir).call();
                } catch (GitAPIException e) {
                    CommandUtils.respond(msg, "**Error:** " + e.getMessage());
                    e.printStackTrace();
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
                CommandUtils.respond(msg, "**Checkout result:** " + checkoutResult.getName());
            } catch (GitAPIException e) {
                // If it was an invalid reference
                if (e instanceof RefNotFoundException) {
                } else {
                    e.printStackTrace();
                }

                CommandUtils.respond(msg, "**Error:** " + e.getMessage());
                return;
            }
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
}

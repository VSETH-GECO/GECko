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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import sx.blah.discord.handle.obj.IMessage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Update extends Command {
    public Update() {
        this.setName("update");
        this.setParams("[branch]");
        this.setDescription("Fetches the latest changes and compiles them. It will try to update to the given branch or to master if no branch is given.");
    }

    private static final String REMOTE_URL = "https://github.com/VSETH-GECO/GECkO.git";
    private static final String LOCAL_PATH = "build/";

    @Override
    public void execute(IMessage msg, List<String> args) {
        // Check if directory exists
        boolean dirExists = false;
        File repoDir = new File(LOCAL_PATH);
        if (!repoDir.isDirectory()) {
            if (repoDir.mkdir()) {
                dirExists = true;
            }
        } else {
            dirExists = true;
        }

        if (dirExists) {
            if (RepositoryCache.FileKey.isGitRepository(repoDir, FS.DETECTED)) {
                // Repo found, check if it's valid
                try {
                    Repository repo = new FileRepositoryBuilder().setGitDir(repoDir).readEnvironment().findGitDir().build();
                    if (hasAtLeastOneReference(repo)) {
                        // Pull if valid
                        Git git = new Git(repo);
                        try {
                            git.pull().call();
                        } catch (GitAPIException e) {
                            GECkO.logger.error("[Update] Could not pull changes from remote repository.");
                            e.printStackTrace();
                        }
                    } else {
                        // Reclone if invalid
                        try {
                            Git.cloneRepository().setURI(REMOTE_URL).setDirectory(repoDir).call();
                        } catch (GitAPIException e) {
                            GECkO.logger.error("[Update] Could not clone remote repository.");
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    GECkO.logger.error("[Update] Could not open local repository.");
                    e.printStackTrace();
                }
            } else {
                // Clone if non-existent
                try {
                    Git.cloneRepository().setURI(REMOTE_URL).setDirectory(repoDir).call();
                } catch (GitAPIException e) {
                    GECkO.logger.error("[Update] Could not clone remote repository.");
                    e.printStackTrace();
                }
            }

            // Switch branch
            try {
                Repository repo = new FileRepositoryBuilder().setGitDir(repoDir).readEnvironment().findGitDir().build();
                Git git = new Git(repo);
                if (args.size() > 0) {
                    git.checkout().setName(args.get(0));
                } else {
                    git.checkout().setName("master");
                }
            } catch (IOException e) {
                GECkO.logger.error("[Update] Could not switch branch.");
                e.printStackTrace();
            }

            // The local repo should be up-to-date now
            // TODO: Build process
        } else {
            GECkO.logger.error("[Update] Could not create new directory for local git repo.");
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

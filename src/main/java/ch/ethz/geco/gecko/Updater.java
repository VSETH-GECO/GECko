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

package ch.ethz.geco.gecko;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class providing all necessary functions to update the bot.
 */
public class Updater {
    /**
     * Contains the last build log of maven or null if the last build failed.
     */
    private static String lastBuildLog;

    /**
     * The SSH session factory.
     */
    private static final SshSessionFactory botSSHSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host host, Session session) {
            // Do nothing
        }

        @Override
        protected JSch createDefaultJSch(FS fs) throws JSchException {
            // Load authorization from config
            String knownHostsPath = ConfigManager.getProperties().getProperty("git_knownHostsFile");
            String privateKeyPath = ConfigManager.getProperties().getProperty("git_privateKeyFile");

            JSch defaultJSch = super.createDefaultJSch(fs);
            defaultJSch.removeAllIdentity();
            defaultJSch.addIdentity(privateKeyPath);
            defaultJSch.setKnownHosts(knownHostsPath);
            return defaultJSch;
        }
    };

    /**
     * The transport callback to tell JGit to use SSH
     */
    private static final TransportConfigCallback botSSHCallback = transport -> {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(botSSHSessionFactory);
    };

    /**
     * Checks and tries to create the given folder if it doesn't exist.
     *
     * @param path the folder to check
     * @return true if folder exist or has been successfully created, false otherwise
     */
    private static boolean checkFolder(String path) {
        // Check if directories exists
        File dir = new File(path);
        if (!dir.isDirectory()) {
            if (!dir.mkdir()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the given directory is a valid git repository.
     *
     * @param gitFolder the .git folder of the repository
     * @return whether or not the given folder is a valid git repository
     */
    private static boolean isValidGitRepository(File gitFolder) {
        if (RepositoryCache.FileKey.isGitRepository(gitFolder, FS.DETECTED)) {
            Repository repo = null;
            try {
                repo = new FileRepositoryBuilder().setGitDir(gitFolder).readEnvironment().findGitDir().build();

                for (Ref ref : repo.getAllRefs().values()) {
                    if (ref.getObjectId() == null)
                        continue;
                    return true;
                }

                return false;
            } catch (IOException e) {
                ErrorHandler.handleError(e);
                return false;
            } finally {
                if (repo != null) {
                    repo.close();
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Tries to return a working git instance based on the repository and authorization configured.
     *
     * @param localRepo  the folder of the local repository
     * @param remoteRepo the path of the remote repository
     * @param isBot      if it's the bot repository
     * @return a working git instance or null if something went wrong
     */
    public static Git getReadyGit(String localRepo, String remoteRepo, boolean isBot) {
        File repoFolder = new File(localRepo);
        File gitFolder = new File(repoFolder.getPath() + "/.git/");
        Git git = null;
        if (checkFolder(localRepo)) {
            try {
                // If there is no valid git repository
                if (!isValidGitRepository(gitFolder)) {
                    // Clear existing folder
                    if (!repoFolder.delete()) {
                        FileUtils.cleanDirectory(repoFolder);
                    } else {
                        if (!repoFolder.mkdir()) {
                            throw new IOException("Repository folder could not be recreated.");
                        }
                    }

                    // Clone repository
                    if (isBot) {
                        git = Git.cloneRepository().setURI(remoteRepo).setDirectory(repoFolder).setTransportConfigCallback(botSSHCallback).call();
                    } else {
                        git = Git.cloneRepository().setURI(remoteRepo).setDirectory(repoFolder).call();
                    }
                } else {
                    git = new Git(new FileRepositoryBuilder().setGitDir(gitFolder).readEnvironment().findGitDir().build());
                }
            } catch (IOException | GitAPIException e) {
                ErrorHandler.handleError(e);
            }
        } else {
            ErrorHandler.handleError(new IOException("Could not create essential updater folders."));
        }

        return git;
    }

    /**
     * Returns a collection of available remote refs.
     *
     * @param git   the git object of the repository
     * @param isBot if it's the bot repository
     * @return a collection of available remote refs
     */
    public static Collection<Ref> getRemoteRefs(Git git, boolean isBot) {
        Collection<Ref> refList = null;
        if (git != null) {
            try {
                if (isBot) {
                    refList = git.lsRemote().setTransportConfigCallback(botSSHCallback).call().stream()
                            .filter(ref -> ref.getName().startsWith("refs/heads/")).collect(Collectors.toCollection(HashSet::new));
                } else {
                    refList = git.lsRemote().call().stream()
                            .filter(ref -> ref.getName().startsWith("refs/heads/")).collect(Collectors.toCollection(HashSet::new));
                }
            } catch (GitAPIException e) {
                ErrorHandler.handleError(e);
            }
        }

        return refList;
    }

    /**
     * Pulls changes and switches to the given branch.
     *
     * @param git    the git object of the repository
     * @param branch the branch to switch to
     * @param isBot  if it's the bot repository
     * @return the name of the new head commit
     * @throws GitAPIException if an git error occurred
     */
    public static String updateToBranch(Git git, String branch, boolean isBot) throws GitAPIException {
        String newHead = null;

        if (git != null) {
            // Check if requested branch exist on remote
            Collection<Ref> remoteRefs = getRemoteRefs(git, isBot);
            boolean branchExistsOnRemote = false;
            for (Ref ref : remoteRefs) {
                if (Objects.equals(ref.getName(), "refs/heads/" + branch)) {
                    branchExistsOnRemote = true;
                }
            }

            if (branchExistsOnRemote) {
                // Check if branch also exists locally. If not, create new branch from remote branch.
                Map<String, Ref> allRefs = git.getRepository().getAllRefs();
                if (allRefs.containsKey("refs/heads/" + branch)) {
                    try {
                        git.checkout().setName(branch).setStartPoint("origin/" + branch).setCreateBranch(false).call();
                    } catch (CheckoutConflictException e2) {
                        // If there is a conflict because someone modified the files, discard all changes.
                        git.reset().setMode(ResetCommand.ResetType.HARD).call();
                        git.checkout().setName(branch).setStartPoint("origin/" + branch).setCreateBranch(false).call();
                    }
                } else {
                    git.checkout().setName(branch).setStartPoint("origin/" + branch).setCreateBranch(true).call();
                }

                if (isBot) {
                    newHead = git.pull().setRemoteBranchName(branch).setTransportConfigCallback(botSSHCallback).call().getMergeResult().getNewHead().getName();
                } else {
                    newHead = git.pull().setRemoteBranchName(branch).call().getMergeResult().getNewHead().getName();
                }
            } else {
                throw new RefNotFoundException("There is no remote branch called: " + branch);
            }

            git.close();
        }

        return newHead;
    }

    /**
     * Invokes the given maven goals on the configured project.
     *
     * @param path  path of the folder containing the pom file
     * @param goals the goals to run
     * @return if the invocation was successful (all goals were reached)
     */
    public static boolean mavenInvoke(String path, List<String> goals) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(path + "/pom.xml"));
        request.setGoals(goals);

        StringWriter writer = new StringWriter();
        InvocationOutputHandler outputHandler = s -> writer.write(s + "\n");
        request.setErrorHandler(outputHandler);
        request.setOutputHandler(outputHandler);

        Invoker invoker = new DefaultInvoker().setMavenHome(new File("maven"));
        InvocationResult result = null;
        try {
            result = invoker.execute(request);
        } catch (Exception e) {
            ErrorHandler.handleError(e);
        }

        // Set the last build log
        if (writer.toString().isEmpty() || writer.toString() == null) {
            lastBuildLog = null;
        } else {
            lastBuildLog = writer.toString();
        }

        if (result != null && result.getExitCode() == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Installs the latest build. It makes a backup of the old artifact, moves the new artifact at it's place and keeps the libs up-to-date.
     *
     * @param path path of the folder containing the pom file
     * @return if the installation process was successful
     */
    public static boolean installBuild(String path) {
        // Define artifact names
        String artifactName = "Chatsounds.jar";
        String buildArtifactName = "chatsounds-dev-SNAPSHOT.jar";

        // Define installation paths
        Path curArtifactPath = Paths.get(artifactName);
        Path newArtifactPath = Paths.get(path + "/target/" + buildArtifactName);
        Path backupPath = Paths.get(artifactName + ".bak");

        Path curLibPath = Paths.get("lib");
        Path newLibPath = Paths.get(path + "/target/lib");

        try {
            // Replace backup with current artifact
            Files.move(curArtifactPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            // Move build artifact to the place of the current artifact
            Files.move(newArtifactPath, curArtifactPath, StandardCopyOption.REPLACE_EXISTING);

            // Move libs
            // TODO: check if deletion of libs on runtime threatens bot stability
            FileUtils.cleanDirectory(curLibPath.toFile());
            Files.move(newLibPath, curLibPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            ErrorHandler.handleError(e);
            return false;
        }

        return true;
    }

    /**
     * Returns the last build log or null if the last build failed without generating output.
     *
     * @return the last build log or null if the last build failed without generating output
     */
    public static String getLastBuildLog() {
        return lastBuildLog;
    }
}

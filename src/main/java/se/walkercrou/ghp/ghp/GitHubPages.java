package se.walkercrou.ghp.ghp;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import se.walkercrou.ghp.ghp.jgit.CreateOrphanBranchCommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Interacts with the "GitHub Pages" feature on GitHub.
 */
public class GitHubPages {
    /**
     * The default branch to push content to, 'gh-pages' is the only branch that will update your "GitHub page" however
     * leaving this configurable leaves room to re-purpose this library.
     */
    public static final String DEFAULT_BRANCH = "gh-pages";
    /**
     * The default directory to copy to repository.
     */
    public static final String APIDOCS = "target/apidocs";

    private Git git;
    private final File workingDir, contentDir;
    private final String uri, branch;
    private final Log log;

    /**
     * Creates a new GitHubPages object and clones the specified repository.
     *
     * @param mojo plugin execution
     * @param workingDir to clone repo to
     * @param contentDir directory within workingDir that content should be copied to
     * @param uri        to clone repo from
     * @param branch     of repo to clone
     * @throws MojoExecutionException if the workingDir cannot be created or the repo cannot be cloned
     * @throws org.apache.maven.plugin.MojoFailureException if content directory is not a subdirectory of working
     *         directory
     */
    public GitHubPages(Mojo mojo, File workingDir, File contentDir, String uri, String branch)
            throws MojoExecutionException, MojoFailureException {
        log = mojo.getLog();
        this.workingDir = workingDir;
        if (!isSubDirectoryOf(contentDir, workingDir))
            throw new MojoFailureException("Content directory must be a subdirectory of the working directory");
        this.contentDir = contentDir;
        this.uri = uri;
        this.branch = branch;
        setupWorkingDirectory();
        cloneRepo();
        if (!checkoutBranch()) {
            // branch not found, create new one
            createBranch();
        }
    }

    /**
     * Creates a new GitHubPages object and clones the specified repository. The constructor uses
     * {@link #DEFAULT_BRANCH} as the branch parameter.
     *
     * @param mojo plugin execution
     * @param workingDir to clone repo to
     * @param contentDir directory within workingDir that content should be copied to
     * @param uri        to clone repo from
     * @throws MojoExecutionException if the workingDir cannot be created or the repo cannot be cloned
     * @throws org.apache.maven.plugin.MojoFailureException if content directory is not a subdirectory of working
     *         directory
     */
    public GitHubPages(Mojo mojo, File workingDir, File contentDir, String uri) throws MojoExecutionException,
            MojoFailureException {
        this(mojo, workingDir, contentDir, uri, DEFAULT_BRANCH);
    }

    /**
     * Creates a new GitHubPages object and clones the specified repository. The constructor uses
     * {@link #DEFAULT_BRANCH} as the branch parameter.
     *
     * @param mojo plugin execution
     * @param workingDir to clone repo to
     * @param uri to clone repo from
     * @throws MojoExecutionException if the workingDir cannot be created or the repo cannot be cloned
     * @throws org.apache.maven.plugin.MojoFailureException if content directory is not a subdirectory of working
     *         directory
     */
    public GitHubPages(Mojo mojo, File workingDir, String uri) throws MojoExecutionException, MojoFailureException {
        this(mojo, workingDir, workingDir, uri);
    }

    private boolean isSubDirectoryOf(File dir, File parentDir) {
        return dir.getAbsolutePath().startsWith(parentDir.getAbsolutePath());
    }

    private void setupWorkingDirectory() throws MojoExecutionException {
        if (!workingDir.exists() && !workingDir.mkdir())
            throw new MojoExecutionException("Could not create working directory");
    }

    private void cloneRepo() throws MojoExecutionException {
        try {
            log.info("Cloning remote repository at : \"" + uri + "\"");
            git = new CloneCommand()
                    .setURI(uri)
                    .setDirectory(workingDir)
                    .setNoCheckout(true)
                    .call();
        } catch (GitAPIException e) {
            throw new MojoExecutionException("Could not clone git repository", e);
        }
    }

    private boolean checkoutBranch() throws MojoExecutionException {
        try {
            log.info("Checking out remote branch : \"" + branch + "\"");
            git.checkout()
                    .setForce(true)
                    .setName(branch)
                    .setStartPoint("origin/" + branch)
                    .setCreateBranch(true)
                    .call();
            return true;
        } catch (RefNotFoundException e) {
            log.warn("Remote branch \"" + branch + "\" not found");
            return false;
        } catch (GitAPIException e) {
            throw new MojoExecutionException("Could not checkout branch : \"" + branch + "\"");
        }
    }

    private void createBranch() throws MojoExecutionException {
        try {
            log.info("Creating new orphan branch : \"" + branch + "\"");
            new CreateOrphanBranchCommand(git.getRepository()).setName(branch).call();
        } catch (GitAPIException e) {
            throw new MojoExecutionException("Could not create new orphan branch \"" + branch + "\"", e);
        }
    }

    private void clearDirectory(File dir) throws MojoExecutionException {
        RmCommand rm = git.rm();
        if (forEachPath(dir, rm::addFilepattern)) {
            try {
                rm.call();
            } catch (GitAPIException e) {
                throw new MojoExecutionException("Could not remove files from directory and git", e);
            }
        }
    }

    /**
     * Removes all files from the git repository and the working directory.
     *
     * @throws MojoExecutionException if the files cannot be removed from git or the directory
     */
    public void resetContent() throws MojoExecutionException {
        log.info("Clearing content directory : \"" + contentDir + "\"");
        if (!contentDir.exists() && !contentDir.mkdirs())
            throw new MojoExecutionException("Could not create content directory : \"" + contentDir + "\"");
        clearDirectory(contentDir);
    }

    /**
     * Copies the contents from the specified directory to the working directory.
     *
     * @param dir to copy content from
     * @throws MojoExecutionException if the content directory is not found or the files cannot be copied
     */
    public void addContent(File dir) throws MojoExecutionException {
        if (!dir.exists())
            throw new MojoExecutionException("No resources found at : " + dir.getAbsolutePath());
        log.info("Copying content from \"" + dir.getPath() + "\" to \"" + contentDir.getPath() + "\"");
        try {
            FileUtils.copyDirectoryStructure(dir, contentDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not copy resources to repository");
        }
    }

    /**
     * Copies the contents from {@link #APIDOCS} to the working directory.
     *
     * @throws MojoExecutionException if the content directory is not found or the files cannot be copied
     */
    public void addContent() throws MojoExecutionException {
        addContent(new File(APIDOCS));
    }

    /**
     * Adds all of the files in the directory to the git repository, commits the changes, and pushes to the remote.
     *
     * @param commitMessage to use in commit
     * @throws MojoExecutionException if the files cannot be added, commited, or pushed
     */
    public void update(String commitMessage) throws MojoExecutionException {
        log.info("Adding directory contents to git");
        AddCommand add = git.add();
        if (forEachPath(workingDir, add::addFilepattern)) {
            try {
                add.call();
                git.commit().setMessage(commitMessage).call();
                log.info("Pushing changes to remote branch \"" + branch + "\" at : \"" + uri + "\"");
                git.push().call();
            } catch (GitAPIException e) {
                throw new MojoExecutionException("Could not commit and push changes", e);
            }
        } else {
            log.warn("Nothing to add");
        }
    }

    private boolean forEachPath(File dir, Consumer<String> c) throws MojoExecutionException {
        boolean atLeastOne = false;
        try {
            for (Path p : Files.newDirectoryStream(dir.toPath())) {
                atLeastOne = true;
                c.accept(p.getFileName().toString());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception thrown while walking directory tree", e);
        }
        return atLeastOne;
    }
}

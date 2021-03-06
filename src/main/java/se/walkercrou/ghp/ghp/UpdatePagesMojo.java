package se.walkercrou.ghp.ghp;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Uses {@link GitHubPages} to update the specified repository from the specified content directory.
 */
@Mojo(name = "update")
public class UpdatePagesMojo extends AbstractMojo {
    /**
     * The directory where the repository will be cloned to.
     */
    public static final String WORKING_DIR = "target/ghp-plugin";
    /**
     * Format string for commit message if left unset.
     */
    public static final String DEFAULT_COMMIT_MESSAGE = "Automatic commit message generated by " +
            "the GitHub Pages Maven Plugin\n%s";
    /**
     * The name of properties file for this plugin.
     */
    public static final String PROPERTIES_FILE_NAME = ".properties";

    @Parameter(defaultValue = "${project.scm.url}", required = true, readonly = true)
    private String uri;
    @Parameter(property = "push.branch", defaultValue = GitHubPages.DEFAULT_BRANCH)
    private String branch;
    @Parameter(property = "push.commitMessage")
    private String commitMessage;
    @Parameter(property = "push.contentDir", defaultValue = GitHubPages.APIDOCS)
    private String contentDir;
    @Parameter(property = "push.contentDestination")
    private String contentDestination;
    private Log log;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        log = getLog();
        GitHubPages ghPages = new GitHubPages(this,
                new File(WORKING_DIR),
                new File(WORKING_DIR + File.separator + contentDestination),
                uri, branch);
        ghPages.resetContent();
        ghPages.addContent(new File(contentDir));
        String commitMessage = getCommitMessage();
        log.debug("Using commit message : \"" + commitMessage + "\"");
        ghPages.update(getCommitMessage());
    }

    private String getCommitMessage() {
        if (commitMessage == null || commitMessage.isEmpty()) {
            String url = getPluginUrl();
            log.debug("Plugin URL : \"" + url + "\"");
            return String.format(DEFAULT_COMMIT_MESSAGE, url == null ? "" : url);
        }
        return commitMessage;
    }

    private String getPluginUrl() {
        Properties properties = new Properties();
        try {
            InputStream in = UpdatePagesMojo.class.getResourceAsStream("/" + PROPERTIES_FILE_NAME);
            if (in == null)
                throw new IOException("Could not get InputStream for properties file");
            properties.load(in);
        } catch (IOException e) {
            getLog().warn("Could not load internal plugin properties", e);
        }
        return properties.getProperty("url");
    }
}

package dev.bootupgrade.core.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * JGit-backed git operations for a target repository being upgraded: create
 * the upgrade branch, commit the result of each step, and report what
 * changed. No shell {@code git} dependency — everything goes through JGit so
 * the pipeline works wherever the JVM runs.
 */
public final class GitOperations {

    private static final String COMMIT_AUTHOR_NAME = "bootupgrade";
    private static final String COMMIT_AUTHOR_EMAIL = "bootupgrade@localhost";

    /** Initializes {@code repoPath} as a git repository with an initial commit, unless it already is one. */
    public void ensureInitialized(Path repoPath) {
        Objects.requireNonNull(repoPath, "repoPath must not be null");
        if (repoPath.resolve(".git").toFile().exists()) {
            return;
        }
        try (Git git = Git.init().setDirectory(repoPath.toFile()).call()) {
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("Initial commit")
                    .setAuthor(COMMIT_AUTHOR_NAME, COMMIT_AUTHOR_EMAIL)
                    .setAllowEmpty(true)
                    .call();
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to initialize git repository at " + repoPath, e);
        }
    }

    /** Creates and checks out a new branch named {@code branchName} from the current HEAD. */
    public void createBranch(Path repoPath, String branchName) {
        Objects.requireNonNull(repoPath, "repoPath must not be null");
        Objects.requireNonNull(branchName, "branchName must not be null");
        try (Git git = Git.open(repoPath.toFile())) {
            git.checkout().setCreateBranch(true).setName(branchName).call();
        } catch (IOException | GitAPIException e) {
            throw new GitOperationException("Failed to create branch " + branchName + " at " + repoPath, e);
        }
    }

    /** Paths with uncommitted changes (modified, added, removed, or untracked), sorted for determinism. */
    public List<String> uncommittedChanges(Path repoPath) {
        Objects.requireNonNull(repoPath, "repoPath must not be null");
        try (Git git = Git.open(repoPath.toFile())) {
            Status status = git.status().call();
            TreeSet<String> changed = new TreeSet<>();
            changed.addAll(status.getModified());
            changed.addAll(status.getAdded());
            changed.addAll(status.getChanged());
            changed.addAll(status.getRemoved());
            changed.addAll(status.getMissing());
            changed.addAll(status.getUntracked());
            return List.copyOf(changed);
        } catch (IOException | GitAPIException e) {
            throw new GitOperationException("Failed to read status at " + repoPath, e);
        }
    }

    /**
     * Stages and commits every change in the working tree. Returns the new commit's SHA, or empty if
     * there was nothing to commit.
     */
    public Optional<String> commitAll(Path repoPath, String message) {
        Objects.requireNonNull(repoPath, "repoPath must not be null");
        Objects.requireNonNull(message, "message must not be null");
        try (Git git = Git.open(repoPath.toFile())) {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            if (git.status().call().isClean()) {
                return Optional.empty();
            }
            RevCommit commit = git.commit()
                    .setMessage(message)
                    .setAuthor(COMMIT_AUTHOR_NAME, COMMIT_AUTHOR_EMAIL)
                    .call();
            return Optional.of(commit.getName());
        } catch (IOException | GitAPIException e) {
            throw new GitOperationException("Failed to commit at " + repoPath, e);
        }
    }
}

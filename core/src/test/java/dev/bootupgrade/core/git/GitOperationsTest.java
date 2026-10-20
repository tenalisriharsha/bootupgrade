package dev.bootupgrade.core.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GitOperationsTest {

    private final GitOperations git = new GitOperations();

    @Test
    void initializesAFreshRepositoryWithAnInitialCommit(@TempDir Path repoPath) throws IOException {
        Files.writeString(repoPath.resolve("README.md"), "hello");

        git.ensureInitialized(repoPath);

        assertThat(repoPath.resolve(".git")).isDirectory();
        assertThat(git.uncommittedChanges(repoPath)).isEmpty();
    }

    @Test
    void ensureInitializedIsIdempotent(@TempDir Path repoPath) {
        git.ensureInitialized(repoPath);
        git.ensureInitialized(repoPath);

        assertThat(repoPath.resolve(".git")).isDirectory();
    }

    @Test
    void createsAndSwitchesToANewBranch(@TempDir Path repoPath) {
        git.ensureInitialized(repoPath);

        git.createBranch(repoPath, "bootupgrade/test-branch");

        try (var jgit = org.eclipse.jgit.api.Git.open(repoPath.toFile())) {
            assertThat(jgit.getRepository().getBranch()).isEqualTo("bootupgrade/test-branch");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void detectsUncommittedChanges(@TempDir Path repoPath) throws IOException {
        git.ensureInitialized(repoPath);

        Files.writeString(repoPath.resolve("NewFile.txt"), "content");

        assertThat(git.uncommittedChanges(repoPath)).containsExactly("NewFile.txt");
    }

    @Test
    void commitAllStagesAndCommitsEveryChange(@TempDir Path repoPath) throws IOException {
        git.ensureInitialized(repoPath);
        Files.writeString(repoPath.resolve("Changed.txt"), "content");

        Optional<String> sha = git.commitAll(repoPath, "add Changed.txt");

        assertThat(sha).isPresent();
        assertThat(git.uncommittedChanges(repoPath)).isEmpty();
    }

    @Test
    void commitAllReturnsEmptyWhenThereIsNothingToCommit(@TempDir Path repoPath) {
        git.ensureInitialized(repoPath);

        Optional<String> sha = git.commitAll(repoPath, "no-op");

        assertThat(sha).isEmpty();
    }

    @Test
    void neverCommitsMavenBuildOutputEvenWithoutAGitignore(@TempDir Path repoPath) throws IOException {
        Files.createDirectories(repoPath.resolve("src"));
        Files.writeString(repoPath.resolve("src/A.java"), "class A {}");
        GitOperations git = new GitOperations();
        git.ensureInitialized(repoPath);
        git.createBranch(repoPath, "upgrade");

        Files.writeString(repoPath.resolve("src/A.java"), "class A { int x; }");
        Files.createDirectories(repoPath.resolve("target/classes"));
        Files.writeString(repoPath.resolve("target/classes/A.class"), "bytes");
        assertThat(git.uncommittedChanges(repoPath)).containsExactly("src/A.java");

        git.commitAll(repoPath, "change");

        try (org.eclipse.jgit.api.Git jgit = org.eclipse.jgit.api.Git.open(repoPath.toFile())) {
            org.eclipse.jgit.revwalk.RevCommit head = jgit.log().setMaxCount(1).call().iterator().next();
            try (org.eclipse.jgit.treewalk.TreeWalk walk = new org.eclipse.jgit.treewalk.TreeWalk(jgit.getRepository())) {
                walk.addTree(head.getTree());
                walk.setRecursive(true);
                java.util.List<String> paths = new java.util.ArrayList<>();
                while (walk.next()) {
                    paths.add(walk.getPathString());
                }
                assertThat(paths).containsExactly("src/A.java");
            }
        } catch (org.eclipse.jgit.api.errors.GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }
}

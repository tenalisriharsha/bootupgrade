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
}

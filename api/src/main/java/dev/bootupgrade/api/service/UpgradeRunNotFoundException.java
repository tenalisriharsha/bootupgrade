package dev.bootupgrade.api.service;

/** Thrown when a run id is looked up but does not exist. */
public class UpgradeRunNotFoundException extends RuntimeException {

    public UpgradeRunNotFoundException(String runId) {
        super("Upgrade run not found: " + runId);
    }
}

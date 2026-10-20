export type RunStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCEEDED' | 'FAILED' | 'PARTIAL';
export type StepStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED';
export type StepKind = 'RECIPE' | 'BUILD_CHECK' | 'AGENT_FIX';

export interface UpgradeStep {
  id: string;
  name: string;
  kind: StepKind;
  status: StepStatus;
  description: string;
  diffSummary: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export interface UpgradeRunSummary {
  id: string;
  repositoryName: string;
  sourceJavaVersion: string;
  targetJavaVersion: string;
  sourceSpringBootVersion: string;
  targetSpringBootVersion: string;
  status: RunStatus;
  createdAt: string;
  totalSteps: number;
  succeededStepCount: number;
  failedStepCount: number;
}

export interface UpgradeRunDetail {
  id: string;
  repositoryName: string;
  sourceJavaVersion: string;
  targetJavaVersion: string;
  sourceSpringBootVersion: string;
  targetSpringBootVersion: string;
  branchName: string;
  status: RunStatus;
  createdAt: string;
  steps: UpgradeStep[];
}

export interface RepositorySuccessRate {
  repositoryName: string;
  totalRuns: number;
  succeededRuns: number;
  partialRuns: number;
  failedRuns: number;
  successRate: number;
}

export interface SuccessRateSummary {
  totalRuns: number;
  succeededRuns: number;
  partialRuns: number;
  failedRuns: number;
  successRate: number;
  byRepository: RepositorySuccessRate[];
}

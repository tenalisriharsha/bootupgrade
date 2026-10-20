import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RepositorySuccessRate, SuccessRateSummary } from '../models/upgrade-run.model';

interface RepoBarSegment {
  widthPct: number;
  cssVar: string;
  label: string;
}

@Component({
  selector: 'app-success-rate',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './success-rate.component.html',
  styleUrl: './success-rate.component.css'
})
export class SuccessRateComponent {
  @Input({ required: true }) summary!: SuccessRateSummary;

  get overallPct(): string {
    return (this.summary.successRate * 100).toFixed(0);
  }

  segmentsFor(repo: RepositorySuccessRate): RepoBarSegment[] {
    if (repo.totalRuns === 0) {
      return [];
    }
    const other = repo.totalRuns - repo.succeededRuns - repo.partialRuns - repo.failedRuns;
    const segments: RepoBarSegment[] = [
      { widthPct: pct(repo.succeededRuns, repo.totalRuns), cssVar: '--status-good', label: 'Succeeded' },
      { widthPct: pct(repo.partialRuns, repo.totalRuns), cssVar: '--status-warning', label: 'Partial' },
      { widthPct: pct(repo.failedRuns, repo.totalRuns), cssVar: '--status-critical', label: 'Failed' }
    ];
    if (other > 0) {
      segments.push({ widthPct: pct(other, repo.totalRuns), cssVar: '--text-muted', label: 'Other' });
    }
    return segments.filter((s) => s.widthPct > 0);
  }
}

function pct(count: number, total: number): number {
  return total === 0 ? 0 : (count / total) * 100;
}

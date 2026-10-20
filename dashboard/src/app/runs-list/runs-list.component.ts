import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { SuccessRateComponent } from '../success-rate/success-rate.component';
import { SuccessRateSummary, UpgradeRunSummary } from '../models/upgrade-run.model';
import { UpgradeRunService } from '../services/upgrade-run.service';

@Component({
  selector: 'app-runs-list',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, SuccessRateComponent],
  templateUrl: './runs-list.component.html',
  styleUrl: './runs-list.component.css'
})
export class RunsListComponent implements OnInit {
  runs: UpgradeRunSummary[] = [];
  successRate: SuccessRateSummary | null = null;
  loading = true;
  errorMessage: string | null = null;

  constructor(private readonly upgradeRunService: UpgradeRunService) {}

  ngOnInit(): void {
    this.upgradeRunService
      .listRuns()
      .pipe(
        catchError(() => {
          this.errorMessage = 'Could not reach the bootupgrade API. Is it running on :8080?';
          return of<UpgradeRunSummary[]>([]);
        })
      )
      .subscribe((runs) => {
        this.runs = runs;
        this.loading = false;
      });

    this.upgradeRunService
      .successRate()
      .pipe(catchError(() => of<SuccessRateSummary | null>(null)))
      .subscribe((summary) => {
        this.successRate = summary;
      });
  }
}

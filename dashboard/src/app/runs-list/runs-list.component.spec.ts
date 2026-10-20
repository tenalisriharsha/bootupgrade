import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RunsListComponent } from './runs-list.component';
import { UpgradeRunService } from '../services/upgrade-run.service';
import { SuccessRateSummary, UpgradeRunSummary } from '../models/upgrade-run.model';

describe('RunsListComponent', () => {
  function configure(service: Partial<UpgradeRunService>) {
    TestBed.configureTestingModule({
      imports: [RunsListComponent],
      providers: [provideRouter([]), { provide: UpgradeRunService, useValue: service }]
    });
  }

  it('renders runs returned by the service', () => {
    const run: UpgradeRunSummary = {
      id: 'run-1',
      repositoryName: 'legacy-rest-service',
      sourceJavaVersion: '8',
      targetJavaVersion: '21',
      sourceSpringBootVersion: '2.7.18',
      targetSpringBootVersion: '3.3.5',
      status: 'SUCCEEDED',
      createdAt: new Date().toISOString(),
      totalSteps: 2,
      succeededStepCount: 2,
      failedStepCount: 0
    };
    const summary: SuccessRateSummary = {
      totalRuns: 1,
      succeededRuns: 1,
      partialRuns: 0,
      failedRuns: 0,
      successRate: 1,
      byRepository: []
    };
    configure({ listRuns: () => of([run]), successRate: () => of(summary) });

    const fixture = TestBed.createComponent(RunsListComponent);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('legacy-rest-service');
    expect(fixture.componentInstance.loading).toBeFalse();
  });

  const emptySummary: SuccessRateSummary = {
    totalRuns: 0,
    succeededRuns: 0,
    partialRuns: 0,
    failedRuns: 0,
    successRate: 0,
    byRepository: []
  };

  it('shows an error message when the API call fails', () => {
    configure({
      listRuns: () => throwError(() => new Error('network error')),
      successRate: () => of(emptySummary)
    });

    const fixture = TestBed.createComponent(RunsListComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.errorMessage).toContain('bootupgrade API');
  });

  it('shows an empty state when there are no runs', () => {
    configure({ listRuns: () => of([]), successRate: () => of(emptySummary) });

    const fixture = TestBed.createComponent(RunsListComponent);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No upgrade runs recorded yet');
  });
});

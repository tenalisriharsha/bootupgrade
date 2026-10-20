import { TestBed } from '@angular/core/testing';
import { SuccessRateComponent } from './success-rate.component';
import { SuccessRateSummary } from '../models/upgrade-run.model';

describe('SuccessRateComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [SuccessRateComponent] });
  });

  function makeFixture(summary: SuccessRateSummary) {
    const fixture = TestBed.createComponent(SuccessRateComponent);
    fixture.componentInstance.summary = summary;
    fixture.detectChanges();
    return fixture;
  }

  it('renders the overall percentage rounded to a whole number', () => {
    const fixture = makeFixture({
      totalRuns: 3,
      succeededRuns: 2,
      partialRuns: 1,
      failedRuns: 0,
      successRate: 2 / 3,
      byRepository: []
    });
    expect(fixture.componentInstance.overallPct).toBe('67');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('67%');
  });

  it('builds proportional bar segments per repository', () => {
    const fixture = makeFixture({
      totalRuns: 4,
      succeededRuns: 2,
      partialRuns: 1,
      failedRuns: 1,
      successRate: 0.5,
      byRepository: [
        { repositoryName: 'repo-a', totalRuns: 4, succeededRuns: 2, partialRuns: 1, failedRuns: 1, successRate: 0.5 }
      ]
    });
    const segments = fixture.componentInstance.segmentsFor(fixture.componentInstance.summary.byRepository[0]);
    expect(segments).toEqual([
      { widthPct: 50, cssVar: '--status-good', label: 'Succeeded' },
      { widthPct: 25, cssVar: '--status-warning', label: 'Partial' },
      { widthPct: 25, cssVar: '--status-critical', label: 'Failed' }
    ]);
  });

  it('shows an empty state when there are no runs', () => {
    const fixture = makeFixture({
      totalRuns: 0,
      succeededRuns: 0,
      partialRuns: 0,
      failedRuns: 0,
      successRate: 0,
      byRepository: []
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No runs recorded yet');
  });
});

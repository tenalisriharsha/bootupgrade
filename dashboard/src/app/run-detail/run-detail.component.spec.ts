import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RunDetailComponent } from './run-detail.component';
import { UpgradeRunService } from '../services/upgrade-run.service';
import { UpgradeRunDetail } from '../models/upgrade-run.model';

describe('RunDetailComponent', () => {
  function configure(service: Partial<UpgradeRunService>, id = 'run-1') {
    TestBed.configureTestingModule({
      imports: [RunDetailComponent],
      providers: [
        provideRouter([]),
        { provide: UpgradeRunService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ id })) }
        }
      ]
    });
  }

  it('renders a run and its steps', () => {
    const detail: UpgradeRunDetail = {
      id: 'run-1',
      repositoryName: 'legacy-security-service',
      sourceJavaVersion: '8',
      targetJavaVersion: '21',
      sourceSpringBootVersion: '2.7.18',
      targetSpringBootVersion: '3.3.5',
      branchName: 'bootupgrade/run-1',
      status: 'PARTIAL',
      createdAt: new Date().toISOString(),
      steps: [
        {
          id: 'step-1',
          name: 'Agent fix',
          kind: 'AGENT_FIX',
          status: 'SUCCEEDED',
          description: 'Completed in-memory user builder',
          diffSummary: '1 file changed',
          startedAt: new Date().toISOString(),
          finishedAt: new Date().toISOString()
        }
      ]
    };
    configure({ getRun: () => of(detail) });

    const fixture = TestBed.createComponent(RunDetailComponent);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('legacy-security-service');
    expect(el.textContent).toContain('Agent fix');
    expect(el.textContent).toContain('1 file changed');
  });

  it('shows a not-found message when the run does not exist', () => {
    configure({ getRun: () => throwError(() => new Error('404')) });

    const fixture = TestBed.createComponent(RunDetailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.notFound).toBeTrue();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Run not found');
  });
});

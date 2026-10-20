import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { UpgradeRunService } from './upgrade-run.service';

describe('UpgradeRunService', () => {
  let service: UpgradeRunService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(UpgradeRunService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lists runs from /runs', () => {
    service.listRuns().subscribe((runs) => {
      expect(runs.length).toBe(0);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/runs`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('fetches a single run by id', () => {
    const detail = { id: 'run-1', repositoryName: 'legacy-rest-service' };
    service.getRun('run-1').subscribe((run) => {
      expect(run).toEqual(detail as never);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/runs/run-1`);
    expect(req.request.method).toBe('GET');
    req.flush(detail);
  });

  it('fetches the success-rate summary', () => {
    const summary = { totalRuns: 0, succeededRuns: 0, partialRuns: 0, failedRuns: 0, successRate: 0, byRepository: [] };
    service.successRate().subscribe((result) => {
      expect(result).toEqual(summary as never);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/runs/stats/success-rate`);
    expect(req.request.method).toBe('GET');
    req.flush(summary);
  });
});

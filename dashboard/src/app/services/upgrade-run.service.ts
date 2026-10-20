import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SuccessRateSummary, UpgradeRunDetail, UpgradeRunSummary } from '../models/upgrade-run.model';

@Injectable({ providedIn: 'root' })
export class UpgradeRunService {
  private readonly baseUrl = `${environment.apiBaseUrl}/runs`;

  constructor(private readonly http: HttpClient) {}

  listRuns(): Observable<UpgradeRunSummary[]> {
    return this.http.get<UpgradeRunSummary[]>(this.baseUrl);
  }

  getRun(id: string): Observable<UpgradeRunDetail> {
    return this.http.get<UpgradeRunDetail>(`${this.baseUrl}/${id}`);
  }

  successRate(): Observable<SuccessRateSummary> {
    return this.http.get<SuccessRateSummary>(`${this.baseUrl}/stats/success-rate`);
  }
}

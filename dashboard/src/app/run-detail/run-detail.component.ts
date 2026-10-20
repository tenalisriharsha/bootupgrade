import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { UpgradeRunDetail } from '../models/upgrade-run.model';
import { UpgradeRunService } from '../services/upgrade-run.service';

@Component({
  selector: 'app-run-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent],
  templateUrl: './run-detail.component.html',
  styleUrl: './run-detail.component.css'
})
export class RunDetailComponent implements OnInit {
  run: UpgradeRunDetail | null = null;
  loading = true;
  notFound = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly upgradeRunService: UpgradeRunService
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const id = params.get('id')!;
          return this.upgradeRunService.getRun(id).pipe(
            catchError(() => {
              this.notFound = true;
              return of(null);
            })
          );
        })
      )
      .subscribe((run) => {
        this.run = run;
        this.loading = false;
      });
  }
}

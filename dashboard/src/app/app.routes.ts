import { Routes } from '@angular/router';
import { RunsListComponent } from './runs-list/runs-list.component';
import { RunDetailComponent } from './run-detail/run-detail.component';

export const routes: Routes = [
  { path: '', component: RunsListComponent },
  { path: 'runs/:id', component: RunDetailComponent },
  { path: '**', redirectTo: '' }
];

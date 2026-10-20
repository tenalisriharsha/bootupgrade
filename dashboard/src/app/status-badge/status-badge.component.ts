import { Component, Input } from '@angular/core';

interface StatusVisual {
  label: string;
  icon: string;
  cssVar: string;
}

const STATUS_VISUALS: Record<string, StatusVisual> = {
  SUCCEEDED: { label: 'Succeeded', icon: '✓', cssVar: '--status-good' },
  PARTIAL: { label: 'Partial', icon: '◐', cssVar: '--status-warning' },
  FAILED: { label: 'Failed', icon: '✕', cssVar: '--status-critical' },
  SKIPPED: { label: 'Skipped', icon: '—', cssVar: '--text-muted' },
  PENDING: { label: 'Pending', icon: '…', cssVar: '--text-muted' },
  IN_PROGRESS: { label: 'In progress', icon: '⟳', cssVar: '--series-blue' }
};

/**
 * Renders a status as an icon + label pill, never color alone, so the
 * status reads correctly for colorblind viewers and in grayscale.
 */
@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `
    <span class="badge" [style.color]="color" [style.border-color]="color">
      <span aria-hidden="true">{{ visual.icon }}</span>
      <span>{{ visual.label }}</span>
    </span>
  `,
  styles: [
    `
      .badge {
        display: inline-flex;
        align-items: center;
        gap: 0.35rem;
        padding: 0.15rem 0.6rem;
        border: 1px solid;
        border-radius: 999px;
        font-size: 0.8rem;
        font-weight: 600;
        white-space: nowrap;
      }
    `
  ]
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;

  get visual(): StatusVisual {
    return STATUS_VISUALS[this.status] ?? STATUS_VISUALS['PENDING'];
  }

  get color(): string {
    return `var(${this.visual.cssVar})`;
  }
}

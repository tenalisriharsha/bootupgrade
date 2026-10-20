import { TestBed } from '@angular/core/testing';
import { StatusBadgeComponent } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  function render(status: string): HTMLElement {
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentInstance.status = status;
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [StatusBadgeComponent] });
  });

  it('renders a label for a known status, never color alone', () => {
    const el = render('SUCCEEDED');
    expect(el.textContent).toContain('Succeeded');
    expect(el.querySelector('span[aria-hidden="true"]')?.textContent?.length).toBeGreaterThan(0);
  });

  it('falls back to pending for an unrecognized status', () => {
    const el = render('SOMETHING_UNKNOWN');
    expect(el.textContent).toContain('Pending');
  });

  it('renders failed and partial distinctly', () => {
    expect(render('FAILED').textContent).toContain('Failed');
    expect(render('PARTIAL').textContent).toContain('Partial');
  });
});

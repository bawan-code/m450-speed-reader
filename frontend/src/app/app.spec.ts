import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  let el: HTMLElement;
  const text = (selector: string) => el.querySelector(selector)?.textContent?.trim();

  /** Opens a file through the picker and waits until `expectation` holds. */
  async function open(content: string, expectation: () => void) {
    const input = el.querySelector<HTMLInputElement>('input[type=file]')!;
    Object.defineProperty(input, 'files', { value: [new File([content], 'tale.txt')], configurable: true });
    input.dispatchEvent(new Event('change'));
    await vi.waitFor(() => {
      TestBed.tick();
      expectation();
    });
  }

  beforeEach(async () => {
    const fixture = TestBed.createComponent(App);
    el = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => vi.useRealTimers());

  it('asks for a scroll before anything else', () => {
    expect(el.querySelector('input[type=file]')).toBeTruthy();
    expect(el.querySelector('.word')).toBeNull();
  });

  it('reads an opened scroll word by word and stops at the end', async () => {
    await open('Once upon a time.', () => expect(text('.word')).toBe('Once'));
    expect(text('.pivot')).toBe('n');

    vi.useFakeTimers();
    document.body.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));
    for (const next of ['upon', 'a', 'time.']) {
      TestBed.tick();
      vi.advanceTimersByTime(200); // one word at the default 300 wpm
      TestBed.tick();
      expect(text('.word')).toBe(next);
    }
    expect(el.querySelector('.play')?.getAttribute('aria-label')).toBe('Play');
  });

  it('turns away blank and binary files', async () => {
    await open(' \n ', () => expect(text('[role=alert]')).toBe('That scroll is blank.'));
    await open('PK\u0003\u0004\u0000\u0000word/document.xml', () =>
      expect(text('[role=alert]')).toBe('Only plain-text (.txt) scrolls can be read.'),
    );
  });
});

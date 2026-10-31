import { delayMs, readText, splitWord, toWords } from './reader';

describe('toWords', () => {
  it('splits on any whitespace and after dashes', () => {
    expect(toWords('  Once upon\n\na\ttime—far — away  ')).toEqual([
      'Once', 'upon', 'a', 'time—', 'far', '—', 'away',
    ]);
  });

  it('keeps hyphenated words whole and returns nothing for blank text', () => {
    expect(toWords('lantern-keeper')).toEqual(['lantern-keeper']);
    expect(toWords(' \n\t ')).toEqual([]);
  });
});

describe('splitWord', () => {
  it('puts the pivot near the start of the word', () => {
    expect(splitWord('a')).toEqual(['', 'a', '']);
    expect(splitWord('the')).toEqual(['t', 'h', 'e']);
    expect(splitWord('reading')).toEqual(['re', 'a', 'ding']);
    expect(splitWord('incomprehensibilities')).toEqual(['inco', 'm', 'prehensibilities']);
  });

  it('never splits a letter from its accent', () => {
    expect(splitWord('née')).toEqual(['n', 'é', 'e']);
  });
});

describe('delayMs', () => {
  it('scales with speed and pauses at punctuation and long words', () => {
    expect(delayMs('word', 300)).toBe(200);
    expect(delayMs('word', 600)).toBe(100);
    expect(delayMs('end.', 300)).toBe(500);
    expect(delayMs('said.”', 300)).toBe(500);
    expect(delayMs('then,', 300)).toBe(300);
    expect(delayMs('then—', 300)).toBe(300);
    expect(delayMs('windowsill', 300)).toBeCloseTo(260);
  });
});

describe('readText', () => {
  const bytes = (...b: number[]) => new Blob([new Uint8Array(b)]);

  it('decodes UTF-8 and strips its byte-order mark', async () => {
    expect(await readText(new Blob(['naïve café']))).toBe('naïve café');
    expect(await readText(bytes(0xef, 0xbb, 0xbf, 0x68, 0x69))).toBe('hi');
  });

  it('falls back to Windows-1252 for legacy ANSI files', async () => {
    expect(await readText(bytes(0x47, 0x72, 0xfc, 0xdf, 0x65))).toBe('Grüße');
  });

  it('decodes UTF-16 files with a byte-order mark', async () => {
    expect(await readText(bytes(0xff, 0xfe, 0x68, 0x00, 0xe9, 0x00))).toBe('hé');
    expect(await readText(bytes(0xfe, 0xff, 0x00, 0x68, 0x00, 0xe9))).toBe('hé');
  });

  it('handles empty files', async () => {
    expect(await readText(bytes())).toBe('');
  });
});

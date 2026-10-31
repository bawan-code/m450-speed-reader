const graphemes = new Intl.Segmenter();

/** Splits text into display words, breaking after dashes so "then—when" shows as "then—", "when". */
export function toWords(text: string): string[] {
  return text.split(/\s+|(?<=[—–])/).filter(Boolean);
}

/** Splits a word around its optimal recognition point: the letter the eye fixates on. */
export function splitWord(word: string): [pre: string, pivot: string, post: string] {
  const chars = Array.from(graphemes.segment(word), (s) => s.segment);
  const p = Math.min(4, Math.floor((chars.length + 2) / 4));
  return [chars.slice(0, p).join(''), chars[p] ?? '', chars.slice(p + 1).join('')];
}

/** How long a word stays on screen: sentences and clauses get a breath, long words a little extra. */
export function delayMs(word: string, wpm: number): number {
  const ms = 60_000 / wpm;
  if (/[.!?…]["'”’»)\]]*$/.test(word)) return ms * 2.5;
  if (/[,;:—–]["'”’»)\]]*$/.test(word)) return ms * 1.5;
  return word.length > 8 ? ms * 1.3 : ms;
}

/** Decodes a text file: UTF-16 by byte-order mark, else UTF-8, else legacy Windows "ANSI" (umlauts in old Notepad files). */
export async function readText(file: Blob): Promise<string> {
  const bytes = new Uint8Array(await file.arrayBuffer());
  const bom = (bytes[0] << 8) | bytes[1];
  const encoding = bom === 0xfffe ? 'utf-16le' : bom === 0xfeff ? 'utf-16be' : 'utf-8';
  try {
    return new TextDecoder(encoding, { fatal: true }).decode(bytes);
  } catch {
    return new TextDecoder('windows-1252').decode(bytes);
  }
}

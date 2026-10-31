import { Component, computed, effect, signal } from '@angular/core';
import { delayMs, readText, splitWord, toWords } from './reader';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  host: {
    '[class.playing]': 'playing()',
    '(document:keydown.space)': 'onSpace($event)',
    '(document:dragover)': '$event.preventDefault()',
    '(document:drop)': 'onDrop($event)',
  },
})
export class App {
  protected readonly words = signal<string[]>([]);
  protected readonly index = signal(0);
  protected readonly playing = signal(false);
  protected readonly wpm = signal(300);
  protected readonly error = signal('');
  protected readonly word = computed(() => splitWord(this.words()[this.index()] ?? ''));

  constructor() {
    // Shows the next word after the current one's delay; re-arms whenever word, speed or play state changes.
    effect((onCleanup) => {
      const words = this.words();
      const i = this.index();
      if (!this.playing()) return;
      if (i >= words.length - 1) {
        this.playing.set(false);
        return;
      }
      const timer = setTimeout(() => this.index.set(i + 1), delayMs(words[i], this.wpm()));
      onCleanup(() => clearTimeout(timer));
    });
  }

  protected toggle() {
    if (!this.playing() && this.index() >= this.words().length - 1) this.index.set(0);
    this.playing.update((p) => !p);
  }

  protected pick(input: HTMLInputElement) {
    this.load(input.files?.[0]);
    input.value = ''; // so picking the same file again still fires (change)
    input.blur(); // so Space starts reading instead of reopening the file dialog
  }

  protected onDrop(e: DragEvent) {
    e.preventDefault(); // otherwise the browser leaves the app to show the dropped file
    this.load(e.dataTransfer?.files[0]);
  }

  protected onSpace(e: Event) {
    // Buttons and the file picker already react to Space on their own.
    if (!this.words().length || (e.target as Element).closest('button, input[type=file]')) return;
    e.preventDefault();
    this.toggle();
  }

  private async load(file?: File) {
    if (!file) return;
    const text = await readText(file);
    const binary = text.includes('\0');
    this.words.set(binary ? [] : toWords(text));
    this.index.set(0);
    this.playing.set(false);
    this.error.set(
      binary ? 'Only plain-text (.txt) scrolls can be read.' : this.words().length ? '' : 'That scroll is blank.',
    );
  }
}

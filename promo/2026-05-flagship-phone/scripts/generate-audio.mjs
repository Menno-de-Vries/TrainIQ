import {mkdirSync, writeFileSync} from 'node:fs';
import {dirname, join} from 'node:path';

const sampleRate = 48000;
const seconds = 27;
const channels = 2;
const samples = sampleRate * seconds;
const out = join(process.cwd(), 'public', 'audio', 'trainiq-pulse.wav');
mkdirSync(dirname(out), {recursive: true});

const data = new Int16Array(samples * channels);

const envelope = (t) => {
  const fadeIn = Math.min(1, t / 1.6);
  const fadeOut = Math.min(1, (seconds - t) / 2.2);
  return Math.max(0, Math.min(fadeIn, fadeOut));
};

for (let i = 0; i < samples; i++) {
  const t = i / sampleRate;
  const beat = Math.pow(Math.max(0, Math.sin(2 * Math.PI * 1.25 * t)), 7);
  const pad =
    Math.sin(2 * Math.PI * 110 * t) * 0.08 +
    Math.sin(2 * Math.PI * 165 * t) * 0.045 +
    Math.sin(2 * Math.PI * 220 * t) * 0.025;
  const pulse = Math.sin(2 * Math.PI * 55 * t) * beat * 0.18;
  const tick = Math.sin(2 * Math.PI * 1760 * t) * Math.pow(Math.max(0, Math.sin(2 * Math.PI * 2.5 * t)), 18) * 0.03;
  const value = (pad + pulse + tick) * envelope(t);
  const left = Math.max(-1, Math.min(1, value * 0.95));
  const right = Math.max(-1, Math.min(1, value * 0.86 + Math.sin(2 * Math.PI * 330 * t) * 0.01 * envelope(t)));
  data[i * 2] = Math.round(left * 32767);
  data[i * 2 + 1] = Math.round(right * 32767);
}

const byteRate = sampleRate * channels * 2;
const blockAlign = channels * 2;
const dataSize = data.length * 2;
const buffer = Buffer.alloc(44 + dataSize);
buffer.write('RIFF', 0);
buffer.writeUInt32LE(36 + dataSize, 4);
buffer.write('WAVE', 8);
buffer.write('fmt ', 12);
buffer.writeUInt32LE(16, 16);
buffer.writeUInt16LE(1, 20);
buffer.writeUInt16LE(channels, 22);
buffer.writeUInt32LE(sampleRate, 24);
buffer.writeUInt32LE(byteRate, 28);
buffer.writeUInt16LE(blockAlign, 32);
buffer.writeUInt16LE(16, 34);
buffer.write('data', 36);
buffer.writeUInt32LE(dataSize, 40);
Buffer.from(data.buffer).copy(buffer, 44);
writeFileSync(out, buffer);
console.log(out);

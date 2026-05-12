import {mkdirSync} from 'node:fs';
import {execFileSync} from 'node:child_process';

const frames = [
  120, 144, 150, 160, 210, 252, 258, 268, 320, 366, 372, 382, 420, 468, 474, 484,
  530, 580, 586, 596, 640, 704, 708, 720, 744, 780,
];

mkdirSync('evidence/qa-v2', {recursive: true});
for (const frame of frames) {
  const out = `evidence/qa-v2/frame-${String(frame).padStart(3, '0')}.png`;
  execFileSync('cmd.exe', ['/d', '/s', '/c', `npx.cmd remotion still src/index.ts TrainIqFlagshipPromo ${out} --frame=${frame} --scale=0.35`], {stdio: 'inherit'});
}

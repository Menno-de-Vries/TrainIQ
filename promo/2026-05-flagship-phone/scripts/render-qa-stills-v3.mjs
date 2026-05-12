import {mkdirSync} from 'node:fs';
import {execFileSync} from 'node:child_process';

const frames = [
  0, 18, 36, 54, 72, 90, 108,
  144, 150, 160,
  168, 186, 204,
  252, 258, 268,
  270, 288, 306,
  366, 372, 382,
  390, 408,
  468, 474, 484,
  492, 510,
  580, 586, 596,
  612,
  704, 708, 720, 744,
];

mkdirSync('evidence/qa-v3', {recursive: true});
for (const frame of frames) {
  const out = `evidence/qa-v3/frame-${String(frame).padStart(3, '0')}.png`;
  execFileSync('cmd.exe', ['/d', '/s', '/c', `npx.cmd remotion still src/index.ts TrainIqFlagshipPromo ${out} --frame=${frame} --scale=0.35`], {stdio: 'inherit'});
}

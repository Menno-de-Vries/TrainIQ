import {mkdirSync} from 'node:fs';
import {execFileSync} from 'node:child_process';

mkdirSync('itch-upload-pack-v2', {recursive: true});
for (let i = 1; i <= 6; i++) {
  const out = `itch-upload-pack-v2/trainiq-itch-screenshot-${String(i).padStart(2, '0')}.png`;
  execFileSync('cmd.exe', ['/d', '/s', '/c', `npx.cmd remotion still src/index.ts ItchScreenshot${i} ${out}`], {stdio: 'inherit'});
}

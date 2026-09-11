import {test} from 'node:test';
import assert from 'node:assert/strict';
import {once} from 'node:events';
import {createGateway} from './server.mjs';

async function withServer(client, env, action) {
  const server = createGateway(client, env).listen(0, '127.0.0.1');
  await once(server, 'listening');
  try { await action(`http://127.0.0.1:${server.address().port}`); }
  finally { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
}

test('unverified retention rights never query upstream', async () => {
  await withServer({lookup: () => { throw new Error('must not query'); }}, {}, async base => {
    const response = await fetch(`${base}/food/barcode/012345678905`);
    assert.equal(response.status, 503);
    assert.deepEqual(await response.json(), {error: 'not_configured'});
    assert.equal(response.headers.get('cache-control'), 'no-store');
  });
});
test('authorized local contract distinguishes miss, auth and bad input', async () => {
  const env = {FATSECRET_DURABLE_NUTRITION_ALLOWED: 'true'};
  await withServer({lookup: async () => null}, env, async base => {
    assert.equal((await fetch(`${base}/food/barcode/012345678905`)).status, 404);
    assert.equal((await fetch(`${base}/food/barcode/bad`)).status, 400);
  });
  await withServer({lookup: async () => { const e = new Error('private response'); e.code = 'auth'; throw e; }}, env, async base => {
    const response = await fetch(`${base}/food/barcode/012345678905`);
    assert.equal(response.status, 403);
    assert.deepEqual(await response.json(), {error: 'auth'});
  });
});

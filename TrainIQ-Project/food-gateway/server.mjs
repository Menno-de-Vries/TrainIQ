import {createServer} from 'node:http';
import {createFatSecretClient} from './fatsecret.mjs';

export function createGateway(client = createFatSecretClient(), env = process.env) {
  return createServer(async (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.setHeader('Cache-Control', 'no-store');
    const match = /^\/food\/barcode\/(\d{8}|\d{12}|\d{13})$/.exec(req.url);
    if (req.method !== 'GET' || !match) { res.writeHead(400); res.end('{"error":"invalid_barcode"}'); return; }
    // TrainIQ persists meal snapshots. Standard API terms do not grant indefinite
    // nutrient storage: only enable this adapter with account-specific permission.
    if (env.FATSECRET_DURABLE_NUTRITION_ALLOWED !== 'true') {
      res.writeHead(503); res.end('{"error":"not_configured"}'); return;
    }
    try {
      const product = await client.lookup(match[1]);
      res.writeHead(product ? 200 : 404); res.end(JSON.stringify(product || {error: 'not_found'}));
    } catch (error) {
      const statuses = {invalid_barcode: 400, auth: 403, not_configured: 503, unsupported_serving: 422, invalid_response: 502, upstream: 502};
      const code = Object.hasOwn(statuses, error.code) ? error.code : 'upstream';
      res.writeHead(statuses[code]); res.end(JSON.stringify({error: code}));
    }
  });
}

if (process.argv[1] && import.meta.url === new URL(`file:///${process.argv[1].replaceAll('\\', '/')}`).href) {
  createGateway().listen(Number(process.env.PORT || 8787), '127.0.0.1');
}

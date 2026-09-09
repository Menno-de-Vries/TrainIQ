export class ProviderError extends Error {
  constructor(code) { super(code); this.code = code; }
}

export function gtin13(value) {
  if (!/^(?:\d{8}|\d{12}|\d{13})$/.test(value)) throw new ProviderError('invalid_barcode');
  const sum = [...value.slice(0, -1)].reverse().reduce((n, c, i) => n + Number(c) * (i % 2 ? 1 : 3), 0);
  if ((10 - sum % 10) % 10 !== Number(value.at(-1))) throw new ProviderError('invalid_barcode');
  return value.padStart(13, '0');
}

async function jsonResponse(response) {
  if (response.status === 401 || response.status === 403) throw new ProviderError('auth');
  if (!response.ok) throw new ProviderError('upstream');
  const reader = response.body.getReader();
  let length = 0; const chunks = [];
  try {
    for (;;) {
      const {done, value} = await reader.read(); if (done) break;
      length += value.length;
      if (length > 262144) throw new ProviderError('invalid_response');
      chunks.push(value);
    }
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  } catch (error) {
    if (error instanceof ProviderError) throw error;
    throw new ProviderError('invalid_response');
  } finally { await reader.cancel(); }
}

export function normalizeFood(response) {
  if (response.error) {
    const code = Number(response.error.code);
    if (code === 211) return null;
    throw new ProviderError([13, 14, 5, 9].includes(code) ? 'auth' : 'upstream');
  }
  const food = response.food;
  const all = food?.servings?.serving;
  const servings = Array.isArray(all) ? all : all ? [all] : [];
  // Current app product values are per 100g. Never reinterpret ml/oz as grams.
  const serving = servings.find(s => s.metric_serving_unit === 'g' && Number(s.metric_serving_amount) > 0);
  if (!food?.food_name || !serving) throw new ProviderError('unsupported_serving');
  const amount = Number(serving.metric_serving_amount);
  const nutrient = name => {
    if (serving[name] == null || String(serving[name]).trim() === '') throw new ProviderError('invalid_response');
    const value = Number(serving[name]) * 100 / amount;
    if (!Number.isFinite(value) || value < 0 || value > (name === 'calories' ? 5000 : 1000)) throw new ProviderError('invalid_response');
    return value;
  };
  return {name: String(food.food_name), foodId: String(food.food_id), servingId: String(serving.serving_id),
    caloriesPer100g: nutrient('calories'), proteinPer100g: nutrient('protein'),
    carbsPer100g: nutrient('carbohydrate'), fatPer100g: nutrient('fat'), provider: 'FATSECRET'};
}

export function createFatSecretClient({env = process.env, fetchImpl = fetch, now = Date.now} = {}) {
  let cached; let pending;
  const scopes = env.FATSECRET_SCOPES || 'barcode';
  const request = async (url, options = {}) => {
    try { return await jsonResponse(await fetchImpl(url, {...options, redirect: 'error', signal: AbortSignal.timeout(5000)})); }
    catch (error) { if (error instanceof ProviderError) throw error; throw new ProviderError('upstream'); }
  };
  async function token() {
    if (cached && cached.until > now()) return cached.value;
    if (pending) return pending;
    pending = (async () => {
      if (!env.FATSECRET_CLIENT_ID || !env.FATSECRET_CLIENT_SECRET) throw new ProviderError('not_configured');
      const start = now();
      const result = await request('https://oauth.fatsecret.com/connect/token', {method: 'POST',
        headers: {Authorization: `Basic ${Buffer.from(`${env.FATSECRET_CLIENT_ID}:${env.FATSECRET_CLIENT_SECRET}`).toString('base64')}`, 'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams({grant_type: 'client_credentials', scope: scopes}).toString()});
      if (result.error || typeof result.access_token !== 'string' || !Number.isFinite(Number(result.expires_in)) || Number(result.expires_in) <= 0) throw new ProviderError('auth');
      cached = {value: result.access_token, until: start + Math.max(0, Number(result.expires_in) - 60) * 1000};
      return cached.value;
    })();
    try { return await pending; } finally { pending = undefined; }
  }
  return {async lookup(barcode) {
    const normalized = gtin13(barcode);
    const params = new URLSearchParams({barcode: normalized, format: 'json'});
    if (env.FATSECRET_REGION) {
      if (!scopes.split(/\s+/).includes('localization')) throw new ProviderError('auth');
      params.set('region', env.FATSECRET_REGION);
      if (env.FATSECRET_LANGUAGE) params.set('language', env.FATSECRET_LANGUAGE);
    }
    try {
      return normalizeFood(await request(`https://platform.fatsecret.com/rest/food/barcode/find-by-id/v2?${params}`, {headers: {Authorization: `Bearer ${await token()}`}}));
    } catch (error) { if (error.code === 'auth') cached = undefined; throw error; }
  }};
}

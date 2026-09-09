import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createFatSecretClient, gtin13, normalizeFood} from './fatsecret.mjs';
const product = {food: {food_id: '1', food_name: 'Fixture', servings: {serving: {serving_id: '2', metric_serving_amount: '50', metric_serving_unit: 'g', calories: '100', protein: '2', carbohydrate: '10', fat: '4'}}}};
test('GTIN validation and padding', () => {
  assert.equal(gtin13('012345678905'), '0012345678905');
  assert.equal(gtin13('96385074'), '0000096385074');
  assert.throws(() => gtin13('12345678'));
  assert.throws(() => gtin13('abc012345678905'));
});
test('one provider, gram serving semantics and missing fields', () => {
  assert.equal(normalizeFood(product).caloriesPer100g, 200);
  assert.equal(normalizeFood({error: {code: '211'}}), null);
  assert.throws(() => normalizeFood({error: {code: '14'}}), {code: 'auth'});
  assert.throws(() => normalizeFood({food: {...product.food, servings: {serving: {...product.food.servings.serving, metric_serving_unit: 'ml'}}}}), {code: 'unsupported_serving'});
});
test('server token single flight and expiry, no implicit localization', async () => {
  let calls = []; let time = 0;
  const client = createFatSecretClient({env: {FATSECRET_CLIENT_ID: 'fixture', FATSECRET_CLIENT_SECRET: 'fixture'}, now: () => time,
    fetchImpl: async (url, options) => { calls.push({url, options}); return Response.json(url.includes('connect/token') ? {access_token: 'fixture-token', expires_in: 120} : product); }});
  await Promise.all([client.lookup('012345678905'), client.lookup('012345678905')]);
  assert.equal(calls.filter(c => c.url.includes('connect/token')).length, 1);
  assert.equal(calls.some(c => c.url.includes('region=')), false);
  time = 61000; await client.lookup('012345678905');
  assert.equal(calls.filter(c => c.url.includes('connect/token')).length, 2);
});
test('missing credentials and unentitled localization fail closed', async () => {
  const fetchImpl = () => { throw new Error('must not call'); };
  await assert.rejects(createFatSecretClient({env: {}, fetchImpl}).lookup('012345678905'), {code: 'not_configured'});
  await assert.rejects(createFatSecretClient({env: {FATSECRET_REGION: 'NL'}, fetchImpl}).lookup('012345678905'), {code: 'auth'});
});
test('auth and transport errors retain safe categories', async () => {
  const env = {FATSECRET_CLIENT_ID: 'fixture', FATSECRET_CLIENT_SECRET: 'fixture'};
  await assert.rejects(createFatSecretClient({env, fetchImpl: async () => new Response('', {status: 401})}).lookup('012345678905'), {code: 'auth'});
  await assert.rejects(createFatSecretClient({env, fetchImpl: async () => { throw new Error('sensitive upstream text'); }}).lookup('012345678905'), {message: 'upstream'});
});

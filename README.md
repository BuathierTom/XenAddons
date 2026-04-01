# XenAddons Cosmetics API

API HTTP en TypeScript qui sert les cosmetics des joueurs Minecraft depuis `cosmeticPeople.json`, avec :

- une API admin protégée par `x-api-key`
- une authentification utilisateur via `better-auth` + `genericOAuth` avec MC-ID
- une route self-service `PATCH /me/username`

## Prérequis

- Node.js 20+

## Installation

```bash
npm install
```

## Configuration

```bash
cp .env.example .env
```

Variables principales :

- `PORT`, `HOST`, `TRUST_PROXY`
- `COSMETICS_FILE`
- `API_KEY`
- `FRONTEND_ORIGIN`
- `BETTER_AUTH_URL`
- `BETTER_AUTH_SECRET`
- `BETTER_AUTH_BASE_PATH`
- `SESSION_TTL_SECONDS`
- `SESSION_COOKIE_SECURE`
- `SESSION_COOKIE_SAME_SITE`
- `MCID_CLIENT_ID`
- `MCID_CLIENT_SECRET`
- `MCID_DISCOVERY_URL`

## Lancement

```bash
npm run dev
npm run build
npm run start
npm test
```

## Endpoints

- `GET /health`
- `GET /me`
- `PATCH /me/username`
- `GET /v1/cosmetics`
- `GET /v1/cosmetics/:uuid`
- `PATCH /v1/cosmetics/:uuid/username`
- `GET|POST|PATCH|PUT|DELETE ${BETTER_AUTH_BASE_PATH}/*`

## Intégration MC-ID Avec Better Auth

Le backend utilise `better-auth` avec `genericOAuth` et le provider `mc-id` :

- discovery URL : `https://mc-id.com/.well-known/openid-configuration`
- scopes : `openid profile email connections`
- validation stricte de l’issuer activée
- session stateless côté serveur via cookie chiffré/signé par Better Auth

Le montage Better Auth est géré côté backend sur `${BETTER_AUTH_BASE_PATH}`. Par défaut :

```text
/api/auth/*
```

## Frontend

Dans ton front, configure le client Better Auth avec le plugin `genericOAuthClient()` puis lance la connexion MC-ID via le provider `mc-id`.

Exemple côté front :

```ts
import { createAuthClient } from "better-auth/client";
import { genericOAuthClient } from "better-auth/client/plugins";

export const authClient = createAuthClient({
  baseURL: "http://127.0.0.1:3000",
  plugins: [genericOAuthClient()]
});
```

```ts
await authClient.signIn.oauth2({
  providerId: "mc-id",
  callbackURL: "http://localhost:5173/dashboard",
  errorCallbackURL: "http://localhost:5173/login?error=1"
});
```

Une fois connecté :

```ts
const meResponse = await fetch("http://127.0.0.1:3000/me", {
  credentials: "include"
});
const me = await meResponse.json();

await fetch("http://127.0.0.1:3000/me/username", {
  method: "PATCH",
  credentials: "include",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    username: "NouveauPseudo"
  })
});
```

## Règles de Sécurité

- Le front public n’utilise jamais `API_KEY`.
- `/v1/cosmetics*` reste réservé à l’admin/serveur-à-serveur.
- `/me` et `/me/username` reposent sur la session Better Auth.
- `PATCH /me/username` exige une session valide et un `Origin` égal à `FRONTEND_ORIGIN`.
- Le login MC-ID est refusé si le compte Minecraft authentifié n’existe pas dans `cosmeticPeople.json`.
- L’UUID MC-ID est normalisé au format UUID canonique avec tirets, même si MC-ID renvoie un UUID compact sans tirets.
- `GET /health` reste public.

## Exemples Curl

Admin :

```bash
curl http://127.0.0.1:3000/v1/cosmetics \
  -H 'x-api-key: change-me'
```

Session utilisateur :

```bash
curl http://127.0.0.1:3000/me \
  -H 'Cookie: <session-cookie>'
```

```bash
curl -X PATCH http://127.0.0.1:3000/me/username \
  -H 'Origin: http://localhost:5173' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: <session-cookie>' \
  -d '{"username":"RenamedPlayer"}'
```

## Notes de Compatibilité

- Les badges sont renvoyés dans `badges` et jamais concaténés dans `displayName`.
- Si `badges` est absent dans le JSON source, l'API tente d'extraire les suffixes legacy comme `{capy}` ou `{france}` à la fin de `displayName`.
- Les entrées invalides sont ignorées avec un warning dans les logs, sans arrêter le serveur.

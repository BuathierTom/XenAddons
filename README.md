# XenAddons Cosmetics API

API HTTP en TypeScript qui sert les cosmetics des joueurs Minecraft depuis `cosmeticPeople.json`, avec :

- une API admin protégée par `x-api-key`
- une authentification utilisateur via MC-ID par code de vérification ingame
- une route self-service `PATCH /me/cosmetics`

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
- `SESSION_SECRET`
- `SESSION_TTL_SECONDS`
- `SESSION_COOKIE_SECURE`
- `SESSION_COOKIE_SAME_SITE`
- `MCID_API_KEY`
- `MCID_CODE_REQUEST_URL`
- `MCID_CODE_VERIFY_URL`

Compatibilité :

- `BETTER_AUTH_SECRET` reste accepté comme fallback de `SESSION_SECRET` pour éviter de casser un ancien `.env`

## Lancement

```bash
npm run dev
npm run build
npm run start
npm test
```

## Endpoints

- `GET /health`
- `GET /auth/mc-id/config`
- `POST /auth/mc-id/request-code`
- `POST /auth/mc-id/verify-code`
- `POST /auth/logout`
- `GET /me`
- `PATCH /me/cosmetics`
- `GET /v1/cosmetics`
- `GET /v1/cosmetics/:uuid`
- `PATCH /v1/cosmetics/:uuid`

## Flux MC-ID Par Code De Vérification

Le backend n’utilise plus le flux OAuth redirect. Le flux attendu est :

1. l’utilisateur saisit son pseudo Minecraft, un alias connu, ou son UUID
2. le front appelle `POST /auth/mc-id/request-code`
3. le backend résout ce joueur dans `cosmeticPeople.json`
4. le backend appelle `POST https://mc-id.com/api/v1/codes/request` avec `uuid`
5. l’utilisateur rejoint le serveur Minecraft configuré dans MC-ID
6. le plugin/serveur affiche le code de vérification en jeu
7. l’utilisateur saisit ce code dans le front
8. le front appelle `POST /auth/mc-id/verify-code`
9. le backend appelle `POST https://mc-id.com/api/v1/codes/verify`
10. si le code est valide, le backend crée une session serveur en cookie signé `HttpOnly`
11. le front consomme ensuite `/me` et `PATCH /me/cosmetics` avec `credentials: "include"`

## Frontend

Le front peut récupérer la config minimale du flow via `GET /auth/mc-id/config`.

Réponse :

```json
{
  "codeLength": 6,
  "codeTtlSeconds": 300,
  "flow": "verification_code",
  "identifierField": "identifier"
}
```

Demander un code :

```ts
await fetch("http://localhost:3000/auth/mc-id/request-code", {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    identifier: "dtXenus"
  })
});
```

Réponse succès :

```json
{
  "expiresAt": "2026-04-01T12:05:00.000Z",
  "message": "Successfully generated the code.",
  "player": {
    "uuid": "24c56ab5-b504-4d38-bd06-e5af6686fcaa",
    "username": "dtXenus",
    "aliases": ["dtXenus"],
    "displayName": "InitialName",
    "scale": null
  },
  "requested": true
}
```

Vérifier le code :

```ts
await fetch("http://localhost:3000/auth/mc-id/verify-code", {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    uuid: "24c56ab5-b504-4d38-bd06-e5af6686fcaa",
    code: "123456"
  })
});
```

Ensuite :

```ts
const me = await fetch("http://localhost:3000/me", {
  credentials: "include"
}).then((response) => response.json());
```

```ts
await fetch("http://localhost:3000/me/cosmetics", {
  method: "PATCH",
  credentials: "include",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({
    displayName: "NouveauDisplayName",
    scale: {
      x: 2,
      y: 0.5,
      z: 1
    }
  })
});
```

## Règles de Sécurité

- Le front public n’utilise jamais `API_KEY`.
- `/v1/cosmetics*` reste réservé à l’admin/serveur-à-serveur.
- `/me` et `/me/cosmetics` reposent sur une session backend en cookie signé.
- `POST /auth/mc-id/request-code` et `POST /auth/mc-id/verify-code` utilisent l’API key MC-ID côté serveur, jamais côté front.
- Les routes sensibles exigent un `Origin` égal à `FRONTEND_ORIGIN`.
- Le login MC-ID est refusé si le joueur n’existe pas dans `cosmeticPeople.json`.
- Les UUID envoyés à MC-ID sont normalisés au format compact 32 caractères.
- Les UUID renvoyés par l’API de cette app sont exposés au format UUID canonique avec tirets.
- Les seules écritures autorisées sont `displayName` et `scale`.
- `username` et `aliases` sont en lecture seule et ne peuvent pas être modifiés via l’API.
- L’API n’expose plus de champ `badges`.
- `displayName` est renvoyé tel quel, y compris les suffixes manuels comme `{capy}` ou `{france}`.
- `GET /health` reste public.

## Exemples Curl

Admin :

```bash
curl http://localhost:3000/v1/cosmetics \
  -H 'x-api-key: change-me'
```

Config front :

```bash
curl http://localhost:3000/auth/mc-id/config
```

Demande de code :

```bash
curl -X POST http://localhost:3000/auth/mc-id/request-code \
  -H 'Origin: http://localhost:5173' \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"dtXenus"}'
```

Vérification du code :

```bash
curl -X POST http://localhost:3000/auth/mc-id/verify-code \
  -H 'Origin: http://localhost:5173' \
  -H 'Content-Type: application/json' \
  -d '{"uuid":"24c56ab5-b504-4d38-bd06-e5af6686fcaa","code":"123456"}'
```

Session utilisateur :

```bash
curl http://localhost:3000/me \
  -H 'Cookie: <session-cookie>'
```

```bash
curl -X PATCH http://localhost:3000/me/cosmetics \
  -H 'Origin: http://localhost:5173' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: <session-cookie>' \
  -d '{"displayName":"UpdatedName","scale":{"x":1.25,"y":0.75,"z":0.5}}'
```

## Notes de Compatibilité

- Les entrées invalides sont ignorées avec un warning dans les logs, sans arrêter le serveur.
- Les sessions sont en mémoire : un redémarrage du backend invalide les sessions en cours.

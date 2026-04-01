# XenAddons Cosmetics API

API HTTP en TypeScript qui sert les cosmetics des joueurs Minecraft depuis un fichier local `cosmeticPeople.json`.

## Prérequis

- Node.js 20+

## Installation

```bash
npm install
```

## Configuration

Copiez `.env.example` vers `.env` si vous voulez personnaliser les variables :

```bash
cp .env.example .env
```

Variables disponibles :

- `PORT` : port HTTP, défaut `3000`
- `HOST` : host d'écoute, défaut `0.0.0.0`
- `COSMETICS_FILE` : chemin vers le fichier source, défaut `./data/cosmeticPeople.json`
- `API_KEY` : clé API obligatoire pour accéder aux routes `/v1/cosmetics`

## Lancement

Développement :

```bash
npm run dev
```

Build :

```bash
npm run build
```

Production :

```bash
npm run start
```

Tests :

```bash
npm test
```

## Endpoints

- `GET /health`
- `GET /v1/cosmetics`
- `GET /v1/cosmetics/:uuid`
- `PATCH /v1/cosmetics/:uuid/username`

## Authentification

Toutes les routes `/v1/cosmetics` exigent l'en-tête HTTP `x-api-key`.

Exemple :

```bash
curl http://127.0.0.1:3000/v1/cosmetics \
  -H 'x-api-key: change-me'
```

`GET /health` reste public pour les checks de supervision.

## Notes de compatibilité

- Les badges sont renvoyés dans `badges` et jamais concaténés dans la réponse `displayName`.
- Si `badges` est absent dans le JSON source, l'API tente d'extraire les suffixes legacy comme `{capy}` ou `{france}` à la fin de `displayName`.
- Les entrées invalides sont ignorées avec un warning dans les logs, sans arrêter le serveur.

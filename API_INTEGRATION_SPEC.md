# Bizo Mobile API Integration Spec

Ce document est destine a une equipe mobile ou a une IA chargee du client Android/iOS.
Il decrit les contrats reels de l'API, les user flows, les formats de reponse, les erreurs a gerer et les contraintes metier observees.

Base URL de production :

```txt
https://bizo.aiko.qzz.io/api/v1
```

## 1. Resume executif

Backend valide en production sur les flux suivants :

- inscription / connexion / deconnexion
- reset password complet
- lecture / mise a jour du profil
- upload avatar
- creation et lecture d'annonces avec upload image
- recherche
- favoris
- conversations
- lecture / envoi de messages
- transactions
- avis
- notifications in-app
- enregistrement FCM token
- notifications FCM reelles via backend
- previews web vendeur / annonce

Ce document privilegie l'integration mobile reelle, pas seulement la description des routes.

## 2. Regles globales

### 2.1 Authentification

Les routes protegees utilisent un Bearer token Sanctum.

Header a envoyer :

```txt
Authorization: Bearer <token>
```

### 2.2 Format de reponse

L'API retourne principalement :

- des objets JSON simples
- des objets enveloppes sous `data`
- des collections Laravel paginees sous `data`, `links`, `meta`

### 2.3 Formats de requete

- JSON : `Content-Type: application/json`
- Uploads : `multipart/form-data`

### 2.4 Pagination

Les listes paginees suivent le format Laravel standard :

```json
{
  "data": [],
  "links": {
    "first": "https://...",
    "last": "https://...",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "path": "https://...",
    "per_page": 20,
    "to": 3,
    "total": 3
  }
}
```

### 2.5 Rate limit

Rate limit nominal observe :

- `120 req/min`

Le client mobile doit gerer les `429`.

### 2.6 Dates

Les dates sont retournees en ISO 8601 UTC.

Exemple :

```txt
2026-05-24T15:03:43.000000Z
```

### 2.7 Erreurs

Codes les plus utiles :

- `200` succes standard
- `201` creation
- `204` suppression sans body
- `400` erreur metier
- `401` authentification invalide
- `403` action interdite
- `404` ressource introuvable
- `409` conflit metier
- `422` validation
- `429` rate limit
- `500` erreur serveur

Format `422` typique :

```json
{
  "message": "The given data was invalid.",
  "errors": {
    "field_name": [
      "Message d'erreur"
    ]
  }
}
```

## 3. Modele de session mobile

Strategie recommande cote app :

1. `login` ou `register`
2. stocker le `token` en local securise
3. appeler `GET /profile`
4. recuperer ou generer le token FCM local
5. appeler `POST /auth/fcm-token`
6. a la deconnexion, appeler `POST /auth/logout` puis supprimer le token local

## 4. User object

Format reel du `UserResource` :

```json
{
  "id": "uuid",
  "email": "user@example.com",
  "display_name": "Aiko",
  "username": "aiko_dev",
  "photo_url": "/storage/avatars/xxx.png",
  "bio": "Bio utilisateur",
  "country_code": "BJ",
  "rating": 4.5,
  "review_count": 12,
  "total_sales": 4,
  "is_verified": false,
  "has_seen_onboarding": true,
  "created_at": "2026-05-24T15:03:43.000000Z"
}
```

Notes :

- `username` peut etre `null`
- `photo_url` peut etre `null`
- `rating`, `review_count`, `total_sales` existent reellement et sont a afficher cote profil vendeur

## 5. Auth flows

### 5.1 Register

Route :

```txt
POST /auth/register
```

Body :

```json
{
  "email": "user@example.com",
  "password": "password123",
  "password_confirmation": "password123",
  "display_name": "Aiko",
  "username": "aiko_dev"
}
```

Contraintes :

- `email` unique
- `password` min 8
- `password_confirmation` doit matcher
- `display_name` max 80
- `username` nullable, regex `^[a-z0-9_]{3,30}$`

Succes :

- code `201`
- retourne `token` + `user`

Exemple :

```json
{
  "token": "plain-text-token",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "display_name": "Aiko",
    "username": "aiko_dev",
    "photo_url": null,
    "bio": null,
    "country_code": null,
    "rating": 0,
    "review_count": 0,
    "total_sales": 0,
    "is_verified": false,
    "has_seen_onboarding": false,
    "created_at": "2026-05-24T15:03:43.000000Z"
  }
}
```

Erreurs a gerer :

- `422` email deja pris
- `422` username deja pris
- `422` validation password

### 5.2 Login

Route :

```txt
POST /auth/login
```

Body :

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Succes :

- code `200`
- retourne `token` + `user`

Erreur :

- `401`

Body observe :

```json
{
  "message": "Identifiants incorrects"
}
```

### 5.3 Logout

Route :

```txt
POST /auth/logout
```

Succes :

```json
{
  "message": "Déconnexion réussie."
}
```

Comportement :

- supprime le token courant si present

### 5.4 Forgot password

Route :

```txt
POST /auth/password/reset
```

Body :

```json
{
  "email": "user@example.com"
}
```

Succes :

- code `200`
- message generique, meme si l'email n'existe pas

Reponse observee :

```json
{
  "message": "Si ce compte existe, un lien de réinitialisation a été envoyé par email."
}
```

### 5.5 Reset password

Route :

```txt
POST /auth/password/update
```

Body :

```json
{
  "token": "reset-token",
  "email": "user@example.com",
  "password": "NewPassword123",
  "password_confirmation": "NewPassword123"
}
```

Succes :

```json
{
  "message": "Mot de passe réinitialisé avec succès."
}
```

Effet metier :

- change le mot de passe
- revoque les tokens Sanctum existants de l'utilisateur

### 5.6 Save FCM token

Route :

```txt
POST /auth/fcm-token
```

Body :

```json
{
  "fcm_token": "device-token"
}
```

Succes observe :

```json
{
  "message": "Token FCM mis a jour avec succes."
}
```

User flow recommande :

1. login
2. generer / recuperer le token FCM
3. appeler `/auth/fcm-token`
4. refaire l'appel si Firebase regenere le token

## 6. Profil courant

### 6.1 Lire son profil

Routes :

- `GET /me`
- `GET /profile`

Les deux servent au profil courant.

Succes :

- code `200`
- retourne un `UserResource`

### 6.2 Mettre a jour le profil

Route :

```txt
PUT /profile
```

Body partiel supporte.

Champs acceptes :

- `display_name`
- `username`
- `bio`
- `country_code`
- `is_profile_public`
- `has_seen_onboarding`
- `notif_messages`
- `notif_troc`
- `notif_rappels`
- `notif_favoris`

Exemple :

```json
{
  "display_name": "Seller A Updated",
  "bio": "Bio de test",
  "notif_messages": false
}
```

Validation utile :

- `display_name` max 80
- `username` nullable, unique, regex `^[a-z0-9_]{3,30}$`
- `bio` max 500

Succes :

- retourne le `UserResource` a jour

### 6.3 Upload avatar

Route :

```txt
POST /profile/avatar
```

Format :

- `multipart/form-data`

Champ :

- `avatar`

Validation :

- image requise
- `jpg|jpeg|png|webp`
- max `5 Mo`

Succes :

- code `200`
- retourne le `UserResource` mis a jour

Comportement :

- si un ancien avatar existe, il est supprime

### 6.4 Delete account

Route :

```txt
DELETE /profile
```

Succes :

- `204`

Effets metier :

- suppression des tokens API
- soft delete user
- nettoyage conversations/messages lies
- nettoyage favoris/notifications/demandes/reports lies
- suppression logique des annonces du user
- nettoyage photos annonce + avatar

## 7. Profils publics vendeur

### 7.1 Lire un profil public

Route :

```txt
GET /users/{uid}
```

Retourne uniquement un profil avec `is_profile_public = true`.

Sinon :

- `404`

### 7.2 Lire les annonces publiques d'un vendeur

Route :

```txt
GET /users/{uid}/listings
```

Comportement :

- seulement si le profil est public
- seulement les annonces `active`
- tri boostees puis recentes

## 8. Listing object

Format reel du `ListingResource` :

```json
{
  "id": "uuid",
  "title": "iPhone 13",
  "description": "Description",
  "type": "VENTE",
  "price": "180000",
  "cash_complement": null,
  "exchange_for": null,
  "category": "electronique",
  "condition": "bon",
  "delivery_mode": "les_deux",
  "photos": [
    "/storage/photos/xxx.webp"
  ],
  "country": "BJ",
  "city": "Cotonou",
  "neighborhood": "Cadjehoun",
  "tags": [],
  "view_count": 0,
  "favorite_count": 0,
  "status": "active",
  "is_boosted": false,
  "price_history": [],
  "expires_at": "2026-06-23T13:58:02.000000Z",
  "created_at": "2026-05-24T13:58:02.000000Z",
  "updated_at": "2026-05-24T13:58:02.000000Z",
  "owner": {
    "...": "UserResource"
  }
}
```

Notes :

- `photos` est toujours un tableau
- les images sont stockees en WebP pour les annonces
- `owner` est embarque sur plusieurs endpoints publics

## 9. Feed, recherche, annonces

### 9.1 Feed principal

Route :

```txt
GET /listings
```

Filtres supportes :

- `per_page`
- `category`
- `type`
- `condition`
- `country`
- `city`
- `min_price`
- `max_price`

Comportement :

- seulement annonces actives
- tri `is_boosted desc`, puis `created_at desc`

### 9.2 Recherche

Route :

```txt
GET /search?q=iphone
```

Filtres supportes :

- `category`
- `type`
- `city`
- `condition`
- `min_price`
- `max_price`

Comportement :

- cherche dans `title_search` et `description`
- favorise les titres qui commencent par la requete
- retourne seulement les annonces actives

### 9.3 Detail annonce

Route :

```txt
GET /listings/{id}
```

Effet metier :

- incremente `view_count`

### 9.4 Creer une annonce

Route :

```txt
POST /listings
```

Format :

- `multipart/form-data`

Champs :

- `title` requis
- `description` requis
- `type` requis : `VENTE|TROC|TROC_CASH`
- `price`
- `cash_complement`
- `exchange_for`
- `category` requis
- `condition` requis : `neuf|excellent|bon|correct`
- `delivery_mode` requis : `main_propre|livraison|les_deux`
- `country` requis
- `city` requis
- `neighborhood`
- `tags[]`
- `photos[]` requis

Regles metier :

- `type = VENTE` => `price` requis
- `type = TROC|TROC_CASH` => `exchange_for` requis
- `photos[]` min `1`, max `10`
- chaque photo max `5 Mo`

Succes :

- `201`
- retourne `data` avec `ListingResource`

### 9.5 Mettre a jour une annonce

Route :

```txt
PUT /listings/{id}
```

Regles :

- proprietaire uniquement
- mise a jour partielle
- si changement vers `VENTE`, `price` doit etre coherent
- si changement vers `TROC|TROC_CASH`, `exchange_for` doit etre coherent

### 9.6 Supprimer une annonce

Route :

```txt
DELETE /listings/{id}
```

Regles :

- proprietaire uniquement

Effet metier :

- `status = deleted`
- soft delete
- suppression des photos associees

### 9.7 Ajouter des photos

Route :

```txt
POST /listings/{id}/photos
```

Regles :

- proprietaire uniquement
- max total absolu `10`

Erreur observee si depassement :

- `422`

### 9.8 Supprimer une photo

Route :

```txt
DELETE /listings/{id}/photos/{idx}
```

`idx` = index du tableau `photos`

### 9.9 Boost

Route :

```txt
POST /listings/{id}/boost
```

### 9.10 Renew

Route :

```txt
POST /listings/{id}/renew
```

### 9.11 Mes annonces

Route :

```txt
GET /my/listings
```

## 10. Conversations et messages

## 10.1 Conversation object

Format reel :

```json
{
  "id": "conv-id",
  "listing_id": "listing-uuid",
  "listing_title": "Titre",
  "listing_photo": "/storage/photos/xxx.webp",
  "last_message": "Bonjour",
  "last_message_at": "2026-05-24T13:58:02.000000Z",
  "unread_count": 1,
  "other_user": {
    "id": "uuid",
    "display_name": "Autre utilisateur",
    "photo_url": null,
    "last_seen_at": "2026-05-24T13:58:02.000000Z"
  },
  "created_at": "2026-05-24T13:58:02.000000Z"
}
```

### 10.2 Lister les conversations

Route :

```txt
GET /conversations
```

Comportement :

- uniquement les conversations du user courant
- triees par `last_message_at desc`

### 10.3 Lire le detail d'une conversation

Route :

```txt
GET /conversations/{id}
```

Erreur :

- `403` si le user n'est pas participant

### 10.4 Creer une conversation

Route :

```txt
POST /conversations
```

Body :

```json
{
  "listing_id": "uuid",
  "message": "Bonjour, est-ce toujours disponible ?"
}
```

Regles metier :

- annonce doit etre `active`
- l'acheteur ne peut pas etre le proprietaire de l'annonce
- `conv_id` est deterministe et evite les doublons

Succes :

- `201`
- retourne :
  - `data` = `ConversationResource`
  - `message` = `MessageResource`

### 10.5 Message object

Format reel :

```json
{
  "id": "uuid",
  "conv_id": "conv-id",
  "sender_id": "uuid",
  "type": "text",
  "text": "Bonjour",
  "image_url": null,
  "proposal": null,
  "is_read": false,
  "created_at": "2026-05-24T13:58:02.000000Z"
}
```

Cas `troc_proposal` :

```json
{
  "proposal": {
    "offered_listing_id": "uuid",
    "offered_listing_title": "Titre",
    "offered_listing_photo": "/storage/photos/xxx.webp",
    "cash_amount": 10000,
    "status": "pending",
    "refusal_reason": null
  }
}
```

### 10.6 Lister les messages

Route :

```txt
GET /conversations/{id}/messages
```

Effet metier :

- marque la conversation comme lue pour le user courant

### 10.7 Envoyer un message

Route :

```txt
POST /conversations/{id}/messages
```

Body selon le type.

#### Message texte

```json
{
  "type": "text",
  "text": "Bonjour"
}
```

#### Message image

Format :

- `multipart/form-data`

Champs :

- `type=image`
- `image=@...`

#### Proposition de troc

```json
{
  "type": "troc_proposal",
  "offered_listing_id": "uuid",
  "cash_amount": 10000
}
```

Regles metier :

- l'annonce proposee doit appartenir a l'expediteur

### 10.8 Marquer comme lu

Routes :

- `POST /conversations/{id}/read`
- `PUT /conversations/{id}/read`

Reponse :

```json
{
  "message": "Messages marques comme lus."
}
```

## 11. Favoris

### 11.1 Favorite object

```json
{
  "id": "uuid",
  "user_id": "uuid",
  "listing_id": "uuid",
  "listing_title": "Titre",
  "listing_photo": "/storage/photos/xxx.webp",
  "listing_price": "150000",
  "listing_type": "VENTE",
  "created_at": "2026-05-24T13:58:02.000000Z",
  "listing": {
    "...": "ListingResource"
  }
}
```

### 11.2 Lister les favoris

Route :

```txt
GET /favorites
```

### 11.3 Ajouter un favori

Route :

```txt
POST /favorites/{listingId}
```

Comportement :

- idempotent en base via `firstOrCreate`
- renvoie quand meme `201`
- incremente `favorite_count` seulement si creation reelle
- declenche une push `new_favorite` au proprietaire

### 11.4 Supprimer un favori

Route :

```txt
DELETE /favorites/{listingId}
```

Effet metier :

- decremente `favorite_count` si possible

## 12. Transactions

### 12.1 Transaction object

```json
{
  "id": "uuid",
  "listing_id": "uuid",
  "seller_id": "uuid",
  "buyer_id": "uuid",
  "type": "VENTE",
  "final_price": 150000,
  "seller_reviewed": false,
  "buyer_reviewed": false,
  "created_at": "2026-05-24T13:58:02.000000Z"
}
```

### 12.2 Creer une transaction

Route :

```txt
POST /transactions
```

Body :

```json
{
  "listing_id": "uuid",
  "buyer_id": "uuid",
  "type": "VENTE",
  "final_price": 150000
}
```

Regles metier :

- seul le proprietaire de l'annonce peut creer la transaction
- l'annonce doit etre active
- `buyer_id` ne peut pas etre le vendeur lui-meme

Effets metier :

- `listing.status` devient `sold`
- notifications in-app creees
- push `transaction_done` au buyer

### 12.3 Lire une transaction

Route :

```txt
GET /transactions/{id}
```

Regles :

- seulement buyer ou seller

## 13. Reviews

### 13.1 Creer un avis

Route :

```txt
POST /reviews
```

Body :

```json
{
  "transaction_id": "uuid",
  "rating": 5,
  "comment": "Transaction parfaite"
}
```

Regles metier :

- seul un participant de la transaction peut noter
- un seul avis par auteur et par transaction

Erreur duplication :

- `409`
- body :

```json
{
  "message": "Avis deja existant."
}
```

Effets metier :

- recalcul `rating` du destinataire
- recalcul `review_count`
- mise a jour de `seller_reviewed` ou `buyer_reviewed`

### 13.2 Lire les avis recus d'un vendeur

Route :

```txt
GET /users/{uid}/reviews
```

## 14. Requests et Reports

### 14.1 Demandes publiques

Route :

```txt
GET /requests
```

Retourne seulement les demandes `active`.

### 14.2 Mes demandes

Route :

```txt
GET /my/requests
```

### 14.3 Creer une demande

Route :

```txt
POST /requests
```

Body :

```json
{
  "title": "Je cherche un iPhone",
  "description": "Budget serieux",
  "category": "electronique",
  "max_price": 200000,
  "country": "BJ",
  "city": "Cotonou"
}
```

Effets metier :

- `status = active`
- `expires_at = now + 30 jours`
- dispatch du job `CheckRequestMatches`

### 14.4 Supprimer une demande

Route :

```txt
DELETE /requests/{id}
```

Regles :

- proprietaire uniquement

### 14.5 Signaler un contenu

Route :

```txt
POST /reports
```

Body :

```json
{
  "target_type": "listing",
  "target_id": "uuid",
  "reason": "spam"
}
```

Valeurs supportees :

- `target_type`: `listing|user|message`
- `reason`: `spam|fake|inappropriate|scam`

## 15. Notifications in-app

### 15.1 Notification object

```json
{
  "id": "uuid",
  "type": "new_favorite",
  "title": "Nouveau favori",
  "body": "Quelqu un a ajoute votre annonce en favori.",
  "data": {
    "listing_id": "uuid"
  },
  "is_read": false,
  "created_at": "2026-05-24T13:58:02.000000Z"
}
```

### 15.2 Lister les notifications

Route :

```txt
GET /notifications
```

### 15.3 Marquer une notification comme lue

Route :

```txt
POST /notifications/{id}/read
```

### 15.4 Marquer tout comme lu

Route :

```txt
POST /notifications/read-all
```

Types observes en pratique :

- `new_message`
- `new_favorite`
- `transaction_done`

## 16. Push notifications FCM

Etat valide :

- token FCM sauvegarde via `/auth/fcm-token`
- notification directe Firebase v1 recue
- notification backend Laravel recue en reel

Exemple recu en foreground :

```json
{
  "from": "733271569706",
  "messageId": "uuid",
  "notification": {
    "title": "Nouveau favori",
    "body": "Quelqu un a ajoute votre annonce en favori."
  },
  "data": {
    "type": "new_favorite",
    "listing_id": "uuid"
  }
}
```

Strategie mobile :

1. recuperer token FCM device
2. login API
3. envoyer `/auth/fcm-token`
4. router la notification selon `data.type`

## 17. Web previews et deep-linking

Pages publiques :

- `/a/{listingId}`
- `/u/{username}`

Balises Open Graph validees :

- `og:title`
- `og:description`
- `og:image`

Utilisation mobile :

- partage social
- preview de lien
- fallback web si l'app n'est pas ouverte

App Links Android :

- `/.well-known/assetlinks.json`
- a completer avec les vraies infos Android en phase mobile native

## 18. User flows mobile recommandes

### 18.1 Onboarding

1. register ou login
2. fetch `/profile`
3. sauvegarder token
4. eventuellement upload avatar
5. envoyer token FCM
6. marquer `has_seen_onboarding = true`

### 18.2 Parcours acheteur

1. ouvrir feed `/listings`
2. utiliser `/search`
3. ouvrir `/listings/{id}`
4. ajouter en favori ou contacter vendeur
5. creer conversation
6. suivre messages
7. transaction finalisee par vendeur
8. laisser un avis via `/reviews`

### 18.3 Parcours vendeur

1. creer annonce via `/listings`
2. gerer ses annonces via `/my/listings`
3. recevoir favoris / messages / notifications
4. conclure transaction via `/transactions`
5. consulter reviews recues via `/users/{uid}/reviews`

### 18.4 Reset password flow

1. app demande email
2. `/auth/password/reset`
3. l'utilisateur ouvre le lien recu
4. ecran reset password
5. `/auth/password/update`
6. retour login

## 19. Recommandations d'implementation mobile

### 19.1 Cache local

Mettre en cache :

- profil courant
- dernier feed consulte
- derniers favoris
- conversations recentes

### 19.2 Retry

Retries limites pour :

- `/auth/fcm-token`
- uploads image
- fetch notifications

Pas de retry aveugle sur :

- `POST /transactions`
- `POST /reviews`

### 19.3 UX erreurs

- `401` => retour login
- `403` => snackbar / toast clair
- `404` => ressource retiree ou non publique
- `409` => action deja effectuee
- `422` => mapping field-by-field dans le formulaire
- `500` => message generique + retry manuel

### 19.4 Images

Conseille :

- compresser cote app avant upload
- limiter la resolution
- afficher placeholder si `photo_url` ou `photos` vide

## 20. MVP endpoints a integrer en premier

### Auth / profil

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/logout`
- `POST /auth/password/reset`
- `POST /auth/password/update`
- `GET /profile`
- `PUT /profile`
- `POST /profile/avatar`
- `POST /auth/fcm-token`

### Feed / listings

- `GET /listings`
- `GET /search`
- `GET /listings/{id}`
- `POST /listings`
- `PUT /listings/{id}`
- `GET /my/listings`
- `POST /listings/{id}/photos`

### Social

- `GET /conversations`
- `GET /conversations/{id}`
- `POST /conversations`
- `GET /conversations/{id}/messages`
- `POST /conversations/{id}/messages`
- `PUT /conversations/{id}/read`
- `GET /favorites`
- `POST /favorites/{listingId}`
- `DELETE /favorites/{listingId}`
- `POST /transactions`
- `POST /reviews`
- `GET /notifications`

## 21. References

- doc backend generale : [API.md](./API.md)
- scenarios de test reels : [API_TEST_SCENARIOS.md](./API_TEST_SCENARIOS.md)
- suivi projet : [PROGRESS.md](./PROGRESS.md)

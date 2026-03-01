# API VOD Payment

API Spring Boot pour la gestion des paiements d'un service VOD.

## Architecture

Le projet suit une architecture en couches :
- `Controller` : exposition HTTP des endpoints
- `Service` : logique metier
- `Repository` : acces aux donnees via Spring Data JPA
- `Entity` / `DTO` / `Mapper` : modeles de persistance, echange API et conversion

## Endpoints Payments

Base URL locale : `http://localhost:8080`

1. `GET /payments`
2. `GET /payments/{id}`
3. `POST /payments`
4. `DELETE /payments/{id}`

## Modele Payment

Champs exposes par l'API :
- `id` : `long`
- `type` : enum `CARTE | WALLET`
- `statut` : enum `EN_ATTENTE | EFFECTUE | ECHOUE`
- `montant` : `integer` (>= 0)
- `walletProvider` : enum `PAYPAL | APPLE_PAY | GOOGLE_PAY` (obligatoire si `type=WALLET`)
- `cardOwner` : `string` (obligatoire si `type=CARTE`)
- `cardNumber` : `string` (16 chiffres, obligatoire si `type=CARTE`)
- `cardExpiryDate` : `string` format `MM/YY` (obligatoire si `type=CARTE`)
- `cvv` : `string` (3 chiffres, obligatoire si `type=CARTE`)

## Regles metier paiement

- Si `type=CARTE` : `cardOwner`, `cardNumber`, `cardExpiryDate`, `cvv` sont obligatoires et `walletProvider` doit etre `null`.
- Si `type=WALLET` : `walletProvider` est obligatoire et les champs carte doivent etre vides.

## Exemples de requetes

Creer un paiement CARTE :

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CARTE",
    "statut": "EN_ATTENTE",
    "montant": 15,
    "cardOwner": "Jean Dupont",
    "cardNumber": "4970100000000000",
    "cardExpiryDate": "12/30",
    "cvv": "123"
  }'
```

Creer un paiement WALLET :

```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{
    "type": "WALLET",
    "statut": "EN_ATTENTE",
    "montant": 20,
    "walletProvider": "PAYPAL"
  }'
```

## Documentation OpenAPI

- Schema : `src/main/resources/openapi.yaml`
- Swagger UI : `http://localhost:8080/swagger-ui.html`
- JSON docs : `http://localhost:8080/api-docs`

## Tests

Les tests HTTP sont dans :
- `src/test/java/com/controllers/PaymentControllerTest.java`

Ils couvrent chaque requete avec cas nominaux et cas particuliers :
- `GET /payments` : liste avec resultats et liste vide
- `GET /payments/{id}` : paiement trouve, paiement inexistant (404)
- `POST /payments` :
  - paiement CARTE valide (201)
  - paiement WALLET valide (201)
  - id fourni dans le payload (201)
  - type manquant (400)
  - statut manquant (400)
  - montant manquant (400)
  - montant negatif (400)
  - CARTE sans `cvv` (400)
  - CARTE sans `cardOwner` (400)
  - CARTE sans `cardNumber` (400)
  - CARTE avec `cardNumber` invalide (400)
  - CARTE sans `cardExpiryDate` (400)
  - CARTE avec `cardExpiryDate` invalide (400)
  - CARTE avec `cvv` invalide (400)
  - WALLET sans `walletProvider` (400)
  - WALLET avec `walletProvider` invalide (400)
  - CARTE avec `walletProvider` (400)
  - enum invalide (400)
- `DELETE /payments/{id}` : suppression OK, paiement inexistant (404)

Les tests metier (service) sont dans :
- `src/test/java/com/services/impl/PaymentServiceImplTest.java`

Ils couvrent :
- persistance d'un paiement CARTE valide
- rejet d'un paiement CARTE invalide (CVV manquant)
- rejet d'un paiement WALLET invalide (walletProvider manquant)
- `getPayment` sur id inexistant (EntityNotFoundException)
- `deletePayment` sur id inexistant (EntityNotFoundException)

Execution (si Gradle est disponible) :

```bash
./gradlew test
```

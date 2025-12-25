# 💈 BarberShop API — Documentation Technique (V1)

**Description**

Backend RESTful pour l'application mobile de réservation de coiffeurs/barbiers. Il gère l'authentification, la géolocalisation, la prise de rendez-vous, les avis et les notifications push.

---

## 🛠️ Stack technologique

- **Langage** : Java 17
- **Framework** : Spring Boot 3.x
- **Base de données** : PostgreSQL (+ PostGIS)
- **Sécurité** : Spring Security + JWT
- **Messaging** : Apache Kafka (Transactional Outbox)
- **Notifications** : Firebase Cloud Messaging (FCM)
- **Documentation API** : OpenAPI / Swagger

---

## 🚀 Installation & démarrage

### 1) Prérequis

- Java JDK 17+
- Maven 3.8+
- PostgreSQL 14+
- Kafka & Zookeeper (ou via Docker Compose)

### 2) Créer la base de données

```sql
CREATE DATABASE barbershop_db;
```

### 3) Variables d'environnement / Configuration

Configurez `application.properties` ou les variables d'environnement :

| Variable                    |                  Description | Exemple                                          |
| --------------------------- | ---------------------------: | ------------------------------------------------ |
| SPRING_DATASOURCE_URL       |            URL JDBC Postgres | `jdbc:postgresql://localhost:5432/barbershop_db` |
| SPRING_DATASOURCE_USERNAME  |               Utilisateur DB | `postgres`                                       |
| SPRING_DATASOURCE_PASSWORD  |              Mot de passe DB | `admin`                                          |
| APP_JWT_SECRET              | Clé secrète JWT (>=32 chars) | `MaSuperCleSecreteDePlusDe32Caracteres...`       |
| APP_KAFKA_BOOTSTRAP_SERVERS |              Kafka bootstrap | `localhost:9092`                                 |
| FIREBASE_CONFIG_PATH        | Chemin fichier JSON Firebase | `classpath:firebase-service-account.json`        |

> ⚠️ **Note** : conservez `APP_JWT_SECRET` en sécurité (secrets manager / variables d'environnement CI).

### 4) Lancer l'application

```bash
mvn spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 🏛️ Architecture & Patterns clés

### 1) Cycle de vie d'une réservation (Booking Flow)

- **PENDING** : Créé par le client (POST `/v1/bookings`) — paiement en attente.
- **CONFIRMED** : Accepté par le barbier (PATCH `/v1/bookings/{id}/confirm`) — notification envoyée.
- **COMPLETED** : Prestation terminée (PATCH `/v1/bookings/{id}/complete`) — paiement marqué PAID.
- **CANCELLED** : Annulé par client ou barbier.

### 2) Transactional Outbox (notifications fiables)

- Sauvegarder l'entité (ex. Booking) et l'événement dans `outbox_events` dans une même transaction SQL.
- Un scheduler/connector lit `outbox_events` et publie dans Kafka.
- Un consumer Kafka envoie le push via Firebase.

> ✅ Permet d'éviter la perte d'événements en cas d'échecs externes.

### 3) "Super Endpoint" (optimisation mobile)

- **GET** `/v1/barbers/{id}/profile` — renvoie : infos barbier, services, portfolio (photos), avis récents, statut (ouvert/fermé), favoris.
- Réduit le nombre d'appels réseau côté mobile.

---

## 📡 Endpoints importants (résumé)

### 🔐 Authentification (`/api/auth`)

- `POST /login` — connexion (retourne JWT)
- `POST /register/client` — inscription client
- `POST /register/barber` — inscription barbier

### 📍 Barbiers & recherche (`/v1/barbers`)

- `GET /nearby?lat=...&lon=...&radius=10` — barbiers proches
- `GET /{id}/profile` — super endpoint
- `GET /{id}/availability?date=YYYY-MM-DD` — créneaux disponibles

### 📅 Réservations (`/v1/bookings`)

- `POST /` — créer un RDV
  - Body ex.:
    ```json
    { "barberId": 1, "serviceId": 2, "startAt": "2025-12-25T14:00:00" }
    ```
- `GET /my-bookings` — mes RDV (client)
- `PATCH /{id}/confirm` — confirmer (barbier)
- `PATCH /{id}/complete` — terminer (barbier)
- `PATCH /{id}/cancel` — annuler

### 🔔 Notifications (`/v1/notifications`)

- `PUT /token` — enregistrer token FCM du device (à l'initialisation de l'app)

### ⭐ Avis (`/v1/reviews`)

- `POST /` — laisser un avis (seulement si booking `COMPLETED`)
- `GET /barber/{id}` — lire les avis

### 📊 Dashboard (`/v1/dashboard`)

- `GET /stats` — stats (CA, nombre de RDV, etc.)

---

## 💾 Modèle de données (simplifié)

- **Users** : compte global (email, password, roles)
- **Clients / Barbers** : profils liés à `User`
- **Bookings** : lié à Client, Barber, Service — contient `status`, `paymentMethod`
- **Reviews** : lié à Booking — `rating`, `comment`, `photos`(JSON)
- **Availabilities** : planning (horaires récurrents + exceptions)

---

## 🧪 Tests & validation

Flux de test manuel (Postman) :

1. Créer compte barbier & service
2. Créer disponibilités (compte barbier)
3. Créer compte client, récupérer token JWT
4. `GET /v1/barbers/nearby`
5. `POST /v1/bookings` (client)
6. Vérifier logs (`Notification envoyée à Kafka...`)
7. `PATCH /v1/bookings/{id}/confirm` (barbier)

---

## 📞 Contact & support

Développé par **SalahTech**.

En cas de bug : vérifier les logs console et la table `outbox_events` pour les problèmes de notifications.

---

## Licence

Distribué sans licence explicite — ajoutez une licence si nécessaire.

# 📱 BetSocial

A social application where users create, share and participate in prediction-based
challenges with friends and the wider community. It combines social networking,
real-time messaging and a community prediction system.

The focus is social interaction and engagement, not real-money gambling.

---

## Repository layout

This is a single repository containing both halves of the application:

```
.
├── app/          React Native client (screens, components, API layer)
├── android/      Android native project
├── ios/          iOS native project
└── BetSocial/    Spring Boot backend (Maven project)
```

| Layer | Technology |
|---|---|
| Mobile client | React Native 0.76, React 18, TypeScript |
| Backend | Java 21, Spring Boot 3.4, Spring Data JDBC, Spring Security |
| Database | PostgreSQL (schema managed by Flyway) |
| Realtime | STOMP over WebSocket |
| Push / auth | Firebase (Admin SDK server-side, React Native Firebase client-side) |

---

## Prerequisites

- **JDK 21** — the backend targets Java 21 (LTS). Check with `java -version`.
- **PostgreSQL 14+** running locally.
- **Node.js 18+** and npm.
- React Native environment set up for your platform — follow the
  [React Native environment setup](https://reactnative.dev/docs/set-up-your-environment)
  guide through "Creating a new application".

Maven does **not** need to be installed; the repo ships the Maven wrapper (`./mvnw`).

---

## Backend setup

### 1. Configure the environment

```bash
cd BetSocial
cp .env.example .env
```

Fill in `.env`. At minimum you need `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.
`MAIL_USERNAME` / `MAIL_PASSWORD` are only needed for account-verification emails.

These are read by `application.properties` as environment variables. Supply them
via your shell, an `.env` loader, or IntelliJ's *Run Configuration → Environment
variables* field.

### 2. Provide Firebase credentials

Place the service-account JSON at `BetSocial/src/main/resources/firebaseAPI.json`.
This file is gitignored and must never be committed. To load it from elsewhere,
set `FIREBASE_CREDENTIALS=file:/path/to/firebaseAPI.json`.

### 3. Create the database

Create an empty database matching your `DB_URL`. Flyway builds the schema on
first startup — there is no manual SQL step.

```sql
CREATE DATABASE betsocial;
```

### 4. Run

```bash
cd BetSocial
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`. On first run against an empty
database, `Startup` seeds eleven development users (`admin`, `john`, `jane`, …),
all with the password `password`.

To build a jar instead:

```bash
cd BetSocial
./mvnw clean package
java -jar target/World-0.0.1-SNAPSHOT.jar
```

### Database migrations

The schema lives in `BetSocial/src/main/resources/db/migration` and is applied by
Flyway at startup. To change it, add a new `V<n>__description.sql` file — never
edit an already-applied migration.

An existing database that already contains the tables is adopted automatically
(`spring.flyway.baseline-on-migrate=true`) and marked as being at V1.

---

## Frontend setup

### 1. Install dependencies

```bash
npm install
```

### 2. Point the app at your backend

The API base URL is `IP_STRING` in `app/Constants.js`. A device or emulator
cannot reach `localhost` on your machine, so set it to your machine's LAN IP:

```js
export const IP_STRING = "http://192.168.1.53:8080";
```

### 3. Start Metro, then the app

```bash
npm start
```

In a second terminal:

```bash
npm run android
```

or

```bash
npm run ios
```

---

## Tests

```bash
cd BetSocial
./mvnw test
```

```bash
npm test
```

Backend coverage is currently minimal — `contextLoads` only — and the test
requires a reachable database.

---

## Troubleshooting

**`release version 23 not supported`** — you are on an older JDK than the build
expects, or your IDE is overriding the project SDK. The build targets Java 21.

**`FATAL: password authentication failed`** — `DB_PASSWORD` is unset or wrong.
Environment variables set in a shell do not reach an IDE run configuration; set
them in both places.

**`Unable to obtain connection from database`** — PostgreSQL is not running, or
`DB_URL` names a database that does not exist yet.

**Flyway reports a checksum mismatch** — an already-applied migration was edited.
Revert the edit and add a new versioned migration instead.

**Metro cannot connect / network request failed** — `IP_STRING` still points at
`localhost`, or your phone is on a different network than your machine.

---

## Author

Created by Amara Okonkwo.

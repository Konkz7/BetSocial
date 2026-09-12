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
database, `Startup` seeds ten development users (`john`, `jane`, …), all with
the password `password`.

The `admin` account is seeded separately, on **every** start rather than only
against an empty database, so there is always a privileged login to test with.
It has `user_role = 2` (ADMIN) and the same password `password`, and signing in
with it lands on the admin approval queue instead of the social feed. These are
development seeds; anything deployed publicly needs them changed.

### Sample data for testing pagination

Eleven users and a few threads never cross a page boundary, so pagination cannot
be seen by hand on a fresh database. `SAMPLE_DATA` tops the database up to 60
accounts, 75 threads and one conversation of 120 messages:

```bash
cd BetSocial
SAMPLE_DATA=true ./mvnw spring-boot:run
```

It is off by default, tops up to those targets rather than appending on every
restart, and uses a fixed random seed so the content is the same each time. The
generated accounts have a deliberately unusable password — they exist to be
listed, searched and paged through, not signed in to.

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

### 2. Provide the Firebase client config

`app/Secrets.js` is gitignored. Copy the template and fill in the values from
the Firebase console (*Project settings → General → Your apps*):

```bash
cp app/Secrets.example.js app/Secrets.js
```

The `firebaseConfig` export name must stay as-is — `app/Constants.js` imports it
by name, and a missing or unexported value makes every screen fail with
`app/no-options` followed by `Cannot read property 'IP_STRING' of undefined`.

### 3. Point the app at your backend

The API base URL is `IP_STRING` in `app/Constants.js`. A device or emulator
cannot reach `localhost` on your machine, so set it to your machine's LAN IP:

```js
export const IP_STRING = "http://192.168.1.53:8080";
```

### 4. Start Metro, then the app

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

The backend suite is a set of integration tests that run against a real
PostgreSQL container via Testcontainers, so **Docker must be running** — there
is no other setup, and no need for a local database.

| Suite | Covers |
|---|---|
| `MigrationTest` | Flyway migrations apply to an empty database; tables, unique constraints and foreign keys are all present |
| `SecurityRegressionTest` | authorization rules, response projections, removed endpoints, conversation membership, empty-collection responses |
| `PredictionRepositoryTest` | removing a prediction soft-deletes the prediction, not a bet |
| `ConversationListTest` | a conversation with no messages does not break the list; each row is paired with its own membership |
| `NotificationDedupeTest` | duplicate notifications collapse instead of failing the lookup |
| `ThreadProfileViewerTest` | the liked flag reflects the viewer, not the thread's author |
| `FeedVisibilityTest` | private threads need a mutual follow; authors always see their own; liked flag and comment count are per viewer |
| `FeedQueryCountTest` | the feed's query count does not grow with the number of threads |
| `AdminSeedingTest` | an admin account is always seeded, including into a database that already has users, and never duplicated |

A container is started once and shared across the suite; the first run pulls
`postgres:17-alpine`, so expect it to take a little longer.

CI runs the same command on every pull request touching `BetSocial/` — see
`.github/workflows/backend-tests.yml`.

---

## Media

Photos and videos go **straight from the device to Firebase Storage**. The bytes
never pass through this server — only the resulting URL does, which is stored in
`thread_.media`, or in a media message's `description`.

```
images/<uuid>.jpg          thread and comment images
videos/<uuid>.mp4          thread and message videos
profile_pictures/<uid>.jpg profile pictures, one per account, overwritten
```

### There are two bucket settings, and they are not the same one

| Setting | Side | Does what |
|---|---|---|
| `storageBucket` in `app/Secrets.js` | client | **where uploads go.** Without it, `getStorage()` has no bucket and every upload fails with `storage/no-default-bucket` — the app shows *"Apologies! Image couldnt be Uploaded"* |
| `FIREBASE_STORAGE_BUCKET` | server | **which files are recognised as ours**, for validating references and deleting them later |

Setting the server one does not fix an upload, and setting the client one does
not make the server check anything. Both need the same bucket.

The authoritative value is in the Firebase console under *Storage* — the
`gs://…` name at the top of the Files tab. Copy the part after `gs://`.

```bash
FIREBASE_STORAGE_BUCKET=your-project.firebasestorage.app ./mvnw spring-boot:run
```

Bucket names come in two shapes: projects made before late 2024 have
`<project>.appspot.com`, newer ones `<project>.firebasestorage.app`. Only one
exists for any given project, so the server accepts either spelling of the name
it is given rather than making the choice matter.

With it set, the server refuses any media URL that does not name an object in
that bucket under one of those prefixes, and deletes the object when its thread
or message is removed. **Without it, neither happens** — references are stored
unchecked and files are never cleaned up. The application warns at startup when
it is unset, and logs the bucket it expected whenever it refuses a reference.

### The limits are not enforced here

Because uploads bypass the server, **nothing in this codebase can enforce a file
size or type.** `FBStorageService` compresses above 3MB and refuses above 15MB
for images (50MB / 100MB for video), but that is the app being polite to itself —
anything talking to Firebase directly ignores it.

The only place those limits can actually be enforced is the **Storage rules on
the bucket**, in the Firebase console. Something like:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /images/{file} {
      allow read;
      allow write: if request.auth != null
                   && request.resource.size < 15 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
    match /videos/{file} {
      allow read;
      allow write: if request.auth != null
                   && request.resource.size < 100 * 1024 * 1024
                   && request.resource.contentType.matches('video/.*');
    }
    match /profile_pictures/{file} {
      allow read;
      allow write: if request.auth != null
                   && request.resource.size < 15 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
  }
}
```

**Check what the rules currently are before going public.** A bucket left on the
default test rules is writable by anyone who has the project's client config,
which ships inside the app.

## Logging

Every line carries the request it belongs to and who was making it:

```
INFO [3f9a1c22/7] c.e.World.Bets.BetService : Closing 2 expired bets
```

The bracketed pair is `requestId/userId`, put there by `LogContextFilter`. Empty
brackets mean there was no request in progress — a scheduled sweep, or startup.
The same id comes back on every response as `X-Request-Id`, so a report of "it
failed around four o'clock" can be turned into one exact request.

The class is not called `RequestContextFilter`, which is the obvious name: Spring
Boot already registers a bean by that name, bean overriding is off by default,
and the collision stops the application starting.

Levels are `INFO` by default. To chase an authorisation problem:

```bash
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG ./mvnw spring-boot:run
```

That was previously the default, and it wrote the entire security context on
every request — which buried everything worth reading.

For anything that ships logs to an aggregator, `LOG_FORMAT` switches them to
JSON (`ecs`, `logstash` or `gelf`):

```bash
LOG_FORMAT=ecs ./mvnw spring-boot:run
```

Off by default, because a person reading a terminal wants the readable form.

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

**`app/no-options` and/or `Cannot read property 'IP_STRING' of undefined`** —
you have not created `app/Secrets.js`, or it declares `firebaseConfig` without
exporting it. These two errors always appear together: `initializeApp` throws at
module scope, which aborts `Constants.js` evaluation, so every module importing
`IP_STRING` from it sees `undefined`. Fix the export and both clear.

**"Apologies! Image couldnt be Uploaded"** — `app/Secrets.js` has no
`storageBucket`, so the Firebase client has no bucket to upload to
(`storage/no-default-bucket`). Copy it from the Firebase console and restart
Metro. The server's `FIREBASE_STORAGE_BUCKET` is a different setting and does
not affect uploading — see [Media](#media).

**Media uploads succeed but posting fails with "That media reference is not one
of ours"** — `FIREBASE_STORAGE_BUCKET` names a different bucket than the client
uploads to. The server log line next to the refusal says which one it expected.

**`Could not find a valid Docker environment` when running the backend tests** —
Docker is not running, or its Engine API is newer than the Testcontainers
version can negotiate. Docker Engine 29 dropped API versions below 1.44; the
`testcontainers.version` property in `BetSocial/pom.xml` overrides the Spring
Boot default for exactly this reason. Check `docker version` and raise that
property if the daemon's minimum API has moved again.

**Metro cannot connect / network request failed** — `IP_STRING` still points at
`localhost`, or your phone is on a different network than your machine.

---

## Author

Created by Amara Okonkwo.

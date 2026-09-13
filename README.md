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
with it lands on the admin approval queue instead of the social feed.

All of that is development only. `DEV_SEED=false` turns it off, and the `prod`
profile sets it — a deployed server creates an admin only if `ADMIN_PASSWORD` is
given, and no demo accounts at all. Startup warns while the defaults are in use,
so a server that still has them says so in its own log.

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
(`spring.flyway.baseline-on-migrate=true`) and marked as being at V1. The `prod`
profile turns that off: adopting an unknown schema as V1 is a convenience for a
development database and a way to silently skip migrations anywhere else.

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
| `ProductionSeedingTest` | with seeding off and no `ADMIN_PASSWORD`, nothing is created — no demo accounts, no default admin |
| `HealthCheckTest` | `/health` answers without a session and says nothing beyond reachability |

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
the bucket**, in the Firebase console:

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
      // Only the owner. The file is named after the uid, and the token says
      // which uid the caller is, so one cannot overwrite another's picture.
      allow write: if request.auth != null
                   && file == request.auth.uid + '.jpg'
                   && request.resource.size < 15 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
  }
}
```

### How an upload gets an identity

Signing in to this app is a session against our own database — Firebase is not
part of it, and for a long time that meant every upload reached the bucket
anonymous. `request.auth` was null, so the rules above could not have been
written: anyone holding the client config, which ships inside the app, could
write to the bucket.

There are also **two Firebase SDKs** in the client, and they do not share a
signed-in user:

| SDK | Used for | Signed in by |
|---|---|---|
| `@react-native-firebase/*` | phone OTP at registration, push messaging | the OTP flow |
| `firebase` (JS SDK) | **Storage uploads** | `FBAuthService` |

So `FBAuthService` gives the JS SDK instance an identity of its own:

```
POST /api/media/token   →  a custom token, signed with the service-account key
                           the backend already holds, asserting "this is user 7"
signInWithCustomToken   →  a Firebase session whose uid is 7
```

The uid is our own user id, which is why `profile_pictures/7.jpg` can be pinned
to its owner without a second mapping to keep in step. The token is minted from
the **session** and never from anything in the request — issuing one for a uid a
caller supplied would let anybody upload as anybody, and the rules would then be
enforcing an identity the caller chose.

The Firebase session is held in memory and dropped on logout, so it cannot
outlive the session it came from. If it expires mid-upload, `withUploadIdentity`
re-mints once and retries.

**Check what the rules currently are before going public.** A bucket left on the
default test rules is writable by anyone who has the project's client config,
which ships inside the app.

## Deploying

The backend ships as a container. `deploy/docker-compose.yml` runs the whole
server — database, application, and Caddy in front for TLS — on one machine.

It is written for an **Oracle Cloud Always Free** instance, which costs nothing
permanently and is generous enough (4 ARM cores, 24GB) that this uses a fraction
of it. Nothing in the compose file is Oracle-specific; it is three containers on
one box and would run anywhere Docker does.

### What you need first

**A hostname pointing at the machine.** Not optional: a certificate cannot be
issued for a bare IP, and without HTTPS the application authenticates nobody —
the session cookie is `Secure` in the prod profile, and that same cookie carries
the WebSocket handshake. A free [DuckDNS](https://duckdns.org) subdomain is
enough.

**Ports 80 and 443 open, in both places.** Oracle instances have a cloud
firewall *and* iptables on the instance itself, and the instance one is closed by
default. Missing the second is the usual reason a new Oracle box looks dead:

```bash
# on the instance
sudo iptables -I INPUT -p tcp --dport 80  -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

and add the same two rules to the subnet's Security List in the Oracle console.

### Setting it up

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER && newgrp docker

git clone https://github.com/Konkz7/BetSocial.git
cd BetSocial/deploy
cp .env.example .env
```

Fill in `.env` — every value is a credential except `DOMAIN`, and compose
refuses to start rather than defaulting any of them. The Firebase key goes in as
one line:

```bash
# with firebaseAPI.json copied onto the server
FIREBASE_CREDENTIALS_JSON=$(jq -c . firebaseAPI.json)
```

Then:

```bash
docker compose up -d --build
```

**Build on the machine that runs it.** Always Free instances are Ampere ARM; an
image built on an x86 laptop will not start on one. The first build takes a few
minutes — it downloads the full Maven dependency tree.

Caddy gets a certificate on first start. Watch it happen:

```bash
docker compose logs -f caddy
```

### Checking it

```bash
curl https://<your-domain>/health      # {"status":"ok"}
```

That endpoint runs `SELECT 1`, so it answers only when the application can
actually reach the database.

### Backups

Running Postgres yourself is what makes this free, and backups are the part a
managed database would have been doing. `deploy/backup.sh` dumps and rotates;
put it in cron:

```bash
0 * * * * /home/ubuntu/BetSocial/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
```

The volume survives `docker compose down`. It does not survive `down -v`, a
deleted instance, or a mistaken `DELETE`.

### Updating

```bash
git pull && docker compose up -d --build
```

Flyway applies any new migrations at startup. Take a backup first — that is the
moment one is most worth having.

### Running it as a plain jar instead

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/World-0.0.1-SNAPSHOT.jar
```

Without that profile the server runs its development defaults, and the first of
those is an account called `admin` with the password `password`. The profile
turns off seeding, tightens the session cookie, stops exception messages
reaching clients, and switches the log to JSON — see
`application-prod.properties`, which says why for each. The Dockerfile sets the
profile itself, so an image cannot be started without it by accident.

### What the host polls

`GET /health` is the only unauthenticated endpoint besides registration. It runs
`SELECT 1`, so an instance that cannot reach the database reports 503 and stops
being sent traffic rather than serving errors — which also means a rolling
deploy cannot replace working instances with broken ones and call it a success.
It returns `{"status":"ok"}` and nothing else; it is the one endpoint anybody on
the internet can reach.

### The environment it needs

| Variable | |
|---|---|
| `DB_URL` `DB_USERNAME` `DB_PASSWORD` | the database |
| `ADMIN_PASSWORD` | creates the admin account with this password. **Without it no admin is created at all** — deliberately, because the alternative is a guessable one |
| `SPRING_PROFILES_ACTIVE` | `prod`. Set by the Dockerfile, so a container cannot start without it by accident |
| `FIREBASE_CREDENTIALS_JSON` | the service-account key as a value, for a container with no file. Or `FIREBASE_CREDENTIALS=file:/path/...` where there is one |
| `FIREBASE_STORAGE_BUCKET` | or media references are stored unchecked and files are never deleted |
| `MAIL_USERNAME` `MAIL_PASSWORD` | verification and password-reset email |
| `APP_BASE_URL` | the public URL, used in password-reset links |

### Before the first deploy

**Rotate everything that has been in the repository.** `application.properties`
was committed with literal values on 2026-09-05 (`8ebe730`) and git keeps them
whatever the current file says:

- the database password, username and URL
- the mail password — a Google app password
- `spring.security.user.password`

Rotating is the fix. Rewriting history is not, on its own: clones and forks keep
what they already have.

The Firebase service-account key has already been rotated; if it is ever
rotated again, replace `firebaseAPI.json` **and restart**, or the server keeps
signing with a key Google no longer accepts and every upload identity, push and
verification email fails at once.

### HTTPS is not optional here

The session cookie authenticates both the API and the WebSocket handshake, so
over plain HTTP it is readable by anyone on the same network, and a phone on
public wifi hands out a login by using the app.

The prod profile sets `server.servlet.session.cookie.secure=true`, which means
the cookie is **only** sent over HTTPS — so with TLS not yet in front of it,
nothing will authenticate at all. That is the intended failure: it breaks
loudly rather than shipping sessions in clear.

`server.forward-headers-strategy=framework` is set for TLS terminating at a
reverse proxy, which is the usual arrangement.

The client must move with it: `IP_STRING` in `app/Constants.js` becomes
`https://`, and `WebSocketService` derives `wss://` from it.

## How the chat socket authenticates

`/ws` requires an authenticated session — WebSocket identity comes from the
Spring principal, not from a client header, so a client cannot claim to be
somebody else. The handshake proves who it is in one of two ways:

```
1. the JSESSIONID cookie          — what a browser does, and what this expected
2. ?ticket=<one-time token>       — when the cookie does not arrive
```

React Native's Android `WebSocketModule` *does* attach cookies to a `ws://`
handshake (`getCookie` reads the same store the HTTP stack writes to, mapping
`ws://` to `http://` so the domain matches), and iOS was never verified at all.
That was enough to build on and not enough to rely on: in practice the cookie
does not always arrive, and the failure is a refused handshake with nothing to
say why.

So the app asks for a ticket over HTTP — where the session demonstrably works —
and puts it in the socket URL:

```
POST /api/ws/ticket    (session)  → a ticket, good for 30 seconds, once
ws://host/ws?ticket=…             → HandshakeTicketFilter turns it into the
                                    same authentication a cookie would have
```

The cookie is still tried first. `HandshakeTicketFilter` only looks at the
ticket when the request reached it unauthenticated, so a working cookie costs
nothing and leaves the ticket unspent.

A credential in a query string is a thing to keep small: the ticket is 256 bits
of `SecureRandom`, single-use, dead in 30 seconds, stored only as a hash, and the
filter is scoped to `/ws` so it authenticates nothing else. Consuming it goes
back through `UserDetailsService`, which is where a deleted account is refused
and where current roles come from.

The client fetches one in stompjs's `beforeConnect`, which runs before *every*
attempt — a ticket is spent by the handshake that uses it, so a reconnect five
seconds later needs its own.

## Downloading your data

*Settings → Download My Data* saves a JSON file with everything held about the
account. It does not come from the app.

```
POST /api/users/my-data/link       (session)  → a one-time token
GET  /api/users/my-data/download   (token)    → the file
```

The app cannot write the file itself. From Android 10 the public Downloads
folder is closed to ordinary file writes, and React Native's `Share` takes a
string rather than a file on Android — so anything the app saved would land
somewhere its owner could not get at. The phone's browser can do both, so the
app hands it a link and the browser's own download handling does the saving.

The browser is not the app and carries no session cookie, so the URL has to
carry its own permission. That is worth being careful about — a URL reaches
browser history, and this one returns an email address, a phone number and
private messages. So the token is 256 bits of `SecureRandom`, **usable once**,
and **dead after two minutes**. Only its hash is stored, and it is held in
memory rather than in a table, which assumes a single server: with two, the
browser could be handed to the one that did not issue it. `DownloadTokens` is
where that gets fixed if it happens.

`GET /api/users/my-data` still exists and still needs the session — the
`permitAll` rule names the download path and the GET method exactly, because a
wildcard there would have opened it too.

## What a failed request says back

There are two kinds of message and they were being treated as one.

A reason on a `ResponseStatusException` was written by whoever threw it, for the
person who will read it — *"You cannot stake more than you have"* — and the app
shows it. A message on an arbitrary exception was written by a library, about
itself, and on a 500 it is usually a driver naming the host, the database and
the user.

`server.error.include-message` decides between them with one switch, so the
choice was to leak the second or lose the first. `ApiErrorHandler` makes the
distinction instead: written reasons pass through, everything else becomes
`Something went wrong` and goes to the log with the request id that produced it.

```json
{"status": 429, "error": "Too Many Requests", "message": "..."}
{"status": 500, "error": "Internal Server Error", "message": "Something went wrong", "requestId": "3f9a1c22"}
```

`message` is the field the client reads (`error.response?.data?.message`), so
renaming it would turn every error in the app into *"Request failed with status
code 400"*.

It extends `ResponseEntityExceptionHandler` rather than only catching
`Exception`. That base class already maps every standard Spring MVC failure — a
wrong method, malformed JSON, a missing parameter — to its proper status, and
catching `Exception` alone would turn all of them into 500s.

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

**"Apologies! Image couldnt be Uploaded"** / `storage/no-default-bucket` —
`app/Secrets.js` has no `storageBucket`, so the Firebase client has no bucket to
upload to. Copy it from the Firebase console and restart Metro. The server's
`FIREBASE_STORAGE_BUCKET` is a different setting and does not affect uploading —
see [Media](#media).

The other way to get this error is to have put the **service-account JSON** in
`app/Secrets.js`. Both come from the Firebase console and both look like "the
Firebase credentials", but the service account is a private key with full access
to the project, it is server-side only, and `app/Secrets.js` is bundled into the
app — so it would ship to every device. Its bucket is also spelled
`storage_bucket` and carries a `gs://` prefix, which is why storage reports no
bucket at all. `FBStorageService` logs which of the two happened. If it has been
built or shared, rotate the key in *Project settings → Service accounts*.

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

**`error listen EADDRINUSE :::8081`** — a Metro from an earlier run is still
serving, and the app is talking to *that* one. Until it is stopped, the bundle
on the device is the old one no matter what you rebuild.

**`npm start --reset-cache` does nothing** — npm keeps the flag for itself and
says `Unknown cli config "--reset-cache"`. The arguments have to be handed past
it:

```bash
npm start -- --reset-cache
```

**The chat socket hangs on "Opening Web Socket..."** — the handshake is being
refused and the client cannot tell. A refusal now comes back as `401`; it used
to be a `302` to the login page, which a WebSocket client can do nothing with,
so it retried every five seconds in silence. `WebSocketService` logs the close
code: **1006 with no frames means the handshake was refused.**

`/ws` accepts either the session cookie or a one-time ticket — see
[How the chat socket authenticates](#how-the-chat-socket-authenticates) — so a
refusal means neither arrived. Look just above the close for
`Couldnt get a socket ticket`: a `401` there means the app is signed out, and
signing in again is the fix. No such line, and the ticket was fetched and still
refused — check the clock skew between phone and server, since a ticket only
lives 30 seconds.

---

## Author

Created by Amara Okonkwo.

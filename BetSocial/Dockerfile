# Build ----------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# The pom on its own first, so the dependency download is a layer that survives
# every change to src/. Without this split, editing one Java file re-downloads
# the whole tree - and this one includes firebase-admin, which is not small.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

# Tests are not run here. They are integration tests against a real PostgreSQL
# started by Testcontainers, which needs a Docker daemon - not something a
# container build has. CI runs them on every pull request instead; see
# .github/workflows/backend-tests.yml. A build that silently skipped tests it
# could have run would be worth objecting to; this one cannot run them at all.
RUN mvn -B -DskipTests package

# Run -------------------------------------------------------------------------
# Debian rather than Alpine on purpose. firebase-admin brings conscrypt, whose
# native library is built against glibc, and musl would fail at the point TLS is
# first needed rather than at build time.
FROM eclipse-temurin:21-jre

# Not root. If anything ever escapes the JVM, it should land as a user who owns
# nothing.
RUN useradd --system --create-home --uid 10001 betsocial
USER betsocial
WORKDIR /app

COPY --from=build --chown=betsocial:betsocial /build/target/World-*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx: the JVM reads the container's limit,
# so the same image is right on a 512MB machine and a 2GB one. Left at a default
# the heap would be a quarter of the limit and the rest wasted.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

# prod is the default here rather than something to remember: the development
# profile seeds an account called 'admin' whose password is 'password', and an
# image that does that by default is a mistake waiting for a distracted day.
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

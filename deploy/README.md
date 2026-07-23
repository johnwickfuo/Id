# Deploying to id.africodeai.online

The project has two parts that deploy differently:

| Part       | What it is                     | Where it goes                          |
|------------|--------------------------------|----------------------------------------|
| `frontend/`| Static HTML/JS/CSS             | `public_html` (served by nginx)        |
| `backend/` | Kotlin/Ktor API on port 8080   | A systemd service, proxied by nginx    |

The frontend calls the API with a **relative path** (`/generate`), so nginx
serves both the site and the API from the same domain — no CORS, no mixed
content.

---

## 1. Deploy the frontend

SSH into the server, then:

```bash
# Pull the repo somewhere temporary
git clone -b claude/driver-license-generator-migration-x61kap \
  https://github.com/johnwickfuo/Id.git /tmp/id-deploy

# Copy the frontend into public_html
cp -r /tmp/id-deploy/frontend/* \
  /home/admin/web/id.africodeai.online/public_html/

chown -R admin:admin /home/admin/web/id.africodeai.online/public_html
```

## 2. Wire up nginx

**Plain nginx:** copy `nginx-id.africodeai.online.conf` into
`/etc/nginx/sites-available/`, symlink it into `sites-enabled/`, adjust the
SSL cert paths, then `nginx -t && systemctl reload nginx`.

**HestiaCP / VestaCP:** do NOT overwrite the panel-managed vhost. Add only the
API `location` block via a proxy template or the custom config include:

```bash
# Create a custom include the panel will pick up
cat > /home/admin/conf/web/id.africodeai.online/nginx.ssl.conf_location <<'EOF'
location ~ ^/(generate|validate)$ {
    proxy_pass         http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header   Host              $host;
    proxy_set_header   X-Real-IP         $remote_addr;
    proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header   X-Forwarded-Proto $scheme;
}
EOF
nginx -t && systemctl reload nginx
```

(Exact filename varies by panel version — check the existing
`/home/admin/conf/web/id.africodeai.online/` directory for the pattern.)

## 3. Build & run the backend

The backend is a self-contained Ktor app. Build the fat JAR (requires
JDK 17+; the Gradle wrapper is included, so no system Gradle needed):

```bash
cd /tmp/id-deploy/backend
./gradlew buildFatJar       # produces build/libs/driver-license-generator-all.jar

mkdir -p /home/admin/apps/driver-license-generator
cp build/libs/driver-license-generator-all.jar \
  /home/admin/apps/driver-license-generator/driver-license-generator.jar

# Install the service
sudo cp /tmp/id-deploy/deploy/driver-license-backend.service \
  /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now driver-license-backend
sudo systemctl status driver-license-backend
```

Check it responds locally:

```bash
curl -X POST http://127.0.0.1:8080/generate \
  -H 'Content-Type: application/json' \
  -d '{"state":"CA","firstName":"Jane","lastName":"Doe"}'
```

Then load `https://id.africodeai.online/` and click **Generate**.

---

## Backend notes

The backend was rewritten so it actually builds and runs (the original
import did not compile). What changed:

- Standard Gradle layout: `build.gradle.kts` / `settings.gradle.kts` at
  `backend/`, sources under `backend/src/main/kotlin`.
- Gradle wrapper committed (`./gradlew`), so no system Gradle is required.
- Kotlin 1.9.24 + Ktor 2.3.12, targeting JVM 17.
- Valid package name `com.africodeai.id`.
- `generate` and `validate` reimplemented as idiomatic Kotlin; the
  per-state validation patterns are a Kotlin `Map<String, Regex>`.
- A small JUnit test suite (`./gradlew test`).

Build/verify locally:

```bash
cd backend
./gradlew test          # runs the unit tests
./gradlew buildFatJar   # builds build/libs/driver-license-generator-all.jar
java -jar build/libs/driver-license-generator-all.jar   # serves on 127.0.0.1:8080
```

### API

| Method | Path        | Body                                          | Response                       |
|--------|-------------|-----------------------------------------------|--------------------------------|
| POST   | `/generate` | `{"state","firstName","lastName"}`            | `{"licenseNumber":"CA-JD-…"}`  |
| POST   | `/validate` | `{"state","licenseNumber"}`                   | `{"valid":true|false}`         |

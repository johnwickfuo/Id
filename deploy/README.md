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

> ⚠️ **The backend does not compile as-is** — see "Known backend issues"
> below. Fix those first, then:

```bash
cd /tmp/id-deploy/backend/src
./gradlew build            # requires JDK 17+ ; produces a JAR in build/libs/

mkdir -p /home/admin/apps/driver-license-generator
cp build/libs/*-all.jar \
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

## Known backend issues (must fix before step 3 works)

The imported backend is a rough draft and will not build without changes:

1. **No Gradle wrapper** — there's no `gradlew` script or
   `gradle-wrapper.jar`, so `./gradlew` won't exist. Run `gradle wrapper`
   once (with a system Gradle) to generate them, or use a system `gradle`.
2. **`Application.kt` is not valid Kotlin:**
   - Line 1 is an HTML comment (`<!-- ... -->`).
   - The package name `com.papaguycodes.driver-license-generator` contains
     hyphens, which Kotlin does not allow in package identifiers.
   - The Ktor imports are 1.x-style but the build requests Ktor 2.5.0
     (which also isn't a real release — latest 2.x is 2.3.x).
   - `validateLicenseNumber` uses JavaScript/Python regex-literal and map
     syntax (`'AL': /^.../,`) that is not Kotlin.
3. **Odd project layout** — `build.gradle.kts` lives under `backend/src/`
   rather than `backend/`.

I can fix all of this in a follow-up if you'd like a backend that actually
builds and runs.

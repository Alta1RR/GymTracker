# GymTracker Docker Deploy

## 1. First server setup

Install Docker and Docker Compose plugin on the server.

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Fill real values in `.env`:

```properties
POSTGRES_PASSWORD=...
TELEGRAM_BOT_TOKEN=...
TELEGRAM_AUTH_ENABLED=true
```

## 2. Run

```bash
docker compose up -d --build
```

The app will be available on:

```text
http://SERVER_IP:8080
```

## 3. Update after code changes

```bash
git pull
docker compose up -d --build
```

## 4. Logs

```bash
docker compose logs -f app
docker compose logs -f db
```

## 5. Stop

```bash
docker compose down
```

Do not run `docker compose down -v` unless you intentionally want to delete the database volume.

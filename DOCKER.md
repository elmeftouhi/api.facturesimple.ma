# Docker Setup for Facturesimple API

This guide explains how to build and run the Facturesimple API using Docker.

## Prerequisites

- Docker and Docker Compose installed
- At least 2GB of free disk space for the build

## Quick Start with Docker Compose (Recommended)

The easiest way to get the application running with PostgreSQL:

```bash
docker-compose up -d
```

This will:
1. Start a PostgreSQL database container
2. Build the API image
3. Start the API container
4. Wait for the database to be healthy before starting the API

The API will be available at: `http://localhost:8080`

## Important: Update JWT_SECRET

Before running in production, update the `JWT_SECRET` in `docker-compose.yml`:
- Must be at least 32 characters long
- Use a secure random string

```yaml
environment:
  JWT_SECRET: your-secure-32-character-minimum-secret-key-here
```

## Docker Compose Commands

### Start the application
```bash
docker-compose up -d
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api
docker-compose logs -f postgres
```

### Stop the application
```bash
docker-compose down
```

### Stop and remove volumes (WARNING: deletes database data)
```bash
docker-compose down -v
```

### Rebuild the image
```bash
docker-compose up -d --build
```

## Building the Docker Image Manually

If you prefer to build the image without Docker Compose:

```bash
docker build -t facturesimple-api:latest .
```

## Running the Container Manually

```bash
docker run \
  --name facturesimple-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://postgres:5432/facturesimple \
  -e DB_USERNAME=admin \
  -e DB_PASSWORD=admin123 \
  -e JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long \
  -e JWT_EXPIRATION_MINUTES=120 \
  facturesimple-api:latest
```

## Environment Variables (Production Profile)

The following environment variables are required:

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `DB_URL` | Database connection URL | `jdbc:postgresql://postgres:5432/facturesimple` |
| `DB_USERNAME` | Database user | `admin` |
| `DB_PASSWORD` | Database password | `admin123` |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | `your-secure-secret-key-here` |
| `JWT_EXPIRATION_MINUTES` | JWT token expiration (optional) | `120` |

## Health Check

The API includes a health check endpoint. You can verify the application is running:

```bash
curl http://localhost:8080/actuator/health
```

## Accessing the Application

Once running, the API is available at:
- **Base URL**: `http://localhost:8080`
- **API Version**: `/v1`

### Example endpoints:
- POST `/v1/auth/register` - Register a new user
- POST `/v1/auth/login` - Login
- GET `/v1/me` - Get current user info (requires auth)

See the main README.md for full API documentation.

## Database Management

### Accessing PostgreSQL directly

```bash
docker exec -it facturesimple-db psql -U admin -d facturesimple
```

### Viewing database logs

```bash
docker-compose logs postgres
```

## Troubleshooting

### API fails to start - Database connection error
- Ensure PostgreSQL container is running: `docker-compose logs postgres`
- Check the database credentials match in the environment variables
- Wait a few seconds for the database to be fully ready

### Port already in use
If port 8080 or 5432 is already in use, modify docker-compose.yml:

```yaml
services:
  api:
    ports:
      - "8081:8080"  # Change to any available port
  postgres:
    ports:
      - "5433:5432"  # Change to any available port
```

### Out of memory during build
Increase Docker's memory allocation or build without optimizations:

```bash
docker build -t facturesimple-api:latest --memory=2g .
```

## Multi-stage Build Benefits

The Dockerfile uses a multi-stage build process:
1. **Builder stage**: Maven container that compiles the application
2. **Runtime stage**: Lightweight JRE-only image runs the compiled JAR

This keeps the final image small (typically ~500MB) while ensuring proper compilation.

## Security Best Practices

1. **Always use a strong JWT_SECRET** in production (min 32 characters)
2. **Never commit secrets** to version control
3. **Use Docker secrets or environment variable files** for sensitive data
4. **Keep base images updated** regularly
5. **Run container as non-root user** (already configured)

## Next Steps

- Read the main [README.md](README.md) for API documentation
- Check [INVOICE_IMPLEMENTATION.md](INVOICE_IMPLEMENTATION.md) for implementation details
- Review the Spring Boot documentation: https://spring.io/projects/spring-boot


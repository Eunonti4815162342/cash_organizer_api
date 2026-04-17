# Environment Configuration - Cash Organizer Backend

## Overview
The backend uses Spring Boot profiles to manage different configurations for development, staging, and production environments.

## Profiles Available

### 1. Development (`dev`)
- **Database**: PostgreSQL on localhost
- **SQL Logging**: Enabled (show-sql: true)
- **Error Details**: Full stack traces
- **CORS**: localhost:8080, localhost:8085, localhost:3000
- **Logging Level**: DEBUG
- **JWT**: Default dev key (safe to commit)
- **Features**: Full debugging enabled

**Run development:**
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
# OR
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun
```

### 2. Staging (`staging`)
- **Database**: Remote staging database (via env vars)
- **SQL Logging**: Disabled
- **Error Details**: Only with request parameter
- **CORS**: staging.cashorganizer.com, app-staging.cashorganizer.com
- **Logging Level**: INFO (less verbose)
- **JWT**: Via environment variable (required)
- **Database Pool**: Optimized for staging load
- **Features**: Closer to production, but allows debugging

**Run staging:**
```bash
export SPRING_PROFILES_ACTIVE=staging
export SPRING_DATASOURCE_URL=jdbc:postgresql://staging-db:5432/cash_organizer_staging
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<staging-password>
export JWT_SECRET_KEY=<staging-jwt-secret>
./gradlew bootRun
```

### 3. Production (`prod`)
- **Database**: Remote production database (via env vars)
- **SQL Logging**: Disabled
- **Error Details**: Minimal (no stack traces exposed)
- **CORS**: cashorganizer.com, app.cashorganizer.com
- **Logging Level**: ERROR (production only)
- **JWT**: Via environment variable (required)
- **Database Pool**: Optimized for production (20 connections)
- **Features**: Secure, performant, minimal logging
- **Validation Mode**: Hibernate DDL is `validate` (no schema changes)
- **Graceful Shutdown**: Enabled
- **Metrics**: Enabled for monitoring

**Run production:**
```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/cash_organizer
export SPRING_DATASOURCE_USERNAME=<prod-username>
export SPRING_DATASOURCE_PASSWORD=<prod-password>
export JWT_SECRET_KEY=<prod-jwt-secret>
export JWT_EXPIRATION=86400000
./gradlew bootRun
```

## Environment Variables Reference

### Required in Staging/Production
```bash
SPRING_PROFILES_ACTIVE=staging|prod     # Environment profile
SPRING_DATASOURCE_URL                   # Database connection URL
SPRING_DATASOURCE_USERNAME              # Database username
SPRING_DATASOURCE_PASSWORD              # Database password
JWT_SECRET_KEY                          # JWT signing key (min 64 chars)
```

### Optional
```bash
SERVER_PORT                             # Default: 8085
JWT_EXPIRATION                          # Default: 86400000 (24 hours)
```

## Security Best Practices

### For Staging
1. Use staging database with staging credentials
2. Use distinct JWT secret (not production secret)
3. Allow staging domain origins only
4. Monitor logs for issues

### For Production
1. **Never** commit real secrets to git
2. Use strong JWT secret (min 64 characters, random)
3. Use environment variables for all secrets
4. Database should be on private network
5. Enable HTTPS (use reverse proxy/load balancer)
6. Monitor health endpoints: `/actuator/health`
7. Monitor metrics: `/actuator/metrics`
8. Regular backups of production database
9. Keep logs in `/var/log/cash-organizer/` with rotation

## Configuration File Precedence

When running with a profile, Spring loads configurations in this order:
1. `application.yml` (default, base configuration)
2. `application-{profile}.yml` (profile-specific overrides)
3. Environment variables (highest priority)

Example: With `spring.profiles.active=prod`:
- First load `application.yml`
- Then load `application-prod.yml` (overrides values)
- Then load environment variables (override everything)

## Docker/Container Deployment

For container deployments, inject environment variables:

```bash
docker run -e SPRING_PROFILES_ACTIVE=prod \
           -e SPRING_DATASOURCE_URL="jdbc:postgresql://db:5432/cash_organizer" \
           -e SPRING_DATASOURCE_USERNAME="dbuser" \
           -e SPRING_DATASOURCE_PASSWORD="dbpass" \
           -e JWT_SECRET_KEY="your-secret-key-here" \
           -p 8085:8085 \
           cash-organizer-api:latest
```

## Verifying Active Profile

Check which profile is active:
1. Look at startup logs: `The following profiles are active: dev`
2. Access `/actuator/env` endpoint (if actuator enabled)
3. Check application logs for environment-specific messages

## Migration Between Environments

### Promote from Dev to Staging
1. Ensure all code changes committed
2. Database schema is backward compatible
3. Test with staging credentials locally
4. Deploy to staging server
5. Run Liquibase migrations on staging DB

### Promote from Staging to Production
1. All staging tests passed
2. Load testing completed
3. Security review completed
4. Database backup created
5. Rollback plan documented
6. Deploy during low-traffic window
7. Monitor `/actuator/health` for issues

## Troubleshooting

### Check active profile
```bash
# In application logs, look for:
# "The following profiles are active: prod"
```

### Override profile at runtime
```bash
./gradlew bootRun --args='--spring.profiles.active=staging'
```

### Reset to development
```bash
unset SPRING_PROFILES_ACTIVE
./gradlew bootRun  # Will default to dev
```

### Database connection issues
1. Verify `SPRING_DATASOURCE_URL` format
2. Check `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`
3. Ensure database service is running
4. Check firewall/network connectivity

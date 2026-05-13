# Expmatik

## Stack

- Java 21 + Spring Boot 3.5
- React 18 + Vite 7
- PostgreSQL 15
- Maven 3.9 / Node 22

---

## Instalación en local

### Base de datos

Necesitas PostgreSQL 15 corriendo. Copia el `.env.example` y ajusta tus credenciales:

```bash
cp backend/.env.example backend/.env
```

### Backend

```bash
cd backend
.\mvnw spring-boot:run
```

Arranca en `http://localhost:8080`. Swagger en `http://localhost:8080/swagger-ui/index.html`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Arranca en `http://localhost:3000`.

---

## Docker

Necesitas el `backend/.env` creado antes de lanzar.

```bash
docker compose up --build
```

| Servicio | Puerto |
|---|---|
| PostgreSQL | 5432 |
| Backend | 8080 |
| Frontend | 3000 |

```bash
docker compose down        # parar
docker compose down -v     # parar y borrar datos
```

---

## Cuentas de prueba

| Email | Contraseña | Rol |
|---|---|---|
| admin@expmatik.com | admin123 | Administrador |
| admin2@expmatik.com | admin123 | Administrador |
| repo@expmatik.com | repo123 | Reponedor |
| repo2@expmatik.com | repo123 | Reponedor |
| prediction@expmatik.com | repo123 | Administrador |

> **prediction@expmatik.com** es la cuenta que contiene datos históricos de ventas necesarios para que la funcionalidad de predicción funcione correctamente.

---

## Tests

```bash
cd backend
.\mvnw test                   # solo tests
.\mvnw verify                 # tests + informe de cobertura JaCoCo
```

El informe de cobertura queda en `backend/target/site/jacoco/index.html`.

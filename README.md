# SaaS — Sistema de Gestión de Turnos y Servicios Técnicos

Backend **production-ready** en **Java 21 + Spring Boot 3.3** para la gestión completa del ciclo de vida de turnos técnicos en una plataforma SaaS multiempresa.

---

## 🏗️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 (Records, Sealed Classes, Pattern Matching) |
| Framework | Spring Boot 3.3.0 |
| Base de datos | PostgreSQL 15+ |
| ORM | Spring Data JPA + Hibernate 6 |
| Migraciones | Flyway (`ddl-auto: validate`) |
| Seguridad | Spring Security 6, JJWT 0.12.6, BCrypt(12) |
| Rate Limiting | Bucket4j 8.10.1 (in-memory) |
| Mappers | MapStruct 1.5.5 |
| Notificaciones | WhatsApp Business Cloud API + Gmail SMTP |
| Export | Apache POI 5.2.5 (XLSX) |
| Testing | JUnit 5 + Mockito + Testcontainers |

---

## 🚀 Inicio rápido

### 1. Prerequisitos

- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### 2. Variables de entorno

```bash
cp .env.example .env
# Editar .env con tus valores reales
```

### 3. Levantar base de datos

```bash
docker-compose up -d
```

### 4. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

La API estará disponible en: **http://localhost:8080**

Health check: **http://localhost:8080/actuator/health**

---

## 📁 Estructura del proyecto

```
src/main/java/com/turnos/saas/
├── TurnosSaasApplication.java
├── config/          # SecurityConfig, AsyncConfig
├── filter/          # JwtAuthFilter, RateLimitFilter
├── security/        # JwtUtil, TenantGuard
├── exception/       # GlobalExceptionHandler + 4 excepciones de dominio
├── validation/      # @NoHtml (anti-XSS)
├── model/
│   ├── enums/       # TurnoEstado (con máquina de estados), Rol, CotizacionEstado
│   └── entity/      # 11 entidades JPA
├── repository/      # 11 repositorios Spring Data
├── dto/
│   ├── request/     # Records de entrada con Bean Validation
│   └── response/    # Records de salida + ApiResponse<T>
├── mapper/          # 5 interfaces MapStruct
├── service/         # 10 servicios + NotificacionScheduler
└── controller/      # 10 controllers REST
```

---

## 🔐 Seguridad

### Autenticación JWT

- **Access Token:** 15 minutos (HMAC-SHA256)
- **Refresh Token:** 7 días, almacenado con hash SHA-256 en BD

```http
Authorization: Bearer <accessToken>
```

### Rate Limiting (Bucket4j)

| Endpoint | Límite |
|---|---|
| `/api/v1/auth/**` | 10 req/min por IP |
| Resto | 100 req/min por IP |

### Roles

| Rol | Permisos |
|---|---|
| `CLIENTE` | Solicitar turnos, aceptar/rechazar cotizaciones, ver propios |
| `OPERADOR` | Gestionar turnos, cotizar, agenda |
| `ADMIN` | Todo + usuarios, configuración, reportes |

---

## 📅 Máquina de estados del turno

```
SOLICITADO → EN_COTIZACION → COTIZADO → CONFIRMADO → PROGRAMADO → FINALIZADO
                  ↓              ↓           ↓             ↓
               CANCELADO     CANCELADO   CANCELADO     CANCELADO
```

**Regla crítica:** El cliente solo puede cancelar si faltan **≥ 48 horas** para la fecha confirmada.

---

## 📡 API — Resumen de endpoints

Ver [`guia-api.md`](./guia-api.md) para la documentación completa con ejemplos curl.

**Base URL:** `http://localhost:8080/api/v1`

| Módulo | Endpoints |
|---|---|
| Auth | 6 endpoints |
| Empresas | 6 endpoints |
| Usuarios | 6 endpoints |
| Servicios | 5 endpoints |
| Turnos | 9 endpoints |
| Cotizaciones | 5 endpoints |
| Agenda | 5 endpoints |
| Notificaciones | 5 endpoints |
| Reportes | 4 endpoints |

---

## 🔔 Notificaciones

### Triggers automáticos

| Estado | Canal |
|---|---|
| COTIZADO | WhatsApp + Email → Cliente |
| CONFIRMADO | WhatsApp + Email → Cliente + Operador |
| FINALIZADO | WhatsApp + Email → Cliente |
| CANCELADO | WhatsApp + Email → Cliente + Operador |
| Recordatorio 24h | WhatsApp + Email → Cliente |
| Recordatorio 2h | WhatsApp → Cliente |

### Configurar WhatsApp + Gmail por empresa

```http
PUT /api/v1/empresas/{id}/notificaciones/config
```

---

## 🧪 Tests

```bash
# Tests unitarios
mvn test -Dtest=TurnoEstadoTest,TurnoServiceTest,JwtUtilTest

# Tests de integración (requiere Docker)
mvn test -Dtest=AuthControllerIT

# Todos los tests
mvn verify
```

---

## 🐳 Docker

```bash
# Solo PostgreSQL
docker-compose up -d

# PostgreSQL + pgAdmin (UI en http://localhost:5050)
docker-compose --profile tools up -d
```

---

## ⚙️ Variables de entorno

| Variable | Descripción | Requerido |
|---|---|---|
| `DB_URL` | JDBC URL de PostgreSQL | ✅ |
| `DB_USER` | Usuario de la BD | ✅ |
| `DB_PASSWORD` | Contraseña de la BD | ✅ |
| `JWT_SECRET` | Secreto JWT (≥32 chars) | ✅ |
| `GMAIL_USER` | Email para notificaciones | Para email |
| `GMAIL_APP_PASSWORD` | App Password de Gmail | Para email |

Los tokens de WhatsApp se configuran **por empresa** en la BD (tabla `notif_config`).

---

## 📊 Reportes y Export

```bash
# Dashboard KPIs
GET /api/v1/empresas/{id}/reportes/dashboard

# Export Excel
GET /api/v1/empresas/{id}/reportes/export?desde=2026-04-01&hasta=2026-04-30
```

---

*SaaS Gestión de Turnos v1.0.0 — Spring Boot 3.3 + Java 21*

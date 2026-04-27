# Guía de API — Sistema de Gestión de Turnos y Servicios Técnicos 🔌

> **Base URL:** `http://localhost:8080`
> Todos los endpoints (excepto los de autenticación) requieren el header:
> `Authorization: Bearer <accessToken>`

---

## Convenciones de Respuesta

Todos los endpoints devuelven un envelope estándar `ApiResponse<T>`:

```json
{
  "status": "OK",
  "message": "Operación exitosa",
  "data": { ... },
  "timestamp": "2026-04-14T12:00:00Z"
}
```

Las respuestas paginadas tienen la forma `PageResponse<T>`:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```

### Roles disponibles

| Rol | Descripción |
|---|---|
| `ROLE_CLIENTE` | Cliente final. Solicita turnos, acepta cotizaciones. |
| `ROLE_OPERADOR` | Gestiona turnos, cotiza, maneja agenda. |
| `ROLE_ADMIN` | Acceso total. Configura empresa, reportes, usuarios. |

### Estados del turno

```
SOLICITADO → EN_COTIZACION → COTIZADO → CONFIRMADO → PROGRAMADO → FINALIZADO
                                  ↓             ↓            ↓           
                               CANCELADO    CANCELADO    CANCELADO       
```

| Estado | Descripción |
|---|---|
| `SOLICITADO` | El cliente envió la solicitud. |
| `EN_COTIZACION` | El operador está preparando la cotización. |
| `COTIZADO` | Cotización enviada al cliente, esperando respuesta. |
| `CONFIRMADO` | Cliente aceptó y seña registrada. |
| `PROGRAMADO` | Confirmado y agendado para la fecha acordada. |
| `FINALIZADO` | Servicio prestado y cerrado. |
| `CANCELADO` | Cancelado por cliente u operador. |

---

## 1. 🔐 Auth — Autenticación

**Base path:** `/api/v1/auth`
> ⚠️ Estos endpoints **no requieren** token JWT.

---

### `POST /api/v1/auth/register`
**Registra un nuevo cliente en el sistema.**

**Request Body:**
```json
{
  "nombre":   "María García",
  "email":    "maria@example.com",
  "password": "MiPass1234!",
  "telefono": "+54 11 1234-5678"
}
```

**Respuesta exitosa `201 Created`:**
```json
{
  "status": "CREATED",
  "message": "Usuario registrado exitosamente",
  "data": {
    "accessToken":  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "d2NhY2JmNzMtNzQ1...",
    "expiresIn":    900,
    "usuario": {
      "id":        "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "nombre":    "María García",
      "email":     "maria@example.com",
      "rol":       "CLIENTE",
      "empresaId": null
    }
  },
  "timestamp": "2026-04-14T12:00:00Z"
}
```

**Test con curl:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nombre":   "María García",
    "email":    "maria@example.com",
    "password": "MiPass1234!",
    "telefono": "+54 11 1234-5678"
  }'
```

---

### `POST /api/v1/auth/login`
**Inicia sesión y obtiene un par de tokens JWT.**

**Request Body:**
```json
{
  "email":    "maria@example.com",
  "password": "MiPass1234!"
}
```

**Respuesta exitosa `200 OK`:**
```json
{
  "status": "OK",
  "message": "Login exitoso",
  "data": {
    "accessToken":  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "d2NhY2JmNzMtNzQ1...",
    "expiresIn":    900,
    "usuario": {
      "id":        "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "nombre":    "María García",
      "email":     "maria@example.com",
      "rol":       "CLIENTE",
      "empresaId": "550e8400-e29b-41d4-a716-446655440000"
    }
  },
  "timestamp": "2026-04-14T12:00:00Z"
}
```

**Test con curl:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "maria@example.com", "password": "MiPass1234!"}'
```

---

### `POST /api/v1/auth/refresh`
**Renueva el `accessToken` usando el `refreshToken` vigente.**

**Request Body:**
```json
{
  "refreshToken": "d2NhY2JmNzMtNzQ1..."
}
```

**Respuesta exitosa `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "accessToken":  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "ZTMwY2Q4ZjEtNzM4...",
    "expiresIn":    900
  }
}
```

---

### `POST /api/v1/auth/logout` 🔒
**Cierra sesión revocando el refresh token.**

**Headers requeridos:** `Authorization: Bearer <accessToken>`

**Request Body:**
```json
{
  "refreshToken": "d2NhY2JmNzMtNzQ1..."
}
```

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "message": "Sesión cerrada exitosamente",
  "data": null
}
```

---

### `POST /api/v1/auth/forgot-password`
**Envía un email con el token de recuperación de contraseña.**

**Request Body:**
```json
{ "email": "maria@example.com" }
```

**Respuesta `202 Accepted`:**
```json
{
  "message": "Si el email existe, recibirá instrucciones para restablecer su contraseña."
}
```

---

### `POST /api/v1/auth/reset-password`
**Restablece la contraseña usando el token recibido por email.**

**Request Body:**
```json
{
  "token":       "el-token-del-email",
  "newPassword": "NuevaPass5678!"
}
```

**Respuesta `200 OK`:**
```json
{ "message": "Contraseña restablecida exitosamente" }
```

---

## 2. 🏢 Empresas

**Base path:** `/api/v1/empresas`
> 🔒 Todos los endpoints requieren JWT. Solo `ADMIN`.

---

### `GET /api/v1/empresas`
**Lista todas las empresas del sistema (paginado).**

**Roles:** `ADMIN`

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "content": [
      {
        "id":            "550e8400-e29b-41d4-a716-446655440000",
        "nombre":        "Taller García",
        "emailContacto": "info@tallergarcia.com",
        "direccion":     "Av. Corrientes 1234, CABA",
        "telefono":      "+54 11 4444-5555",
        "config": {
          "horaApertura":          "08:00",
          "horaCierre":            "18:00",
          "duracionSlotMinutos":   30,
          "sabadoHabilitado":      true,
          "domingoHabilitado":     false
        },
        "activa":    true,
        "creadoEn": "2026-01-10T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

---

### `POST /api/v1/empresas`
**Crea una nueva empresa tenant.**

**Roles:** `ADMIN`

**Request Body:**
```json
{
  "nombre":        "Taller García",
  "emailContacto": "info@tallergarcia.com",
  "direccion":     "Av. Corrientes 1234, CABA",
  "telefono":      "+54 11 4444-5555"
}
```

**Respuesta `201 Created`:** devuelve el `EmpresaResponse` completo.

**Test con curl:**
```bash
curl -X POST http://localhost:8080/api/v1/empresas \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre":        "Taller García",
    "emailContacto": "info@tallergarcia.com",
    "direccion":     "Av. Corrientes 1234, CABA",
    "telefono":      "+54 11 4444-5555"
  }'
```

---

### `GET /api/v1/empresas/{empresaId}`
**Obtiene una empresa por su UUID.**

**Roles:** `OPERADOR`, `ADMIN`

```bash
curl http://localhost:8080/api/v1/empresas/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

---

### `PUT /api/v1/empresas/{empresaId}`
**Actualiza los datos de una empresa.**

**Roles:** `ADMIN`

**Request Body:** mismos campos que `POST`.

---

### `PATCH /api/v1/empresas/{empresaId}/config`
**Configura horarios y disponibilidad de la empresa.**

**Roles:** `ADMIN`

**Request Body:**
```json
{
  "horaApertura":        "09:00",
  "horaCierre":          "19:00",
  "duracionSlotMinutos": 45,
  "sabadoHabilitado":    true,
  "domingoHabilitado":   false
}
```

---

### `DELETE /api/v1/empresas/{empresaId}`
**Deshabilita una empresa (soft delete).**

**Roles:** `ADMIN`

**Respuesta:** `204 No Content`

---

## 3. 👥 Usuarios

**Base path:** `/api/v1/usuarios` y `/api/v1/empresas/{empresaId}/usuarios`

---

### `GET /api/v1/empresas/{empresaId}/usuarios`
**Lista los usuarios de una empresa (paginado).**

**Roles:** `ADMIN`

**Query Params:** `page`, `size`, `sort` (default: `creadoEn,desc`)

**Test con curl:**
```bash
curl "http://localhost:8080/api/v1/empresas/550e8400.../usuarios?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

---

### `POST /api/v1/empresas/{empresaId}/usuarios`
**Crea un operador dentro de la empresa.**

**Roles:** `ADMIN`

**Request Body:**
```json
{
  "nombre":   "Carlos Operador",
  "email":    "carlos@tallergarcia.com",
  "password": "Pass1234!",
  "telefono": "+54 11 9999-0001"
}
```

**Respuesta `201 Created`:** devuelve `UsuarioResponse` con `rol: "OPERADOR"`.

---

### `GET /api/v1/usuarios/me` 🔒
**Devuelve el perfil del usuario autenticado.**

**Roles:** Cualquier usuario autenticado.

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "id":          "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "nombre":      "María García",
    "email":       "maria@example.com",
    "telefono":    "+54 11 1234-5678",
    "rol":         "CLIENTE",
    "empresaId":   "550e8400-e29b-41d4-a716-446655440000",
    "activo":      true,
    "creadoEn":    "2026-04-10T09:00:00Z"
  }
}
```

---

### `PUT /api/v1/usuarios/me` 🔒
**Actualiza el perfil del usuario autenticado.**

**Roles:** Cualquier usuario autenticado.

**Request Body:**
```json
{
  "nombre":   "María García Ruiz",
  "telefono": "+54 11 9876-5432"
}
```

---

### `PATCH /api/v1/usuarios/{userId}/rol`
**Cambia el rol de un usuario.**

**Roles:** `ADMIN`

**Query Param:** `rol` — `CLIENTE` | `OPERADOR` | `ADMIN`

```bash
curl -X PATCH "http://localhost:8080/api/v1/usuarios/uuid/rol?rol=OPERADOR" \
  -H "Authorization: Bearer <token>"
```

---

### `DELETE /api/v1/usuarios/{userId}`
**Deshabilita un usuario (soft delete).**

**Roles:** `ADMIN`

**Respuesta:** `204 No Content`

---

## 4. 🔧 Servicios

**Base path:** `/api/v1/empresas/{empresaId}/servicios`

---

### `GET /api/v1/empresas/{empresaId}/servicios`
**Lista el catálogo de servicios de la empresa.**

**Roles:** `CLIENTE`, `OPERADOR`, `ADMIN`

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "content": [
      {
        "id":                       "abc12345-...",
        "nombre":                   "Reparación de PC",
        "descripcion":              "Diagnóstico y reparación general",
        "precioBase":               8500.00,
        "duracionEstimadaMinutos":  60,
        "activo":                   true
      }
    ],
    "totalElements": 5
  }
}
```

---

### `POST /api/v1/empresas/{empresaId}/servicios`
**Crea un servicio técnico en el catálogo.**

**Roles:** `ADMIN`

**Request Body:**
```json
{
  "nombre":                   "Reparación de PC",
  "descripcion":              "Diagnóstico y reparación general de computadoras",
  "precioBase":               8500.00,
  "duracionEstimadaMinutos":  60
}
```

**Respuesta `201 Created`:** devuelve el `ServicioResponse` completo.

---

### `GET /api/v1/empresas/{empresaId}/servicios/{servicioId}`
**Obtiene detalle de un servicio.**

**Roles:** `CLIENTE`, `OPERADOR`, `ADMIN`

---

### `PUT /api/v1/empresas/{empresaId}/servicios/{servicioId}`
**Actualiza un servicio.**

**Roles:** `ADMIN`

---

### `PATCH /api/v1/empresas/{empresaId}/servicios/{servicioId}/estado`
**Activa o desactiva un servicio del catálogo.**

**Roles:** `ADMIN`

**Query Param:** `activo=true|false`

```bash
curl -X PATCH "http://localhost:8080/api/v1/empresas/uuid/servicios/uuid/estado?activo=false" \
  -H "Authorization: Bearer <token>"
```

---

## 5. 📅 Turnos

**Base path:** `/api/v1/empresas/{empresaId}/turnos`

---

### `GET /api/v1/empresas/{empresaId}/turnos`
**Lista turnos de la empresa con filtros (paginado).**

**Roles:** `OPERADOR`, `ADMIN`

**Query Params:**

| Parámetro | Default | Descripción |
|---|---|---|
| `estado` | — | Filtrar por estado del turno |
| `fecha` | — | Filtrar por fecha confirmada (`YYYY-MM-DD`) |
| `clienteId` | — | Filtrar por UUID de cliente |
| `page` | `0` | Número de página |
| `size` | `20` | Elementos por página |
| `sort` | `fechaConfirmada,asc` | Campo y dirección |

**Test con curl:**
```bash
curl "http://localhost:8080/api/v1/empresas/550e8400.../turnos?estado=PROGRAMADO&fecha=2026-04-20" \
  -H "Authorization: Bearer <token>"
```

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "content": [
      {
        "id":             "d1e2f3a4-...",
        "fecha":          "2026-04-20",
        "hora":           "10:00",
        "estado":         "PROGRAMADO",
        "nombreCliente":  "María García",
        "nombreServicio": "Reparación de PC"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 12,
    "totalPages": 1
  }
}
```

---

### `POST /api/v1/empresas/{empresaId}/turnos`
**Solicita un nuevo turno.**

**Roles:** `CLIENTE`

**Request Body:**
```json
{
  "servicioId":      "abc12345-...",
  "fechaPreferida":  "2026-04-25",
  "horaPreferida":   "10:00",
  "observaciones":   "La PC no enciende desde ayer"
}
```

**Respuesta `201 Created`:**
```json
{
  "status": "CREATED",
  "message": "Turno solicitado. El operador le enviará una cotización pronto.",
  "data": {
    "id":              "d1e2f3a4-...",
    "empresaId":       "550e8400-...",
    "cliente": {
      "id":     "7c9e6679-...",
      "nombre": "María García",
      "email":  "maria@example.com"
    },
    "servicio": {
      "id":     "abc12345-...",
      "nombre": "Reparación de PC"
    },
    "fechaSolicitada": "2026-04-25",
    "horaSolicitada":  "10:00",
    "estado":          "SOLICITADO",
    "observaciones":   "La PC no enciende desde ayer",
    "cotizacion":      null,
    "senia":           null,
    "historial":       [],
    "creadoEn":        "2026-04-14T15:30:00Z"
  }
}
```

**Test con curl:**
```bash
curl -X POST http://localhost:8080/api/v1/empresas/550e8400.../turnos \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "servicioId":     "abc12345-...",
    "fechaPreferida": "2026-04-25",
    "horaPreferida":  "10:00",
    "observaciones":  "La PC no enciende desde ayer"
  }'
```

---

### `GET /api/v1/empresas/{empresaId}/turnos/{turnoId}`
**Obtiene detalle completo de un turno con historial de estados.**

**Roles:** `CLIENTE` (propio), `OPERADOR`, `ADMIN`

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "id":               "d1e2f3a4-...",
    "empresaId":        "550e8400-...",
    "fechaSolicitada":  "2026-04-25",
    "horaSolicitada":   "10:00",
    "fechaConfirmada":  "2026-04-25",
    "horaConfirmada":   "10:00",
    "estado":           "CONFIRMADO",
    "cotizacion": {
      "id":              "cot-uuid-...",
      "precio":          9000.00,
      "duracionMinutos": 90,
      "descripcion":     "Reparación de fuente y limpieza general",
      "estado":          "ACEPTADA"
    },
    "senia": {
      "id":           "sen-uuid-...",
      "monto":        2000.00,
      "metodoPago":   "Transferencia",
      "referencia":   "CVU 0123456789",
      "registradoEn": "2026-04-14T16:00:00Z"
    },
    "historial": [
      {
        "estadoAnterior": null,
        "estadoNuevo":    "SOLICITADO",
        "motivo":         null,
        "cambiadoPor":    "María García",
        "timestamp":      "2026-04-14T15:30:00Z"
      },
      {
        "estadoAnterior": "SOLICITADO",
        "estadoNuevo":    "EN_COTIZACION",
        "motivo":         null,
        "cambiadoPor":    "Carlos Operador",
        "timestamp":      "2026-04-14T15:45:00Z"
      }
    ]
  }
}
```

---

### `PATCH /api/v1/empresas/{empresaId}/turnos/{turnoId}/estado`
**Cambia el estado del turno siguiendo la máquina de estados.**

**Roles:** `OPERADOR`, `ADMIN`

> ⚠️ Las transiciones inválidas devuelven `409 Conflict`.

**Request Body:**
```json
{
  "nuevoEstado": "EN_COTIZACION",
  "motivo":      "Iniciando revisión del equipo"
}
```

**Transiciones válidas:**

| Desde | Hacia (válidos) |
|---|---|
| `SOLICITADO` | `EN_COTIZACION`, `CANCELADO` |
| `EN_COTIZACION` | `COTIZADO`, `CANCELADO` |
| `COTIZADO` | `CONFIRMADO`, `CANCELADO` |
| `CONFIRMADO` | `PROGRAMADO`, `CANCELADO` |
| `PROGRAMADO` | `FINALIZADO`, `CANCELADO` |
| `FINALIZADO` | — (terminal) |
| `CANCELADO` | — (terminal) |

**Test con curl:**
```bash
curl -X PATCH http://localhost:8080/api/v1/empresas/550e8400.../turnos/d1e2f3a4.../estado \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nuevoEstado": "EN_COTIZACION"}'
```

---

### `PUT /api/v1/empresas/{empresaId}/turnos/{turnoId}/reprogramar`
**Reprograma la fecha y hora de un turno.**

**Roles:** `OPERADOR`, `ADMIN`

**Request Body:**
```json
{
  "nuevaFecha": "2026-04-28",
  "nuevaHora":  "14:00",
  "motivo":     "El técnico solicitó cambio de horario"
}
```

**Respuesta:** `200 OK` con el `TurnoResponse` actualizado. Dispara notificación al cliente.

---

### `DELETE /api/v1/empresas/{empresaId}/turnos/{turnoId}`
**Cancela un turno.**

**Roles:** `CLIENTE` (propio, hasta 48hs antes), `OPERADOR`, `ADMIN`

> ⚠️ Si el cliente intenta cancelar con menos de 48hs de anticipación, devuelve `422 Unprocessable Entity`.

**Query Param (opcional):** `motivo=string`

**Respuesta:** `204 No Content`

```bash
curl -X DELETE "http://localhost:8080/api/v1/empresas/550e8400.../turnos/d1e2f3a4...?motivo=No+puedo+asistir" \
  -H "Authorization: Bearer <token>"
```

---

### `POST /api/v1/empresas/{empresaId}/turnos/{turnoId}/senia`
**Registra el pago de seña de un turno.**

**Roles:** `OPERADOR`, `ADMIN`

**Request Body:**
```json
{
  "monto":      2000.00,
  "metodoPago": "Transferencia",
  "referencia": "CVU 0123456789"
}
```

**Respuesta `200 OK`:** `TurnoResponse` con `senia` completada y estado avanzado a `CONFIRMADO`.

---

### `POST /api/v1/empresas/{empresaId}/turnos/{turnoId}/finalizar`
**Marca el turno como finalizado. Transición `PROGRAMADO → FINALIZADO`.**

**Roles:** `OPERADOR`, `ADMIN`

**Respuesta `200 OK`:** `TurnoResponse` con `estado: "FINALIZADO"`.

```bash
curl -X POST http://localhost:8080/api/v1/empresas/550e8400.../turnos/d1e2f3a4.../finalizar \
  -H "Authorization: Bearer <token>"
```

---

### `GET /api/v1/clientes/me/turnos` 🔒
**Historial de turnos del cliente autenticado (paginado).**

**Roles:** `CLIENTE`

**Query Params:** `page`, `size`, `sort` (default: `creadoEn,desc`)

**Test con curl:**
```bash
curl "http://localhost:8080/api/v1/clientes/me/turnos?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

---

## 6. 💰 Cotizaciones

**Base path:** `/api/v1/empresas/{empresaId}/turnos/{turnoId}/cotizacion`

---

### `POST /api/v1/empresas/{empresaId}/turnos/{turnoId}/cotizacion`
**Crea una cotización para el turno. Requiere que el turno esté en `EN_COTIZACION`.**

**Roles:** `OPERADOR`, `ADMIN`

**Request Body:**
```json
{
  "precio":          9000.00,
  "duracionMinutos": 90,
  "servicioId":      "abc12345-...",
  "descripcion":     "Reparación de fuente y limpieza general. Incluye repuestos."
}
```

**Respuesta `201 Created`:**
```json
{
  "status": "CREATED",
  "message": "Cotización creada. Se notificará al cliente.",
  "data": {
    "id":              "cot-uuid-...",
    "precio":          9000.00,
    "duracionMinutos": 90,
    "descripcion":     "Reparación de fuente y limpieza general. Incluye repuestos.",
    "estado":          "PENDIENTE",
    "creadoEn":        "2026-04-14T16:00:00Z"
  }
}
```

> El estado del turno avanza automáticamente a `COTIZADO` y se notifica al cliente vía WhatsApp y email.

---

### `GET /api/v1/empresas/{empresaId}/turnos/{turnoId}/cotizacion`
**Obtiene la cotización activa del turno.**

**Roles:** `CLIENTE`, `OPERADOR`, `ADMIN`

---

### `PUT /api/v1/empresas/{empresaId}/turnos/{turnoId}/cotizacion`
**Actualiza precio, duración o servicio de la cotización.**

**Roles:** `OPERADOR`, `ADMIN`

**Request Body:**
```json
{
  "precio":          10500.00,
  "duracionMinutos": 120,
  "descripcion":     "Se agrega reemplazo de disco SSD"
}
```

---

### `POST /api/v1/empresas/{empresaId}/turnos/{turnoId}/cotizacion/aceptar`
**El cliente acepta la cotización. Transición: `COTIZADO → CONFIRMADO`.**

**Roles:** `CLIENTE`

**Request Body:** *(vacío)*

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "message": "Cotización aceptada. Proceda con el pago de la seña para confirmar su turno.",
  "data": { "...turnoResponse con estado CONFIRMADO..." }
}
```

```bash
curl -X POST http://localhost:8080/api/v1/empresas/550e8400.../turnos/d1e2f3a4.../cotizacion/aceptar \
  -H "Authorization: Bearer <token>"
```

---

### `POST /api/v1/empresas/{empresaId}/turnos/{turnoId}/cotizacion/rechazar`
**El cliente rechaza la cotización. Transición: `COTIZADO → CANCELADO`.**

**Roles:** `CLIENTE`

**Request Body:** *(vacío)*

**Respuesta `200 OK`:** `TurnoResponse` con `estado: "CANCELADO"`.

---

## 7. 🗓️ Agenda

**Base path:** `/api/v1/empresas/{empresaId}/agenda`

---

### `GET /api/v1/empresas/{empresaId}/agenda/disponibilidad`
**Lista los slots de tiempo disponibles para una fecha y servicio.**

**Roles:** `CLIENTE`, `OPERADOR`, `ADMIN`

**Query Params:**

| Parámetro | Requerido | Descripción |
|---|---|---|
| `fecha` | ✅ | Fecha a consultar (`YYYY-MM-DD`) |
| `servicioId` | ❌ | Filtra según duración del servicio |

**Test con curl:**
```bash
curl "http://localhost:8080/api/v1/empresas/550e8400.../agenda/disponibilidad?fecha=2026-04-25&servicioId=abc12345-..." \
  -H "Authorization: Bearer <token>"
```

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "fecha": "2026-04-25",
    "slots": [
      { "fecha": "2026-04-25", "hora": "08:00", "disponible": true },
      { "fecha": "2026-04-25", "hora": "08:30", "disponible": false },
      { "fecha": "2026-04-25", "hora": "09:00", "disponible": true },
      { "fecha": "2026-04-25", "hora": "09:30", "disponible": true }
    ]
  }
}
```

---

### `GET /api/v1/empresas/{empresaId}/agenda/bloqueos`
**Lista los bloqueos de horario de la empresa.**

**Roles:** `OPERADOR`, `ADMIN`

**Query Params:** `desde` y `hasta` (`YYYY-MM-DD`)

---

### `POST /api/v1/empresas/{empresaId}/agenda/bloqueos`
**Crea un bloqueo manual de horario.**

**Roles:** `OPERADOR`, `ADMIN`

**Request Body:**
```json
{
  "fecha":      "2026-04-26",
  "horaInicio": "13:00",
  "horaFin":    "15:00",
  "motivo":     "Reunión de equipo"
}
```

**Respuesta `201 Created`:**
```json
{
  "status": "CREATED",
  "data": {
    "id":         "blq-uuid-...",
    "fecha":      "2026-04-26",
    "horaInicio": "13:00",
    "horaFin":    "15:00",
    "motivo":     "Reunión de equipo",
    "creadoPor":  "Carlos Operador",
    "creadoEn":   "2026-04-14T10:00:00Z"
  }
}
```

---

### `DELETE /api/v1/empresas/{empresaId}/agenda/bloqueos/{bloqueoId}`
**Elimina un bloqueo de horario.**

**Roles:** `OPERADOR`, `ADMIN`

**Respuesta:** `204 No Content`

---

### `GET /api/v1/empresas/{empresaId}/agenda/calendario`
**Vista de calendario con turnos y bloqueos del día o semana.**

**Roles:** `OPERADOR`, `ADMIN`

**Query Params:**

| Parámetro | Default | Descripción |
|---|---|---|
| `fecha` | ✅ Requerido | Fecha base (`YYYY-MM-DD`) |
| `vista` | `DIA` | `DIA` o `SEMANA` |

**Test con curl:**
```bash
curl "http://localhost:8080/api/v1/empresas/550e8400.../agenda/calendario?fecha=2026-04-20&vista=SEMANA" \
  -H "Authorization: Bearer <token>"
```

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "fecha": "2026-04-20",
    "turnos": [
      {
        "id":             "d1e2f3a4-...",
        "fecha":          "2026-04-20",
        "hora":           "10:00",
        "estado":         "PROGRAMADO",
        "nombreCliente":  "María García",
        "nombreServicio": "Reparación de PC"
      }
    ],
    "bloqueos": [
      {
        "id":         "blq-uuid-...",
        "fecha":      "2026-04-20",
        "horaInicio": "13:00",
        "horaFin":    "15:00",
        "motivo":     "Reunión de equipo"
      }
    ]
  }
}
```

---

## 8. 🔔 Notificaciones

---

### `GET /api/v1/usuarios/me/notificaciones` 🔒
**Lista las notificaciones del usuario autenticado (paginado).**

**Roles:** Cualquier usuario autenticado.

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "content": [
      {
        "id":       "notif-uuid-...",
        "tipo":     "TURNO_CONFIRMADO",
        "titulo":   "Turno confirmado",
        "cuerpo":   "Su turno para el 25/04 a las 10:00 fue confirmado.",
        "leida":    false,
        "creadoEn": "2026-04-14T16:05:00Z"
      }
    ],
    "totalElements": 5
  }
}
```

---

### `PATCH /api/v1/usuarios/me/notificaciones/{id}/leer` 🔒
**Marca una notificación como leída.**

**Roles:** Cualquier usuario autenticado.

**Respuesta:** `204 No Content`

---

### `GET /api/v1/empresas/{empresaId}/notificaciones/config`
**Obtiene la configuración de canales de notificación.**

**Roles:** `ADMIN`

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "whatsappEnabled":    true,
    "emailEnabled":       true,
    "whatsappConfigured": true,
    "gmailConfigured":    false
  }
}
```

---

### `PUT /api/v1/empresas/{empresaId}/notificaciones/config`
**Actualiza tokens y configuración de WhatsApp/Gmail.**

**Roles:** `ADMIN`

**Request Body:**
```json
{
  "whatsappApiToken":    "EAABwzLixnjY...",
  "whatsappPhoneNumberId": "107990345678910",
  "gmailClientId":       "123456789-abc.apps.googleusercontent.com",
  "gmailClientSecret":   "GOCSPX-...",
  "whatsappEnabled":     true,
  "emailEnabled":        true
}
```

---

### `POST /api/v1/empresas/{empresaId}/notificaciones/test`
**Envía una notificación de prueba al canal configurado.**

**Roles:** `ADMIN`

**Query Param:** `canal=WHATSAPP|EMAIL`

**Respuesta:** `202 Accepted`

---

## 9. 📊 Reportes

**Base path:** `/api/v1/empresas/{empresaId}/reportes`

> 🔒 Todos requieren `ADMIN`.

---

### `GET /api/v1/empresas/{empresaId}/reportes/dashboard`
**KPIs principales del negocio.**

**Query Params (opcionales):** `desde`, `hasta` (`YYYY-MM-DD`)

**Respuesta `200 OK`:**
```json
{
  "status": "OK",
  "data": {
    "turnosHoy":        8,
    "turnosMes":        134,
    "turnosCancelados": 12,
    "tasaCancelacion":  8.95,
    "ingresosEstimados": 980500.00,
    "topServicios": [
      { "nombre": "Reparación de PC", "cantidad": 54, "ingresoTotal": 486000.00 },
      { "nombre": "Limpieza general", "cantidad": 31, "ingresoTotal": 217000.00 }
    ]
  }
}
```

---

### `GET /api/v1/empresas/{empresaId}/reportes/turnos`
**Reporte paginado de turnos por rango de fechas.**

**Query Params:** `desde` ✅, `hasta` ✅, `page`, `size`

---

### `GET /api/v1/empresas/{empresaId}/reportes/servicios`
**Servicios más solicitados en el período.**

**Query Params:** `desde` ✅, `hasta` ✅

---

### `GET /api/v1/empresas/{empresaId}/reportes/export`
**Exporta los datos a Excel (XLSX).**

**Query Params:** `desde` ✅, `hasta` ✅

**Produces:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

```bash
curl "http://localhost:8080/api/v1/empresas/550e8400.../reportes/export?desde=2026-04-01&hasta=2026-04-30" \
  -H "Authorization: Bearer <token>" \
  --output reporte-abril.xlsx
```

---

## 10. 📋 Referencia Rápida de Endpoints

| Método | Endpoint | Roles Mínimos |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Público |
| `POST` | `/api/v1/auth/login` | Público |
| `POST` | `/api/v1/auth/refresh` | Público |
| `POST` | `/api/v1/auth/logout` | Autenticado |
| `POST` | `/api/v1/auth/forgot-password` | Público |
| `POST` | `/api/v1/auth/reset-password` | Público |
| `GET` | `/api/v1/empresas` | ADMIN |
| `POST` | `/api/v1/empresas` | ADMIN |
| `GET` | `/api/v1/empresas/{id}` | OPERADOR+ |
| `PUT` | `/api/v1/empresas/{id}` | ADMIN |
| `PATCH` | `/api/v1/empresas/{id}/config` | ADMIN |
| `DELETE` | `/api/v1/empresas/{id}` | ADMIN |
| `GET` | `/api/v1/empresas/{id}/usuarios` | ADMIN |
| `POST` | `/api/v1/empresas/{id}/usuarios` | ADMIN |
| `GET` | `/api/v1/usuarios/me` | Autenticado |
| `PUT` | `/api/v1/usuarios/me` | Autenticado |
| `PATCH` | `/api/v1/usuarios/{id}/rol` | ADMIN |
| `DELETE` | `/api/v1/usuarios/{id}` | ADMIN |
| `GET` | `/api/v1/empresas/{id}/servicios` | CLIENTE+ |
| `POST` | `/api/v1/empresas/{id}/servicios` | ADMIN |
| `PUT` | `/api/v1/empresas/{id}/servicios/{id}` | ADMIN |
| `PATCH` | `/api/v1/empresas/{id}/servicios/{id}/estado` | ADMIN |
| `GET` | `/api/v1/empresas/{id}/turnos` | OPERADOR+ |
| `POST` | `/api/v1/empresas/{id}/turnos` | CLIENTE |
| `GET` | `/api/v1/empresas/{id}/turnos/{id}` | CLIENTE+ |
| `PATCH` | `/api/v1/empresas/{id}/turnos/{id}/estado` | OPERADOR+ |
| `PUT` | `/api/v1/empresas/{id}/turnos/{id}/reprogramar` | OPERADOR+ |
| `DELETE` | `/api/v1/empresas/{id}/turnos/{id}` | CLIENTE+ |
| `POST` | `/api/v1/empresas/{id}/turnos/{id}/senia` | OPERADOR+ |
| `POST` | `/api/v1/empresas/{id}/turnos/{id}/finalizar` | OPERADOR+ |
| `GET` | `/api/v1/clientes/me/turnos` | CLIENTE |
| `POST` | `/api/v1/empresas/{id}/turnos/{id}/cotizacion` | OPERADOR+ |
| `GET` | `/api/v1/empresas/{id}/turnos/{id}/cotizacion` | CLIENTE+ |
| `PUT` | `/api/v1/empresas/{id}/turnos/{id}/cotizacion` | OPERADOR+ |
| `POST` | `/api/v1/empresas/{id}/turnos/{id}/cotizacion/aceptar` | CLIENTE |
| `POST` | `/api/v1/empresas/{id}/turnos/{id}/cotizacion/rechazar` | CLIENTE |
| `GET` | `/api/v1/empresas/{id}/agenda/disponibilidad` | CLIENTE+ |
| `GET` | `/api/v1/empresas/{id}/agenda/bloqueos` | OPERADOR+ |
| `POST` | `/api/v1/empresas/{id}/agenda/bloqueos` | OPERADOR+ |
| `DELETE` | `/api/v1/empresas/{id}/agenda/bloqueos/{id}` | OPERADOR+ |
| `GET` | `/api/v1/empresas/{id}/agenda/calendario` | OPERADOR+ |
| `GET` | `/api/v1/usuarios/me/notificaciones` | Autenticado |
| `PATCH` | `/api/v1/usuarios/me/notificaciones/{id}/leer` | Autenticado |
| `GET` | `/api/v1/empresas/{id}/notificaciones/config` | ADMIN |
| `PUT` | `/api/v1/empresas/{id}/notificaciones/config` | ADMIN |
| `POST` | `/api/v1/empresas/{id}/notificaciones/test` | ADMIN |
| `GET` | `/api/v1/empresas/{id}/reportes/dashboard` | ADMIN |
| `GET` | `/api/v1/empresas/{id}/reportes/turnos` | ADMIN |
| `GET` | `/api/v1/empresas/{id}/reportes/servicios` | ADMIN |
| `GET` | `/api/v1/empresas/{id}/reportes/export` | ADMIN |

---

## 11. 🚨 Errores Comunes

| Código | Significado | Causa habitual |
|---|---|---|
| `400 Bad Request` | Request mal formado | Campo `@NotBlank`, formato de email o contraseña inválida |
| `401 Unauthorized` | No autenticado | Token ausente, expirado o malformado |
| `403 Forbidden` | Sin permisos | El rol del usuario no alcanza para la operación |
| `404 Not Found` | Recurso inexistente | UUID incorrecto o pertenece a otra empresa |
| `409 Conflict` | Transición inválida | Estado del turno no permite esa transición |
| `422 Unprocessable Entity` | Regla de negocio violada | Cancelación con menos de 48hs de anticipación |
| `429 Too Many Requests` | Rate limit excedido | Más de 100 req/min (general) o 10 req/min (auth) por IP |
| `500 Internal Server Error` | Error en servidor | Revisar logs del servicio |

### Estructura de error estándar:
```json
{
  "error":     "VALIDATION_FAILED",
  "message":   "Errores de validación",
  "fields": {
    "email":    "must not be blank",
    "password": "Password must contain uppercase, lowercase, digit and special character"
  },
  "timestamp": "2026-04-14T12:00:00Z"
}
```

### Estructura de error de negocio:
```json
{
  "error":     "INVALID_STATE_TRANSITION",
  "message":   "Cannot transition turno from FINALIZADO to CANCELADO",
  "timestamp": "2026-04-14T12:00:00Z"
}
```

### Rate limit excedido:
```json
{
  "error":   "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Retry after 60 seconds."
}
```
> `Retry-After: 60` también viene en el header de respuesta.

---

*Sistema de Gestión de Turnos y Servicios Técnicos · v1.0.0 · Confidencial*

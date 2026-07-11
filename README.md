# HardAgenda Android

Version Android de [HardAgenda](https://github.com/SpeedMetal444/HardAgenda), un sistema de turnos para consultorios medicos.

## Arquitectura

```
HardAgenda Desktop (PC Recepcion)  ─┐
                                    ├──> FastAPI (server.py) ──> PostgreSQL
HardAgenda Android (Celular)       ─┘
```

El servidor FastAPI actua como puente HTTP entre Android/Desktop y PostgreSQL. Android no puede conectarse directamente a PostgreSQL porque el driver JDBC no funciona en Android.

## Descargas

En la [seccion de releases](https://github.com/SpeedMetal444/HardAgenda-Android/releases) encontraras:

- **HardAgendaServer.exe** - Servidor para ejecutar en la PC con PostgreSQL
- **app-release.apk** - Aplicacion Android para instalar en el celular

## Requisitos

- **PC Windows** con PostgreSQL instalado (la que tiene la BD)
- **Celular Android**
- Python 3.10+ en la PC con PostgreSQL

## Instalacion

### 1. Servidor API (PC con PostgreSQL)

Opcion A - Ejecutar el `.exe` directamente:
```
HardAgendaServer.exe
```

Opcion B - Ejecutar desde Python:
```bash
cd "HardAgenda Android"
python -m venv venv
venv\Scripts\activate
pip install psycopg2-binary fastapi uvicorn
python server.py
```

El servidor queda disponible en `http://0.0.0.0:8080`.

### 2. Instalar la app Android

Transfiere `app-release.apk` al celular e instalalo. Habilita la instalacion de apps desconocidas si es necesario.

### 3. Configurar el acceso

En el login de la app, ingresa:
- **IP del servidor**: `192.168.X.X` (IP local de la PC con PostgreSQL)
- **Puerto**: `8080` (por defecto)
- **Nombre de la base de datos**: el nombre de tu BD
- **Usuario de PostgreSQL**: ej: `postgres`
- **Contrasena de PostgreSQL**: tu contrasena
- Marca **"Crear base de datos"** si es la primera vez

Las tablas se crean automaticamente al conectarse.

### 4. Acceso desde fuera de la red (por internet)

Para que funcione desde datos moviles o WiFi de otro lugar:

1. Busca tu **IP publica**: desde la PC con PostgreSQL, abri el navegador y busca "que es mi IP"
2. Configura **port forwarding** en el router:
   - Entra al panel del router (generalmente `192.168.1.1` o `192.168.0.1`)
   - Busca "Port Forwarding" o "Virtual Server"
   - Agrega una regla: puerto externo `8080` -> IP interna de la PC, puerto `8080`
3. En el login de la app, usa la **IP publica** en vez de la IP local

**Nota**: solo se expone el puerto 8080. PostgreSQL (puerto 5432) queda solo en la PC.

## Endpoints de la API

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/api/ping` | Verificar que el server esta activo |
| GET | `/api/test` | Probar conexion a PostgreSQL |
| GET | `/api/crear_db` | Crear la base de datos |
| GET | `/api/crear_tablas` | Crear las tablas turnos y historial_cambios |
| GET | `/api/turnos/hoy` | Obtener turnos del dia |
| GET | `/api/turnos/todos` | Obtener todos los turnos |
| GET | `/api/historial` | Obtener historial de cambios |
| POST | `/api/turnos/buscar` | Buscar turnos por DNI, nombre o apellido |
| POST | `/api/turnos/registrar` | Registrar un nuevo turno |
| POST | `/api/turnos/avanzar` | Marcar turno como atendido |
| POST | `/api/turnos/editar` | Editar un turno |
| POST | `/api/turnos/reprogramar` | Reprogramar un turno |
| POST | `/api/turnos/eliminar` | Eliminar un turno |
| POST | `/api/historial/registrar` | Registrar un cambio en el historial |

Todos los endpoints reciben las credenciales de PostgreSQL via headers HTTP:
- `X-DB-User`: usuario de PostgreSQL
- `X-DB-Pass`: contrasena de PostgreSQL
- `X-DB-Name`: nombre de la base de datos

Documentacion interactiva (Swagger): `http://localhost:8080/docs`

## Funcionalidades Android

- **Hoy**: turnos del dia actual con resaltado verde
- **Nuevo turno**: formulario de registro con selector de fecha y hora
- **Todos los turnos**: busqueda por DNI, nombre o apellido con menu contextual
- **Historial**: registro de cambios realizados
- **Acerca de**: informacion de la version

## Colores

Misma paleta que la version de escritorio:

- Fondo: `#1a1a1a`
- Superficie: `#242424`
- Verde primario: `#28a745`
- Verde oscuro: `#155724`
- Texto gris: `#6b7280`

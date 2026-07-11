"""
HardAgenda - Servidor API para Android
Ejecutar en la PC Windows: python server.py
"""
import json
import os
import configparser
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from datetime import date, datetime

import psycopg2
from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
from psycopg2 import sql

CONFIG_PATH = os.path.join(os.path.expandvars('%APPDATA%'), 'HardAgenda', 'config.ini')


def get_db_config():
    if os.path.exists(CONFIG_PATH):
        config = configparser.ConfigParser()
        config.read(CONFIG_PATH, encoding='utf-8')
        if config.has_section('database'):
            return {
                'dbname': config.get('database', 'database', fallback='hardagenda_db'),
                'user': config.get('database', 'user', fallback=''),
                'password': config.get('database', 'password', fallback=''),
                'host': config.get('database', 'host', fallback='localhost'),
                'port': config.get('database', 'port', fallback='5432'),
            }
    return None


def connect_db(cfg):
    return psycopg2.connect(**cfg)


def row_to_dict(cursor, row):
    if row is None:
        return None
    cols = [d[0] for d in cursor.description]
    d = {}
    for i, col in enumerate(cols):
        val = row[i]
        if isinstance(val, (date, datetime)):
            val = val.isoformat()
        d[col] = val
    return d


def rows_to_list(cursor, rows):
    return [row_to_dict(cursor, r) for r in rows]


class HardAgendaHandler(BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        pass

    def _get_db_config(self):
        cfg = get_db_config() or {}
        user = self.headers.get('X-DB-User', cfg.get('user', ''))
        password = self.headers.get('X-DB-Pass', cfg.get('password', ''))
        dbname = self.headers.get('X-DB-Name', cfg.get('dbname', 'hardagenda_db'))
        host = cfg.get('host', 'localhost')
        port = cfg.get('port', '5432')
        return {'dbname': dbname, 'user': user, 'password': password, 'host': host, 'port': port}

    def _send_json(self, data, status=200):
        body = json.dumps(data, default=str, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self):
        length = int(self.headers.get('Content-Length', 0))
        if length == 0:
            return {}
        return json.loads(self.rfile.read(length))

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_GET(self):
        path = urlparse(self.path).path
        params = parse_qs(urlparse(self.path).query)

        try:
            if path == '/api/ping':
                self._send_json({'status': 'ok'})

            elif path == '/api/test':
                try:
                    cfg = self._get_db_config()
                    conn = connect_db(cfg)
                    conn.close()
                    self._send_json({'status': 'ok', 'message': 'Conexion exitosa'})
                except Exception as e:
                    self._send_json({'status': 'error', 'message': str(e)}, 400)

            elif path == '/api/crear_db':
                cfg = self._get_db_config()
                conn = psycopg2.connect(
                    dbname='postgres', user=cfg['user'], password=cfg['password'],
                    host=cfg['host'], port=cfg['port']
                )
                conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
                cur = conn.cursor()
                cur.execute("SELECT 1 FROM pg_database WHERE datname=%s;", (cfg['dbname'],))
                if not cur.fetchone():
                    cur.execute(sql.SQL("CREATE DATABASE {};").format(sql.Identifier(cfg['dbname'])))
                cur.close()
                conn.close()
                self._send_json({'status': 'ok', 'message': 'Base de datos creada/verificada'})

            elif path == '/api/crear_tablas':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS turnos (
                        id SERIAL PRIMARY KEY,
                        nombre VARCHAR(100) NOT NULL,
                        apellido VARCHAR(100) NOT NULL,
                        dni VARCHAR(20) NOT NULL,
                        obra_social VARCHAR(100),
                        motivo_consulta TEXT,
                        fecha DATE DEFAULT CURRENT_DATE,
                        hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        estado VARCHAR(20) DEFAULT 'pendiente',
                        usuario VARCHAR(100)
                    )
                """)
                cur.execute("""
                    CREATE TABLE IF NOT EXISTS historial_cambios (
                        id SERIAL PRIMARY KEY,
                        tabla VARCHAR(50) NOT NULL,
                        registro_id INTEGER,
                        accion VARCHAR(50) NOT NULL,
                        detalle TEXT,
                        usuario VARCHAR(100),
                        dni VARCHAR(20),
                        nombre VARCHAR(100),
                        apellido VARCHAR(100),
                        fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                for col in ['usuario', 'dni', 'nombre', 'apellido']:
                    cur.execute(f"ALTER TABLE historial_cambios ADD COLUMN IF NOT EXISTS {col} VARCHAR(100)")
                conn.commit()
                cur.close()
                conn.close()
                self._send_json({'status': 'ok', 'message': 'Tablas creadas/verificadas'})

            elif path == '/api/turnos/hoy':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("SELECT * FROM turnos WHERE fecha = %s ORDER BY hora ASC", (date.today(),))
                rows = rows_to_list(cur, cur.fetchall())
                cur.close()
                conn.close()
                self._send_json({'status': 'ok', 'data': rows})

            elif path == '/api/turnos/todos':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("SELECT * FROM turnos ORDER BY fecha DESC, hora DESC")
                rows = rows_to_list(cur, cur.fetchall())
                cur.close()
                conn.close()
                self._send_json({'status': 'ok', 'data': rows})

            elif path == '/api/historial':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("""
                    SELECT id, tabla, registro_id, accion, detalle, usuario, dni, nombre, apellido, fecha
                    FROM historial_cambios ORDER BY fecha DESC LIMIT 200
                """)
                rows = rows_to_list(cur, cur.fetchall())
                cur.close()
                conn.close()
                self._send_json({'status': 'ok', 'data': rows})

            else:
                self._send_json({'status': 'error', 'message': 'Endpoint no encontrado'}, 404)

        except Exception as e:
            self._send_json({'status': 'error', 'message': str(e)}, 500)

    def do_POST(self):
        path = urlparse(self.path).path

        try:
            body = self._read_body()

            if path == '/api/turnos/buscar':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                conditions = []
                params = []
                if body.get('dni'):
                    conditions.append("dni ILIKE %s")
                    params.append(f"%{body['dni']}%")
                if body.get('nombre'):
                    conditions.append("nombre ILIKE %s")
                    params.append(f"%{body['nombre']}%")
                if body.get('apellido'):
                    conditions.append("apellido ILIKE %s")
                    params.append(f"%{body['apellido']}%")
                if not conditions:
                    self._send_json({'status': 'error', 'message': 'Ingrese al menos un criterio'}, 400)
                    return
                where = " AND ".join(conditions)
                cur.execute(f"SELECT * FROM turnos WHERE {where} ORDER BY fecha DESC, hora DESC", params)
                rows = rows_to_list(cur, cur.fetchall())
                cur.close()
                conn.close()
                self._send_json({'status': 'ok', 'data': rows})

            elif path == '/api/turnos/registrar':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("""
                    INSERT INTO turnos (nombre, apellido, dni, obra_social, motivo_consulta, fecha, hora, usuario)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
                """, (
                    body['nombre'], body['apellido'], body['dni'],
                    body.get('obra_social') or None, body.get('motivo') or None,
                    body.get('fecha'), body.get('hora'),
                    body.get('usuario')
                ))
                new_id = cur.fetchone()[0]
                conn.commit()
                cur.close()
                conn.close()
                self._send_json({'status': 'ok', 'id': new_id})

            elif path == '/api/turnos/avanzar':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("UPDATE turnos SET estado='atendido' WHERE id=%s", (body['id'],))
                conn.commit()
                cur.close()
                conn.close()
                self._send_json({'status': 'ok'})

            elif path == '/api/turnos/editar':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("""
                    UPDATE turnos SET nombre=%s, apellido=%s, dni=%s, obra_social=%s,
                    motivo_consulta=%s, fecha=%s, hora=%s WHERE id=%s
                """, (
                    body['nombre'], body['apellido'], body['dni'],
                    body.get('obra_social') or None, body.get('motivo') or None,
                    body.get('fecha'), body.get('hora'), body['id']
                ))
                conn.commit()
                cur.close()
                conn.close()
                self._send_json({'status': 'ok'})

            elif path == '/api/turnos/reprogramar':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("""
                    UPDATE turnos SET fecha=%s, hora=%s, estado='pendiente' WHERE id=%s
                """, (body['fecha'], body['hora'], body['id']))
                conn.commit()
                cur.close()
                conn.close()
                self._send_json({'status': 'ok'})

            elif path == '/api/turnos/eliminar':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("DELETE FROM turnos WHERE id=%s", (body['id'],))
                conn.commit()
                cur.close()
                conn.close()
                self._send_json({'status': 'ok'})

            elif path == '/api/historial/registrar':
                conn = connect_db(self._get_db_config())
                cur = conn.cursor()
                cur.execute("""
                    INSERT INTO historial_cambios (tabla, registro_id, accion, detalle, usuario, dni, nombre, apellido)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """, (
                    body.get('tabla'), body.get('registro_id'), body['accion'],
                    body.get('detalle'), body.get('usuario'),
                    body.get('dni'), body.get('nombre'), body.get('apellido')
                ))
                conn.commit()
                cur.close()
                conn.close()
                self._send_json({'status': 'ok'})

            else:
                self._send_json({'status': 'error', 'message': 'Endpoint no encontrado'}, 404)

        except Exception as e:
            self._send_json({'status': 'error', 'message': str(e)}, 500)


if __name__ == '__main__':
    PORT = 8080
    server = HTTPServer(('0.0.0.0', PORT), HardAgendaHandler)
    print(f"HardAgenda Server corriendo en http://0.0.0.0:{PORT}")
    print(f"Desde Android, usa: http://192.168.0.82:{PORT}")
    print("Presiona Ctrl+C para detener")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServidor detenido.")
        server.server_close()

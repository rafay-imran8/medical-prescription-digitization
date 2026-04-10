import psycopg2
from psycopg2 import pool

class DatabaseConnection:
    _connection_pool = None

    @classmethod
    def initialize_pool(cls, host, database, user, password, port=5432):
        if cls._connection_pool is None:
            cls._connection_pool = pool.ThreadedConnectionPool(
                minconn=2,
                maxconn=20,
                host=host,
                database=database,
                user=user,
                password=password,
                port=port,
                keepalives=1,
                keepalives_idle=30,
                keepalives_interval=10,
                keepalives_count=5
            )

    @classmethod
    def get_connection(cls):
        if cls._connection_pool is None:
            raise RuntimeError("DB pool not initialized")
        conn = cls._connection_pool.getconn()
        conn.autocommit = False
        return conn

    @classmethod
    def return_connection(cls, conn):
        if cls._connection_pool and conn:
            cls._connection_pool.putconn(conn)

    @classmethod
    def close_all_connections(cls):
        if cls._connection_pool:
            cls._connection_pool.closeall()
            cls._connection_pool = None


def get_connection():
    return DatabaseConnection.get_connection()


def return_connection(conn):
    DatabaseConnection.return_connection(conn)

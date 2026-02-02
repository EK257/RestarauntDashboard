package dao;

import java.sql.*;

public class Database {

    private static final String URL = "jdbc:sqlite:restaurant.db";

    static {
        init();
    }
    public static Connection getConnection() {
        try {
            String url = URL + "?journal_mode=WAL&synchronous=NORMAL&locking_mode=NORMAL";
            return DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            createTimeFunction(conn);

            stmt.execute("CREATE TABLE IF NOT EXISTS zones (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS tables (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "capacity INTEGER, " +
                    "zone TEXT, " +
                    "status TEXT DEFAULT 'Свободен')");

            stmt.execute("CREATE TABLE IF NOT EXISTS clients (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT)");

            try {
                ResultSet tables = conn.getMetaData().getTables(null, null, "reservations", null);
                if (!tables.next()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "client_id INTEGER, " +
                            "table_id INTEGER, " +
                            "date TEXT NOT NULL, " +
                            "start_time TEXT NOT NULL, " +
                            "end_time TEXT NOT NULL, " +
                            "duration INTEGER NOT NULL, " +
                            "guests INTEGER NOT NULL, " +
                            "status TEXT DEFAULT 'Подтверждено', " +
                            "FOREIGN KEY (client_id) REFERENCES clients(id), " +
                            "FOREIGN KEY (table_id) REFERENCES tables(id)" +
                            ")");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void createTimeFunction(Connection conn) {
        try {
            conn.createStatement().execute(
                    "CREATE TEMPORARY VIEW IF NOT EXISTS time_minutes AS " +
                            "SELECT 'time_to_minutes' as func"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
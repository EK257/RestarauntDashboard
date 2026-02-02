package dao;

import model.TableEntity;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableDAO {
    public static List<TableEntity> getAllTables() {
        List<TableEntity> list = new ArrayList<>();
        String sql = "SELECT * FROM tables ORDER BY zone, capacity";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new TableEntity(
                        rs.getInt("id"),
                        rs.getInt("capacity"),
                        rs.getString("zone"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAllZones() {
        List<String> zones = new ArrayList<>();
        String sql = "SELECT DISTINCT zone FROM tables WHERE zone IS NOT NULL AND zone != '' ORDER BY zone";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                zones.add(rs.getString("zone"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zones;
    }

    public static boolean addTable(int capacity, String zone, String status) {
        if (capacity <= 0) return false;

        String sql = "INSERT INTO tables (capacity, zone, status) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, capacity);
            ps.setString(2, zone);
            ps.setString(3, status);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Map<String, Object> getTableStatistics() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN status = 'Свободен' THEN 1 ELSE 0 END) as free, " +
                "SUM(CASE WHEN status = 'Занят' THEN 1 ELSE 0 END) as occupied, " +
                "SUM(CASE WHEN status = 'Забронирован' THEN 1 ELSE 0 END) as reserved, " +
                "SUM(CASE WHEN status = 'На ремонте' THEN 1 ELSE 0 END) as maintenance " +
                "FROM tables";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.put("total", rs.getInt("total"));
                stats.put("free", rs.getInt("free"));
                stats.put("occupied", rs.getInt("occupied"));
                stats.put("reserved", rs.getInt("reserved"));
                stats.put("maintenance", rs.getInt("maintenance"));

                int totalActive = rs.getInt("total") - rs.getInt("maintenance");
                int busy = rs.getInt("occupied") + rs.getInt("reserved");
                double loadPercentage = totalActive > 0 ? (busy * 100.0 / totalActive) : 0;

                if (loadPercentage > 100) loadPercentage = 100;

                stats.put("loadPercentage", loadPercentage);
                stats.put("totalActive", totalActive);
                stats.put("busy", busy);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stats;
    }

    public static boolean updateTable(int id, int capacity, String zone, String status) {
        String sql = "UPDATE tables SET capacity = ?, zone = ?, status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, capacity);
            ps.setString(2, zone);
            ps.setString(3, status);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteTable(int tableId) {
        if (hasActiveReservations(tableId)) {
            return false;
        }

        String sql = "DELETE FROM tables WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean hasActiveReservations(int tableId) {
        String sql = "SELECT COUNT(*) as count FROM reservations " +
                "WHERE table_id = ? AND status != 'Отменено' AND date >= date('now')";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean addZone(String zoneName) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS zones (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL" +
                    ")");

            String sql = "INSERT OR IGNORE INTO zones (name) VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, zoneName);
                return ps.executeUpdate() > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean deleteZone(String zoneName) {
        try (Connection conn = Database.getConnection()) {

            String checkSql = "SELECT COUNT(*) as count FROM tables WHERE zone = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, zoneName);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt("count") > 0) {
                    return false;
                }
            }

            String deleteSql = "DELETE FROM zones WHERE name = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, zoneName);
                return ps.executeUpdate() > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hasTablesInZone(String zoneName) {
        String sql = "SELECT COUNT(*) as count FROM tables WHERE zone = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, zoneName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void updateTableStatus(int tableId, String status) {
        String sql = "UPDATE tables SET status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, tableId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getTableInfo(int tableId) {
        String sql = "SELECT id, zone, capacity FROM tables WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return "Столик №" + rs.getInt("id") + ": " + rs.getString("zone") +
                        " (" + rs.getInt("capacity") + " мест)";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Неизвестный столик";
    }

    public static List<String> getZonesFromZonesTable() {
        List<String> zones = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet tables = conn.getMetaData().getTables(null, null, "zones", null);
            if (tables.next()) {
                String sql = "SELECT name FROM zones ORDER BY name";
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        zones.add(rs.getString("name"));
                    }
                }
            }

            if (zones.isEmpty()) {
                zones = getAllZones();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return zones;
    }
}
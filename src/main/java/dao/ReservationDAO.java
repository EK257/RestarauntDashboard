package dao;

import model.Reservation;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public static List<Reservation> getAllReservations() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.id, c.name, r.date, r.start_time, r.duration, r.guests, r.status, r.table_id " +
                "FROM reservations r " +
                "JOIN clients c ON r.client_id = c.id " +
                "ORDER BY r.date DESC, r.start_time DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int tableId = rs.getInt("table_id");
                String tableInfo = TableDAO.getTableInfo(tableId);

                list.add(new Reservation(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("date"),
                        rs.getString("start_time"),
                        rs.getInt("duration"),
                        rs.getInt("guests"),
                        rs.getString("status"),
                        tableInfo
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<Reservation> getReservationsByDate(String date) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.id, c.name, r.date, r.start_time, r.duration, r.guests, r.status, r.table_id " +
                "FROM reservations r " +
                "JOIN clients c ON r.client_id = c.id " +
                "WHERE r.date = ? " +
                "ORDER BY r.start_time";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int tableId = rs.getInt("table_id");
                String tableInfo = TableDAO.getTableInfo(tableId);

                list.add(new Reservation(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("date"),
                        rs.getString("start_time"),
                        rs.getInt("duration"),
                        rs.getInt("guests"),
                        rs.getString("status"),
                        tableInfo
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAvailableTables(String date, String startTime, int duration, int guests) {
        List<String> tables = new ArrayList<>();

        String sql = "SELECT t.id, t.zone, t.capacity " +
                "FROM tables t " +
                "WHERE t.capacity >= ? AND t.status != 'На ремонте' " +
                "ORDER BY t.capacity, t.zone";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, guests);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int tableId = rs.getInt("id");
                if (isTableAvailable(tableId, date, startTime, duration)) {
                    String zone = rs.getString("zone");
                    int capacity = rs.getInt("capacity");
                    tables.add(String.format("%d - %s (%d мест)", tableId, zone, capacity));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tables;
    }

    public static List<String> getAvailableTablesForEdit(String date, String startTime, int duration, int guests, int excludeReservationId) {
        List<String> tables = new ArrayList<>();

        String sql = "SELECT t.id, t.zone, t.capacity " +
                "FROM tables t " +
                "WHERE t.capacity >= ? AND t.status != 'На ремонте' " +
                "ORDER BY t.capacity, t.zone";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, guests);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int tableId = rs.getInt("id");
                if (isTableAvailableForEdit(tableId, date, startTime, duration, excludeReservationId)) {
                    String zone = rs.getString("zone");
                    int capacity = rs.getInt("capacity");
                    tables.add(String.format("%d - %s (%d мест)", tableId, zone, capacity));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tables;
    }

    private static boolean isTableAvailable(int tableId, String date, String startTime, int duration) {
        String sql = "SELECT start_time, duration FROM reservations " +
                "WHERE table_id = ? AND date = ? AND status IN ('Подтверждено', 'Активно')";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();

            int ourStart = timeToMinutes(startTime);
            int ourEnd = ourStart + duration;

            while (rs.next()) {
                String dbStart = rs.getString("start_time");
                int dbDuration = rs.getInt("duration");

                int dbStartMin = timeToMinutes(dbStart);
                int dbEndMin = dbStartMin + dbDuration;

                if (ourStart < dbEndMin && ourEnd > dbStartMin) {
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isTableAvailableForEdit(int tableId, String date, String startTime,
                                                  int duration, int excludeReservationId) {
        String sql = "SELECT start_time, duration FROM reservations " +
                "WHERE table_id = ? AND date = ? AND id != ? AND status IN ('Подтверждено', 'Активно')";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ps.setString(2, date);
            ps.setInt(3, excludeReservationId);
            ResultSet rs = ps.executeQuery();

            int ourStart = timeToMinutes(startTime);
            int ourEnd = ourStart + duration;

            while (rs.next()) {
                String dbStart = rs.getString("start_time");
                int dbDuration = rs.getInt("duration");

                int dbStartMin = timeToMinutes(dbStart);
                int dbEndMin = dbStartMin + dbDuration;

                if (ourStart < dbEndMin && ourEnd > dbStartMin) {
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addReservationWithStatus(int clientId, int tableId, String date,
                                                   String startTime, int duration, int guests, String status) {
        try (Connection conn = Database.getConnection()) {

            if (!isTableAvailable(tableId, date, startTime, duration)) {
                return false;
            }

            String endTime = calculateEndTime(startTime, duration);

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reservations (client_id, table_id, date, start_time, end_time, duration, guests, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            ps.setInt(1, clientId);
            ps.setInt(2, tableId);
            ps.setString(3, date);
            ps.setString(4, startTime);
            ps.setString(5, endTime);
            ps.setInt(6, duration);
            ps.setInt(7, guests);
            ps.setString(8, status);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                updateTableStatusBasedOnReservation(tableId, status, date, startTime);
                return true;
            }

            return false;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint")) {
                System.out.println("Дублирующая бронь!");
            }
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateReservation(int reservationId, int tableId, String date,
                                            String startTime, int duration, int guests, String status) {
        try (Connection conn = Database.getConnection()) {

            int currentTableId = getTableIdForReservation(reservationId);
            String currentDate = getReservationDate(reservationId);
            String currentStartTime = getReservationStartTime(reservationId);
            int currentDuration = getReservationDuration(reservationId);

            boolean timeChanged = !date.equals(currentDate) || !startTime.equals(currentStartTime) || duration != currentDuration;
            boolean tableChanged = currentTableId != tableId;

            if (timeChanged || tableChanged) {
                if (!isTableAvailableForEdit(tableId, date, startTime, duration, reservationId)) {
                    return false;
                }
            }

            String endTime = calculateEndTime(startTime, duration);

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE reservations SET table_id = ?, date = ?, start_time = ?, " +
                            "end_time = ?, duration = ?, guests = ?, status = ? WHERE id = ?");

            ps.setInt(1, tableId);
            ps.setString(2, date);
            ps.setString(3, startTime);
            ps.setString(4, endTime);
            ps.setInt(5, duration);
            ps.setInt(6, guests);
            ps.setString(7, status);
            ps.setInt(8, reservationId);

            boolean success = ps.executeUpdate() > 0;

            if (success) {
                updateTableStatusBasedOnReservation(tableId, status, date, startTime);

                if (tableChanged && currentTableId > 0) {
                    if (!hasActiveReservations(currentTableId)) {
                        TableDAO.updateTableStatus(currentTableId, "Свободен");
                    }
                }
            }

            return success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateReservationStatus(int reservationId, String newStatus) {
        try (Connection conn = Database.getConnection()) {

            PreparedStatement selectPs = conn.prepareStatement(
                    "SELECT table_id, date, start_time FROM reservations WHERE id = ?");
            selectPs.setInt(1, reservationId);
            ResultSet rs = selectPs.executeQuery();

            if (rs.next()) {
                int tableId = rs.getInt("table_id");

                PreparedStatement updatePs = conn.prepareStatement(
                        "UPDATE reservations SET status = ? WHERE id = ?");
                updatePs.setString(1, newStatus);
                updatePs.setInt(2, reservationId);

                boolean success = updatePs.executeUpdate() > 0;

                if (success) {
                    if (newStatus.equals("Активно")) {
                        TableDAO.updateTableStatus(tableId, "Занят");
                    } else if (newStatus.equals("Завершено") || newStatus.equals("Отменено") || newStatus.equals("Неявка")) {
                        if (!hasActiveReservations(tableId)) {
                            TableDAO.updateTableStatus(tableId, "Свободен");
                        }
                    }
                }

                return success;
            }
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteReservation(int reservationId) {
        int tableId = getTableIdForReservation(reservationId);

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reservations WHERE id = ?")) {

            ps.setInt(1, reservationId);
            boolean success = ps.executeUpdate() > 0;

            if (success && tableId > 0) {
                if (!hasActiveReservations(tableId)) {
                    TableDAO.updateTableStatus(tableId, "Свободен");
                }
            }
            return success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int createOrGetClient(String clientName) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM clients WHERE name = ?")) {

            ps.setString(1, clientName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO clients (name) VALUES (?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, clientName);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static boolean hasActiveReservations(int tableId) {
        String sql = "SELECT COUNT(*) as count FROM reservations " +
                "WHERE table_id = ? AND status IN ('Подтверждено', 'Активно')";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tableId);
            ResultSet rs = ps.executeQuery();

            return rs.next() && rs.getInt("count") > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    static void updateTableStatusBasedOnReservation(int tableId, String status, String date, String time) {
        if (status.equals("Активно")) {
            TableDAO.updateTableStatus(tableId, "Занят");
        } else if (status.equals("Завершено") || status.equals("Отменено") || status.equals("Неявка")) {
            if (!hasActiveReservations(tableId)) {
                TableDAO.updateTableStatus(tableId, "Свободен");
            }
        } else if (status.equals("Подтверждено")) {
            LocalDateTime now = LocalDateTime.now();
            String currentDate = now.toLocalDate().toString();

            if (date.equals(currentDate)) {
                int reservationStart = timeToMinutes(time);
                int currentMinutes = now.getHour() * 60 + now.getMinute();

                if (Math.abs(reservationStart - currentMinutes) <= 30) {
                    TableDAO.updateTableStatus(tableId, "Занят");
                } else {
                    TableDAO.updateTableStatus(tableId, "Забронирован");
                }
            } else {
                TableDAO.updateTableStatus(tableId, "Забронирован");
            }
        }
    }

    private static int getTableIdForReservation(int reservationId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT table_id FROM reservations WHERE id = ?")) {

            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("table_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static String getReservationDate(int reservationId) {
        String sql = "SELECT date FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("date");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String getReservationStartTime(int reservationId) {
        String sql = "SELECT start_time FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("start_time");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static int getReservationDuration(int reservationId) {
        String sql = "SELECT duration FROM reservations WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("duration");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static String calculateEndTime(String startTime, int duration) {
        try {
            String[] parts = startTime.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            int totalMinutes = hours * 60 + minutes + duration;
            int endHours = totalMinutes / 60;
            int endMinutes = totalMinutes % 60;

            return String.format("%02d:%02d", endHours, endMinutes);
        } catch (Exception e) {
            return "23:59";
        }
    }

    private static int timeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (Exception e) {
            return 0;
        }
    }
}
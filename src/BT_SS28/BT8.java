package BT_SS28;

import java.sql.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BT8 {
    // Thông tin kết nối database
    private static final String DB_URL = "jdbc:mysql://localhost:3306/user";
    private static final String USER = "root";
    private static final String PASS = "13012005";

    public static void main(String[] args) {
        // Tạo đối tượng Scanner để nhập liệu
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n==== HỆ THỐNG ĐẶT PHÒNG KHÁCH SẠN ====");
            System.out.println("1. Hiển thị danh sách phòng trống");
            System.out.println("2. Đặt phòng");
            System.out.println("3. Mô phỏng hai người dùng đặt cùng một phòng");
            System.out.println("4. Xem lịch sử đặt phòng");
            System.out.println("5. Xem lịch sử đặt phòng thất bại");
            System.out.println("0. Thoát");
            System.out.print("Chọn chức năng: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Xử lý ký tự xuống dòng

            switch (choice) {
                case 1:
                    showAvailableRooms();
                    break;
                case 2:
                    bookRoom(scanner);
                    break;
                case 3:
                    simulateConcurrentBooking();
                    break;
                case 4:
                    viewBookingHistory();
                    break;
                case 5:
                    viewFailedBookings();
                    break;
                case 0:
                    System.out.println("Cảm ơn bạn đã sử dụng hệ thống!");
                    scanner.close();
                    System.exit(0);
                default:
                    System.out.println("Chức năng không hợp lệ. Vui lòng chọn lại!");
            }
        }
    }

    // Phương thức hiển thị danh sách phòng trống
    private static void showAvailableRooms() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms WHERE availability = TRUE")) {

            System.out.println("\n==== DANH SÁCH PHÒNG TRỐNG ====");
            System.out.printf("%-10s %-15s %-15s%n", "Phòng", "Loại phòng", "Giá (USD)");
            System.out.println("----------------------------------");

            boolean hasRooms = false;
            while (rs.next()) {
                hasRooms = true;
                System.out.printf("%-10d %-15s %-15.2f%n",
                        rs.getInt("room_id"),
                        rs.getString("room_type"),
                        rs.getDouble("price"));
            }

            if (!hasRooms) {
                System.out.println("Không có phòng trống!");
            }

        } catch (SQLException e) {
            System.out.println("Lỗi khi hiển thị danh sách phòng: " + e.getMessage());
        }
    }

    // Phương thức đặt phòng
    private static void bookRoom(Scanner scanner) {
        System.out.println("\n==== ĐẶT PHÒNG ====");

        // Nhập thông tin đặt phòng
        System.out.print("Nhập ID khách hàng: ");
        int customerId = scanner.nextInt();

        System.out.print("Nhập ID phòng muốn đặt: ");
        int roomId = scanner.nextInt();

        // Thực hiện đặt phòng với transaction
        Connection conn = null;
        PreparedStatement checkCustomerStmt = null;
        PreparedStatement checkRoomStmt = null;
        PreparedStatement updateRoomStmt = null;
        PreparedStatement insertBookingStmt = null;
        PreparedStatement logFailureStmt = null;

        try {
            // Tạo kết nối và tắt chế độ auto-commit
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.setAutoCommit(false);

            // 1. Kiểm tra khách hàng có tồn tại không
            checkCustomerStmt = conn.prepareStatement("SELECT * FROM customers WHERE customer_id = ?");
            checkCustomerStmt.setInt(1, customerId);
            ResultSet customerRs = checkCustomerStmt.executeQuery();

            if (!customerRs.next()) {
                // Khách hàng không tồn tại, ghi lại lỗi
                logFailureStmt = conn.prepareStatement(
                        "INSERT INTO failed_bookings (customer_id, room_id, failure_reason) VALUES (?, ?, ?)");
                logFailureStmt.setInt(1, customerId);
                logFailureStmt.setInt(2, roomId);
                logFailureStmt.setString(3, "Khách hàng không tồn tại");
                logFailureStmt.executeUpdate();
                conn.commit();

                System.out.println("Lỗi: Khách hàng không tồn tại trong hệ thống!");
                return;
            }

            // 2. Kiểm tra phòng có tồn tại và trống không
            checkRoomStmt = conn.prepareStatement("SELECT * FROM rooms WHERE room_id = ? AND availability = TRUE FOR UPDATE");
            checkRoomStmt.setInt(1, roomId);
            ResultSet roomRs = checkRoomStmt.executeQuery();

            if (!roomRs.next()) {
                // Phòng không tồn tại hoặc đã được đặt, ghi lại lỗi
                logFailureStmt = conn.prepareStatement(
                        "INSERT INTO failed_bookings (customer_id, room_id, failure_reason) VALUES (?, ?, ?)");
                logFailureStmt.setInt(1, customerId);
                logFailureStmt.setInt(2, roomId);
                logFailureStmt.setString(3, "Phòng không tồn tại hoặc đã được đặt");
                logFailureStmt.executeUpdate();
                conn.commit();

                System.out.println("Lỗi: Phòng không tồn tại hoặc đã được đặt!");
                return;
            }

            // 3. Cập nhật trạng thái phòng thành không còn trống
            updateRoomStmt = conn.prepareStatement("UPDATE rooms SET availability = FALSE WHERE room_id = ?");
            updateRoomStmt.setInt(1, roomId);
            updateRoomStmt.executeUpdate();

            // 4. Thêm thông tin đặt phòng vào bảng bookings
            insertBookingStmt = conn.prepareStatement(
                    "INSERT INTO bookings (customer_id, room_id) VALUES (?, ?)");
            insertBookingStmt.setInt(1, customerId);
            insertBookingStmt.setInt(2, roomId);
            insertBookingStmt.executeUpdate();

            // Commit transaction nếu tất cả thành công
            conn.commit();
            System.out.println("Đặt phòng thành công!");

        } catch (SQLException e) {
            // Rollback transaction nếu có lỗi
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("Lỗi khi rollback: " + ex.getMessage());
            }

            System.out.println("Lỗi khi đặt phòng: " + e.getMessage());

            // Ghi lại lỗi vào bảng failed_bookings
            try {
                if (conn != null) {
                    logFailureStmt = conn.prepareStatement(
                            "INSERT INTO failed_bookings (customer_id, room_id, failure_reason) VALUES (?, ?, ?)");
                    logFailureStmt.setInt(1, customerId);
                    logFailureStmt.setInt(2, roomId);
                    logFailureStmt.setString(3, "Lỗi hệ thống: " + e.getMessage());
                    logFailureStmt.executeUpdate();
                    conn.commit();
                }
            } catch (SQLException ex) {
                System.out.println("Lỗi khi ghi log lỗi: " + ex.getMessage());
            }

        } finally {
            // Đóng tất cả các tài nguyên
            try {
                if (checkCustomerStmt != null) checkCustomerStmt.close();
                if (checkRoomStmt != null) checkRoomStmt.close();
                if (updateRoomStmt != null) updateRoomStmt.close();
                if (insertBookingStmt != null) insertBookingStmt.close();
                if (logFailureStmt != null) logFailureStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println("Lỗi khi đóng tài nguyên: " + e.getMessage());
            }
        }
    }

    // Phương thức mô phỏng hai người dùng đặt cùng một phòng
    private static void simulateConcurrentBooking() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms WHERE availability = TRUE LIMIT 1")) {

            if (!rs.next()) {
                System.out.println("Không có phòng trống để mô phỏng!");
                return;
            }

            final int roomId = rs.getInt("room_id");
            System.out.println("\n==== MÔ PHỎNG ĐẶT PHÒNG ĐỒNG THỜI ====");
            System.out.println("Hai người dùng sẽ đặt cùng phòng " + roomId);

            // Sử dụng ExecutorService để chạy đồng thời hai luồng đặt phòng
            ExecutorService executor = Executors.newFixedThreadPool(2);

            // Luồng người dùng 1 đặt phòng
            executor.submit(() -> {
                bookRoomForSimulation(1, roomId, "Người dùng 1");
            });

            // Đợi một chút để tạo khoảng cách giữa hai lần đặt phòng
            Thread.sleep(100);

            // Luồng người dùng 2 đặt phòng
            executor.submit(() -> {
                bookRoomForSimulation(2, roomId, "Người dùng 2");
            });

            executor.shutdown();

        } catch (SQLException | InterruptedException e) {
            System.out.println("Lỗi khi mô phỏng đặt phòng đồng thời: " + e.getMessage());
        }
    }

    // Phương thức hỗ trợ mô phỏng đặt phòng đồng thời
    private static void bookRoomForSimulation(int customerId, int roomId, String userLabel) {
        Connection conn = null;
        PreparedStatement checkRoomStmt = null;
        PreparedStatement updateRoomStmt = null;
        PreparedStatement insertBookingStmt = null;
        PreparedStatement logFailureStmt = null;

        try {
            System.out.println(userLabel + " đang thử đặt phòng " + roomId);

            // Tạo kết nối và tắt chế độ auto-commit
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.setAutoCommit(false);

            // Kiểm tra phòng có trống không với FOR UPDATE để khóa dòng
            checkRoomStmt = conn.prepareStatement("SELECT * FROM rooms WHERE room_id = ? AND availability = TRUE FOR UPDATE");
            checkRoomStmt.setInt(1, roomId);
            ResultSet rs = checkRoomStmt.executeQuery();

            if (!rs.next()) {
                // Phòng không tồn tại hoặc đã được đặt
                logFailureStmt = conn.prepareStatement(
                        "INSERT INTO failed_bookings (customer_id, room_id, failure_reason) VALUES (?, ?, ?)");
                logFailureStmt.setInt(1, customerId);
                logFailureStmt.setInt(2, roomId);
                logFailureStmt.setString(3, userLabel + ": Phòng không tồn tại hoặc đã được đặt");
                logFailureStmt.executeUpdate();
                conn.commit();

                System.out.println(userLabel + ": Phòng không tồn tại hoặc đã được đặt!");
                return;
            }

            // Mô phỏng thời gian xử lý để tăng khả năng xảy ra xung đột
            Thread.sleep(500);

            // Cập nhật trạng thái phòng
            updateRoomStmt = conn.prepareStatement("UPDATE rooms SET availability = FALSE WHERE room_id = ?");
            updateRoomStmt.setInt(1, roomId);
            updateRoomStmt.executeUpdate();

            // Thêm thông tin đặt phòng
            insertBookingStmt = conn.prepareStatement(
                    "INSERT INTO bookings (customer_id, room_id, status) VALUES (?, ?, ?)");
            insertBookingStmt.setInt(1, customerId);
            insertBookingStmt.setInt(2, roomId);
            insertBookingStmt.setString(3, "CONFIRMED");
            insertBookingStmt.executeUpdate();

            // Commit transaction
            conn.commit();
            System.out.println(userLabel + " đã đặt phòng thành công!");

        } catch (SQLException | InterruptedException e) {
            // Rollback nếu có lỗi
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println(userLabel + " - Lỗi khi rollback: " + ex.getMessage());
            }

            System.out.println(userLabel + " - Lỗi khi đặt phòng: " + e.getMessage());

            // Ghi lại lỗi
            try {
                if (conn != null) {
                    logFailureStmt = conn.prepareStatement(
                            "INSERT INTO failed_bookings (customer_id, room_id, failure_reason) VALUES (?, ?, ?)");
                    logFailureStmt.setInt(1, customerId);
                    logFailureStmt.setInt(2, roomId);
                    logFailureStmt.setString(3, userLabel + " - Lỗi: " + e.getMessage());
                    logFailureStmt.executeUpdate();
                    conn.commit();
                }
            } catch (SQLException ex) {
                System.out.println(userLabel + " - Lỗi khi ghi log lỗi: " + ex.getMessage());
            }
        } finally {
            // Đóng tài nguyên
            try {
                if (checkRoomStmt != null) checkRoomStmt.close();
                if (updateRoomStmt != null) updateRoomStmt.close();
                if (insertBookingStmt != null) insertBookingStmt.close();
                if (logFailureStmt != null) logFailureStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println(userLabel + " - Lỗi khi đóng tài nguyên: " + e.getMessage());
            }
        }
    }

    // Phương thức xem lịch sử đặt phòng
    private static void viewBookingHistory() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT b.booking_id, c.name, c.phone, r.room_id, r.room_type, b.booking_date, b.status " +
                             "FROM bookings b " +
                             "JOIN customers c ON b.customer_id = c.customer_id " +
                             "JOIN rooms r ON b.room_id = r.room_id " +
                             "ORDER BY b.booking_date DESC")) {

            System.out.println("\n==== LỊCH SỬ ĐẶT PHÒNG ====");
            System.out.printf("%-5s %-20s %-15s %-8s %-15s %-20s %-10s%n",
                    "ID", "Tên khách hàng", "SĐT", "Phòng", "Loại phòng", "Ngày đặt", "Trạng thái");
            System.out.println("---------------------------------------------------------------------------------");

            boolean hasBookings = false;
            while (rs.next()) {
                hasBookings = true;
                System.out.printf("%-5d %-20s %-15s %-8d %-15s %-20s %-10s%n",
                        rs.getInt("booking_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getInt("room_id"),
                        rs.getString("room_type"),
                        rs.getTimestamp("booking_date"),
                        rs.getString("status"));
            }

            if (!hasBookings) {
                System.out.println("Chưa có đặt phòng nào!");
            }

        } catch (SQLException e) {
            System.out.println("Lỗi khi xem lịch sử đặt phòng: " + e.getMessage());
        }
    }

    // Phương thức xem lịch sử đặt phòng thất bại
    private static void viewFailedBookings() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT f.id, f.customer_id, c.name, f.room_id, f.failure_reason, f.failure_time " +
                             "FROM failed_bookings f " +
                             "LEFT JOIN customers c ON f.customer_id = c.customer_id " +
                             "ORDER BY f.failure_time DESC")) {

            System.out.println("\n==== LỊCH SỬ ĐẶT PHÒNG THẤT BẠI ====");
            System.out.printf("%-5s %-5s %-20s %-8s %-30s %-20s%n",
                    "ID", "CID", "Tên khách hàng", "Phòng", "Lý do", "Thời gian");
            System.out.println("------------------------------------------------------------------------------");

            boolean hasFailures = false;
            while (rs.next()) {
                hasFailures = true;
                System.out.printf("%-5d %-5d %-20s %-8d %-30s %-20s%n",
                        rs.getInt("id"),
                        rs.getInt("customer_id"),
                        rs.getString("name") != null ? rs.getString("name") : "N/A",
                        rs.getInt("room_id"),
                        rs.getString("failure_reason"),
                        rs.getTimestamp("failure_time"));
            }

            if (!hasFailures) {
                System.out.println("Chưa có đặt phòng thất bại nào!");
            }

        } catch (SQLException e) {
            System.out.println("Lỗi khi xem lịch sử đặt phòng thất bại: " + e.getMessage());
        }
    }
}


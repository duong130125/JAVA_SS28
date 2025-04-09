package BT_SS28;

import java.sql.*;
import java.time.LocalDate;

public class BT5 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "13012005";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            try {
                String insertOrderSQL = "INSERT INTO Orders (customer_name, order_date) VALUES (?, ?)";
                PreparedStatement orderStmt = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS);
                orderStmt.setString(1, "Nguyễn Văn A");
                orderStmt.setDate(2, Date.valueOf(LocalDate.now()));
                orderStmt.executeUpdate();

                ResultSet orderKeys = orderStmt.getGeneratedKeys();
                if (!orderKeys.next()) {
                    throw new SQLException("Không lấy được order_id.");
                }
                int orderId = orderKeys.getInt(1);
                System.out.println("Đã tạo đơn hàng, ID: " + orderId);

                String insertDetailSQL = "INSERT INTO OrderDetails (order_id, product_name, quantity) VALUES (?, ?, ?)";
                PreparedStatement detailStmt = conn.prepareStatement(insertDetailSQL);

                detailStmt.setInt(1, orderId);
                detailStmt.setString(2, "Chuột máy tính");
                detailStmt.setInt(3, 2);
                detailStmt.executeUpdate();

                detailStmt.setInt(1, orderId);
                detailStmt.setString(2, "Bàn phím");
                detailStmt.setInt(3, -1);
                detailStmt.executeUpdate();

                conn.commit();
                System.out.println("Giao dịch thành công: Đơn hàng và chi tiết đã được lưu.");

            } catch (SQLException e) {
                System.out.println("Lỗi trong transaction: " + e.getMessage());
                conn.rollback();
                System.out.println("Đã rollback toàn bộ giao dịch.");
            }

        } catch (SQLException e) {
            System.out.println("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
        }
    }
}

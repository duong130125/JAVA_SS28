package BT_SS28;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class BT2 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "13012005";

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Kết nối thành công!");

            conn.setAutoCommit(false);
            System.out.println("Auto-commit đã tắt.");

            String insertSQL1 = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
            PreparedStatement pstmt1 = conn.prepareStatement(insertSQL1);
            pstmt1.setInt(1, 200);
            pstmt1.setString(2, "Le Van B");
            pstmt1.setString(3, "b@example.com");
            pstmt1.executeUpdate();
            System.out.println("INSERT 1 thành công.");

            String insertSQL2 = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
            PreparedStatement pstmt2 = conn.prepareStatement(insertSQL2);
            pstmt2.setInt(1, 200); // ID bị trùng → sẽ gây lỗi
            pstmt2.setString(2, "Le Van C");
            pstmt2.setString(3, "c@example.com");
            pstmt2.executeUpdate();
            System.out.println("INSERT 2 thành công.");

            conn.commit();
            System.out.println("Đã commit dữ liệu.");

        } catch (SQLException e) {
            System.out.println("Lỗi xảy ra: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("Đã rollback dữ liệu do lỗi.");
                }
            } catch (SQLException rollbackEx) {
                System.out.println("Lỗi khi rollback: " + rollbackEx.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    ResultSet rs = conn.createStatement().executeQuery("SELECT FROM users WHERE id = 200");
                    if (!rs.next()) {
                        System.out.println("Xác minh: Không có dữ liệu nào được thêm vào.");
                    } else {
                        System.out.println("Dữ liệu vẫn tồn tại, rollback không thành công.");
                    }
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println("Lỗi khi đóng kết nối hoặc truy vấn kiểm tra: " + e.getMessage());
            }
        }
    }
}

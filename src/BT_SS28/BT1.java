package BT_SS28;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class BT1 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "13012005";

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Đã kết nối thành công!");

            boolean autoCommitStatus = conn.getAutoCommit();
            System.out.println("Trạng thái auto-commit ban đầu: " + autoCommitStatus);

            conn.setAutoCommit(false);
            System.out.println("Auto-commit đã tắt.");

            String insertSQL = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setInt(1, 105);
            pstmt.setString(2, "Nguyen Van A");
            pstmt.setString(3, "vana@example.com");

            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Số dòng được thêm: " + rowsInserted);

            conn.commit();
            System.out.println("Đã commit thành công!");

            String selectSQL = "SELECT FROM users WHERE id = 101";
            ResultSet rs = conn.createStatement().executeQuery(selectSQL);
            if (rs.next()) {
                System.out.println("Dữ liệu đã thêm: " + rs.getInt("id") + ", " +
                        rs.getString("name") + ", " + rs.getString("email"));
            } else {
                System.out.println("Không tìm thấy dữ liệu.");
            }

        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("Đã rollback do lỗi: " + e.getMessage());
                }
            } catch (SQLException ex) {
                System.out.println("Lỗi rollback: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Lỗi khi đóng kết nối: " + e.getMessage());
            }
        }
    }
}

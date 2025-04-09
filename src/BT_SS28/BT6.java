package BT_SS28;

import java.sql.*;

public class BT6 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "13012005";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);

            try {
                String insertDeptSQL = "INSERT INTO departments (name) VALUES (?)";
                PreparedStatement deptStmt = conn.prepareStatement(insertDeptSQL, Statement.RETURN_GENERATED_KEYS);
                deptStmt.setString(1, "Phòng Kỹ Thuật");
                deptStmt.executeUpdate();

                ResultSet deptKeys = deptStmt.getGeneratedKeys();
                if (!deptKeys.next()) {
                    throw new SQLException("Không lấy được ID phòng ban mới.");
                }
                int departmentId = deptKeys.getInt(1);
                System.out.println("Đã thêm phòng ban, ID: " + departmentId);

                String insertEmpSQL = "INSERT INTO employees (name, department_id) VALUES (?, ?)";
                PreparedStatement empStmt = conn.prepareStatement(insertEmpSQL);

                empStmt.setString(1, "Nguyễn Văn A");
                empStmt.setInt(2, departmentId);
                empStmt.executeUpdate();

                empStmt.setString(1, null);
                empStmt.setInt(2, departmentId);
                empStmt.executeUpdate();

                conn.commit();
                System.out.println("Tất cả dữ liệu đã được commit thành công.");

            } catch (SQLException e) {
                System.out.println("Lỗi trong giao dịch: " + e.getMessage());
                conn.rollback();
                System.out.println("Đã rollback toàn bộ giao dịch.");
            }

        } catch (SQLException e) {
            System.out.println("Lỗi kết nối cơ sở dữ liệu: " + e.getMessage());
        }
    }
}

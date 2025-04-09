package BT_SS28;

import java.sql.*;

public class BT3 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "13012005";

        int fromAccountId = 1;
        int toAccountId = 2;
        double amount = 1000.0;

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false);
            System.out.println("Đã kết nối và tắt auto-commit.");

            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT balance FROM accounts WHERE id = ?"
            );
            checkStmt.setInt(1, fromAccountId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Không tìm thấy tài khoản gửi.");
            }

            double currentBalance = rs.getDouble("balance");
            if (currentBalance < amount) {
                throw new SQLException("Không đủ số dư để chuyển tiền.");
            }

            PreparedStatement deductStmt = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance - ? WHERE id = ?"
            );
            deductStmt.setDouble(1, amount);
            deductStmt.setInt(2, fromAccountId);
            deductStmt.executeUpdate();

            PreparedStatement addStmt = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE id = ?"
            );
            addStmt.setDouble(1, amount);
            addStmt.setInt(2, toAccountId);
            int rowsAffected = addStmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Không tìm thấy tài khoản nhận.");
            }

            conn.commit();
            System.out.println("Chuyển tiền thành công. Đã commit.");

        } catch (SQLException e) {
            System.out.println("Lỗi: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("Đã rollback do lỗi.");
                }
            } catch (SQLException rollbackEx) {
                System.out.println("Lỗi rollback: " + rollbackEx.getMessage());
            }
        } finally {
            try {
                if (conn != null) conn.close();
                System.out.println("Đã đóng kết nối.");
            } catch (SQLException e) {
                System.out.println("Lỗi khi đóng kết nối: " + e.getMessage());
            }
        }
    }
}

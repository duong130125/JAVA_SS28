package BT_SS28;

import java.sql.*;

public class BT4 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/user";
        String user = "root";
        String password = "13012005";

        int fromAccountId = 1;
        int toAccountId = 2;
        double amount = 1000.0;

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            System.out.println("Đã kết nối. Bắt đầu giao dịch chuyển tiền...");

            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT balance FROM bank_accounts WHERE account_id = ?"
            );
            checkStmt.setInt(1, fromAccountId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Không tìm thấy tài khoản gửi.");
            }

            double currentBalance = rs.getDouble("balance");
            if (currentBalance < amount) {
                throw new SQLException("Không đủ số dư trong tài khoản gửi.");
            }

            PreparedStatement deductStmt = conn.prepareStatement(
                    "UPDATE bank_accounts SET balance = balance - ? WHERE account_id = ?"
            );
            deductStmt.setDouble(1, amount);
            deductStmt.setInt(2, fromAccountId);
            deductStmt.executeUpdate();

            PreparedStatement addStmt = conn.prepareStatement(
                    "UPDATE bank_accounts SET balance = balance + ? WHERE account_id = ?"
            );
            addStmt.setDouble(1, amount);
            addStmt.setInt(2, toAccountId);
            int rowsAffected = addStmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Không tìm thấy tài khoản nhận.");
            }

            conn.commit();
            System.out.println("Chuyển khoản thành công. Đã commit lên database.");

        } catch (SQLException e) {
            System.out.println("Lỗi xảy ra trong giao dịch: " + e.getMessage());
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                conn.rollback();
                System.out.println("Đã rollback giao dịch.");
            } catch (SQLException rollbackEx) {
                System.out.println("Lỗi khi rollback: " + rollbackEx.getMessage());
            }
        }
    }
}

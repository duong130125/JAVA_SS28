package BT_SS28;

import java.sql.*;
import java.util.Scanner;

public class BT7 {
    // Thông tin kết nối database
    private static final String DB_URL = "jdbc:mysql://localhost:3306/user";
    private static final String USER = "root";
    private static final String PASS = "13012005";

    // Scanner để tương tác với người dùng
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            // Reset dữ liệu trước khi bắt đầu thí nghiệm
            resetData();

            // Menu chính
            while (true) {
                System.out.println("\n======= THÍ NGHIỆM TRANSACTION ISOLATION LEVEL =======");
                System.out.println("1. READ_UNCOMMITTED - Kiểm tra Dirty Read");
                System.out.println("2. READ_COMMITTED - Kiểm tra Non-Repeatable Read");
                System.out.println("3. REPEATABLE_READ - Kiểm tra Phantom Read");
                System.out.println("4. SERIALIZABLE - Kiểm tra Lock");
                System.out.println("5. Hiển thị tất cả dữ liệu");
                System.out.println("6. Reset dữ liệu về ban đầu");
                System.out.println("0. Thoát");
                System.out.print("Chọn thí nghiệm: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Xử lý ký tự xuống dòng

                switch (choice) {
                    case 1:
                        testReadUncommitted();
                        break;
                    case 2:
                        testReadCommitted();
                        break;
                    case 3:
                        testRepeatableRead();
                        break;
                    case 4:
                        testSerializable();
                        break;
                    case 5:
                        showAllData();
                        break;
                    case 6:
                        resetData();
                        break;
                    case 0:
                        System.out.println("Kết thúc chương trình.");
                        scanner.close();
                        System.exit(0);
                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test READ_UNCOMMITTED - Kiểm tra hiện tượng Dirty Read
     */
    private static void testReadUncommitted() throws SQLException {
        System.out.println("\n=== THÍ NGHIỆM READ_UNCOMMITTED - DIRTY READ ===");

        // Tạo 2 kết nối riêng biệt
        Connection connection1 = DriverManager.getConnection(DB_URL, USER, PASS);
        Connection connection2 = DriverManager.getConnection(DB_URL, USER, PASS);

        try {
            // Thiết lập isolation level cho connection1
            connection1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            connection1.setAutoCommit(false);

            // Thiết lập isolation level cho connection2
            connection2.setAutoCommit(false);

            // Hiển thị dữ liệu ban đầu từ connection1
            System.out.println("Dữ liệu ban đầu từ connection1:");
            displayOrders(connection1);

            // Từ connection2, cập nhật dữ liệu nhưng chưa commit
            Statement stmt2 = connection2.createStatement();
            System.out.println("\nTừ connection2: Thực hiện UPDATE nhưng chưa commit");
            int rowsAffected = stmt2.executeUpdate("UPDATE orderss SET status = 'Updated' WHERE order_id = 1");
            System.out.println("Đã cập nhật " + rowsAffected + " dòng (chưa commit)");

            // Từ connection1, đọc lại dữ liệu
            System.out.println("\nĐọc lại dữ liệu từ connection1 (với READ_UNCOMMITTED):");
            displayOrders(connection1);

            // Hỏi người dùng muốn commit hay rollback connection2
            System.out.print("\nBạn muốn commit hay rollback transaction trên connection2? (c/r): ");
            String decision = scanner.nextLine();

            if (decision.equalsIgnoreCase("c")) {
                connection2.commit();
                System.out.println("Đã commit thay đổi từ connection2");
            } else {
                connection2.rollback();
                System.out.println("Đã rollback thay đổi từ connection2");
            }

            // Đọc lại dữ liệu từ connection1 sau khi commit/rollback
            System.out.println("\nĐọc lại dữ liệu từ connection1 sau khi commit/rollback:");
            displayOrders(connection1);

            // Commit transaction trên connection1
            connection1.commit();

            System.out.println("\n*** Kết luận: ***");
            System.out.println("- Với READ_UNCOMMITTED, connection1 có thể đọc được dữ liệu chưa commit từ connection2");
            System.out.println("- Đây là hiện tượng Dirty Read, có thể gây ra vấn đề nếu connection2 rollback");

        } finally {
            // Đóng các kết nối
            if (connection1 != null) {
                connection1.setAutoCommit(true);
                connection1.close();
            }
            if (connection2 != null) {
                connection2.setAutoCommit(true);
                connection2.close();
            }
        }
    }

    /**
     * Test READ_COMMITTED - Kiểm tra hiện tượng Non-Repeatable Read
     */
    private static void testReadCommitted() throws SQLException {
        System.out.println("\n=== THÍ NGHIỆM READ_COMMITTED - NON-REPEATABLE READ ===");

        // Tạo 2 kết nối riêng biệt
        Connection connection1 = DriverManager.getConnection(DB_URL, USER, PASS);
        Connection connection2 = DriverManager.getConnection(DB_URL, USER, PASS);

        try {
            // Thiết lập isolation level cho connection1
            connection1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection1.setAutoCommit(false);

            // Thiết lập connection2
            connection2.setAutoCommit(false);

            // Đọc dữ liệu lần 1 từ connection1
            System.out.println("Đọc dữ liệu lần 1 từ connection1:");
            displayOrders(connection1);

            // Đợi input từ người dùng
            System.out.print("\nNhấn Enter để tiếp tục...");
            scanner.nextLine();

            // Từ connection2, cập nhật dữ liệu và commit
            Statement stmt2 = connection2.createStatement();
            System.out.println("\nTừ connection2: Thực hiện UPDATE và commit");
            int rowsAffected = stmt2.executeUpdate("UPDATE orderss SET status = 'Modified' WHERE order_id = 2");
            connection2.commit();
            System.out.println("Đã cập nhật và commit " + rowsAffected + " dòng từ connection2");

            // Đọc dữ liệu lần 2 từ connection1 (cùng transaction với lần đọc 1)
            System.out.println("\nĐọc dữ liệu lần 2 từ connection1 (cùng transaction):");
            displayOrders(connection1);

            // Commit transaction trên connection1
            connection1.commit();

            System.out.println("\n*** Kết luận: ***");
            System.out.println("- Với READ_COMMITTED, connection1 có thể thấy thay đổi đã commit từ connection2");
            System.out.println("- Khi cùng một câu lệnh SELECT thực hiện nhiều lần trong cùng transaction,");
            System.out.println("  kết quả có thể khác nhau do đọc được các thay đổi đã commit từ transaction khác");
            System.out.println("- Đây là hiện tượng Non-Repeatable Read");

        } finally {
            // Đóng các kết nối
            if (connection1 != null) {
                connection1.setAutoCommit(true);
                connection1.close();
            }
            if (connection2 != null) {
                connection2.setAutoCommit(true);
                connection2.close();
            }
        }
    }

    /**
     * Test REPEATABLE_READ - Kiểm tra hiện tượng Phantom Read
     */
    private static void testRepeatableRead() throws SQLException {
        System.out.println("\n=== THÍ NGHIỆM REPEATABLE_READ - PHANTOM READ ===");

        // Tạo 2 kết nối riêng biệt
        Connection connection1 = DriverManager.getConnection(DB_URL, USER, PASS);
        Connection connection2 = DriverManager.getConnection(DB_URL, USER, PASS);

        try {
            // Thiết lập isolation level cho connection1
            connection1.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection1.setAutoCommit(false);

            // Thiết lập connection2
            connection2.setAutoCommit(false);

            // Đọc dữ liệu lần 1 từ connection1
            System.out.println("Đọc tất cả dữ liệu lần 1 từ connection1:");
            displayOrders(connection1);

            // Đợi input từ người dùng
            System.out.print("\nNhấn Enter để tiếp tục...");
            scanner.nextLine();

            // Từ connection2, thêm dữ liệu mới và commit
            Statement stmt2 = connection2.createStatement();
            System.out.println("\nTừ connection2: Thực hiện INSERT và commit");
            int rowsAffected = stmt2.executeUpdate(
                    "INSERT INTO orderss (order_id, customer_name, status) VALUES (4, 'Phạm Văn D', 'New')");
            connection2.commit();
            System.out.println("Đã thêm và commit " + rowsAffected + " dòng từ connection2");

            // Đọc dữ liệu lần 2 từ connection1 với cùng câu lệnh
            System.out.println("\nĐọc tất cả dữ liệu lần 2 từ connection1 (cùng transaction):");
            displayOrders(connection1);

            // Đọc dữ liệu mới được thêm vào từ connection1 (thử nghiệm phantom read)
            System.out.println("\nTìm dữ liệu với order_id = 4 từ connection1:");
            displayOrderWithId(connection1, 4);

            // Commit transaction trên connection1
            connection1.commit();

            // Đọc lại sau khi commit
            System.out.println("\nĐọc lại tất cả dữ liệu sau khi commit transaction1:");
            displayOrders(connection1);

            System.out.println("\n*** Kết luận: ***");
            System.out.println("- Với REPEATABLE_READ, các dòng đã đọc trước đó không thay đổi trong cùng transaction");
            System.out.println("- Tuy nhiên, có thể xảy ra Phantom Read khi một transaction khác thêm dữ liệu mới");
            System.out.println("- MySQL InnoDB thực tế ngăn chặn Phantom Read bằng next-key locking trong REPEATABLE_READ");
            System.out.println("- Sau khi commit transaction và đọc lại, có thể thấy dữ liệu mới");

        } finally {
            // Đóng các kết nối
            if (connection1 != null) {
                connection1.setAutoCommit(true);
                connection1.close();
            }
            if (connection2 != null) {
                connection2.setAutoCommit(true);
                connection2.close();
            }
        }
    }

    /**
     * Test SERIALIZABLE - Kiểm tra lock và serialization
     */
    private static void testSerializable() throws SQLException {
        System.out.println("\n=== THÍ NGHIỆM SERIALIZABLE - LOCK & SERIALIZATION ===");

        // Tạo 2 kết nối riêng biệt
        Connection connection1 = DriverManager.getConnection(DB_URL, USER, PASS);
        Connection connection2 = DriverManager.getConnection(DB_URL, USER, PASS);

        try {
            // Thiết lập isolation level cho connection1
            connection1.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection1.setAutoCommit(false);

            // Thiết lập isolation level cho connection2
            connection2.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection2.setAutoCommit(false);

            // Đọc dữ liệu từ connection1
            System.out.println("Đọc dữ liệu từ connection1:");
            displayOrders(connection1);

            // Đợi input từ người dùng
            System.out.print("\nNhấn Enter để tiếp tục...");
            scanner.nextLine();

            // Từ connection2, thử chèn dữ liệu mới (có thể bị block)
            System.out.println("\nTừ connection2: Thử INSERT dữ liệu mới");

            Thread insertThread = new Thread(() -> {
                try {
                    Statement stmt2 = connection2.createStatement();
                    System.out.println("Đang thực hiện INSERT từ connection2...");
                    int rowsAffected = stmt2.executeUpdate(
                            "INSERT INTO orderss (order_id, customer_name, status) VALUES (5, 'Hoàng Thị E', 'Pending')");

                    // Dòng này có thể không được thực hiện ngay nếu bị block
                    System.out.println("Đã INSERT " + rowsAffected + " dòng từ connection2 (chưa commit)");

                } catch (SQLException e) {
                    System.out.println("Lỗi khi thực hiện INSERT từ connection2: " + e.getMessage());
                }
            });

            insertThread.start();

            // Đợi một chút để thread INSERT bắt đầu
            Thread.sleep(1000);

            // Đọc dữ liệu lại từ connection1
            System.out.println("\nĐọc lại dữ liệu từ connection1 (cùng transaction):");
            displayOrders(connection1);

            // Đợi input từ người dùng để quyết định commit connection1
            System.out.print("\nNhấn Enter để commit transaction1...");
            scanner.nextLine();

            // Commit transaction1
            connection1.commit();
            System.out.println("Đã commit transaction1");

            // Đợi thread INSERT hoàn thành
            insertThread.join();

            // Hỏi người dùng muốn commit hay rollback connection2
            System.out.print("\nBạn muốn commit hay rollback transaction trên connection2? (c/r): ");
            String decision = scanner.nextLine();

            if (decision.equalsIgnoreCase("c")) {
                connection2.commit();
                System.out.println("Đã commit thay đổi từ connection2");
            } else {
                connection2.rollback();
                System.out.println("Đã rollback thay đổi từ connection2");
            }

            // Hiển thị dữ liệu sau cùng
            System.out.println("\nDữ liệu sau cùng:");
            Connection finalConn = DriverManager.getConnection(DB_URL, USER, PASS);
            displayOrders(finalConn);
            finalConn.close();

            System.out.println("\n*** Kết luận: ***");
            System.out.println("- SERIALIZABLE là isolation level nghiêm ngặt nhất");
            System.out.println("- Các transaction thực thi tuần tự, một transaction phải đợi transaction khác hoàn thành");
            System.out.println("- Không xảy ra Dirty Read, Non-Repeatable Read, Phantom Read");
            System.out.println("- Hiệu suất thấp nhất do phải đợi và khóa nhiều");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Đóng các kết nối
            if (connection1 != null) {
                connection1.setAutoCommit(true);
                connection1.close();
            }
            if (connection2 != null) {
                connection2.setAutoCommit(true);
                connection2.close();
            }
        }
    }

    /**
     * Hiển thị tất cả dữ liệu trong bảng orders
     */
    private static void showAllData() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        System.out.println("\n=== DANH SÁCH TẤT CẢ ĐƠN HÀNG ===");
        displayOrders(conn);
        conn.close();
    }

    /**
     * Reset dữ liệu về trạng thái ban đầu
     */
    private static void resetData() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        Statement stmt = conn.createStatement();

        // Xóa bảng nếu đã tồn tại và tạo lại
        stmt.execute("DROP TABLE IF EXISTS orderss");
        stmt.execute("CREATE TABLE orderss (" +
                "order_id INT PRIMARY KEY, " +
                "customer_name VARCHAR(100), " +
                "status VARCHAR(20))");

        // Chèn dữ liệu mẫu
        stmt.execute("INSERT INTO orderss VALUES (1, 'Nguyễn Văn A', 'Pending')");
        stmt.execute("INSERT INTO orderss VALUES (2, 'Trần Thị B', 'Completed')");
        stmt.execute("INSERT INTO orderss VALUES (3, 'Lê Văn C', 'Processing')");

        System.out.println("Đã reset dữ liệu về trạng thái ban đầu!");

        stmt.close();
        conn.close();
    }

    /**
     * Hiển thị dữ liệu từ bảng orders
     */
    private static void displayOrders(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM orderss ORDER BY order_id");

        System.out.printf("%-10s %-20s %-15s%n", "ORDER_ID", "CUSTOMER_NAME", "STATUS");
        System.out.println("-----------------------------------------");

        boolean hasData = false;
        while (rs.next()) {
            hasData = true;
            System.out.printf("%-10d %-20s %-15s%n",
                    rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getString("status"));
        }

        if (!hasData) {
            System.out.println("Không có dữ liệu nào!");
        }

        rs.close();
        stmt.close();
    }

    /**
     * Hiển thị dữ liệu đơn hàng với id cụ thể
     */
    private static void displayOrderWithId(Connection conn, int orderId) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM orderss WHERE order_id = ?");
        pstmt.setInt(1, orderId);
        ResultSet rs = pstmt.executeQuery();

        System.out.printf("%-10s %-20s %-15s%n", "ORDER_ID", "CUSTOMER_NAME", "STATUS");
        System.out.println("-----------------------------------------");

        boolean hasData = false;
        while (rs.next()) {
            hasData = true;
            System.out.printf("%-10d %-20s %-15s%n",
                    rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getString("status"));
        }

        if (!hasData) {
            System.out.println("Không tìm thấy đơn hàng với ID = " + orderId);
        }

        rs.close();
        pstmt.close();
    }
}

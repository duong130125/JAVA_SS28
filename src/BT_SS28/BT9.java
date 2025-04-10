package BT_SS28;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BT9 {
    // Class đại diện cho người dùng
    static class User {
        private int userId;
        private String username;
        private double balance;

        public User(int userId, String username, double balance) {
            this.userId = userId;
            this.username = username;
            this.balance = balance;
        }

        // Getters và setters
        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        @Override
        public String toString() {
            return "User [userId=" + userId + ", username=" + username + ", balance=" + balance + "]";
        }
    }

    // Class đại diện cho phiên đấu giá
    static class Auction {
        private int auctionId;
        private String itemName;
        private double highestBid;
        private String status;

        public Auction(int auctionId, String itemName, double highestBid, String status) {
            this.auctionId = auctionId;
            this.itemName = itemName;
            this.highestBid = highestBid;
            this.status = status;
        }

        // Getters và setters
        public int getAuctionId() {
            return auctionId;
        }

        public void setAuctionId(int auctionId) {
            this.auctionId = auctionId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public double getHighestBid() {
            return highestBid;
        }

        public void setHighestBid(double highestBid) {
            this.highestBid = highestBid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "Auction [auctionId=" + auctionId + ", itemName=" + itemName +
                    ", highestBid=" + highestBid + ", status=" + status + "]";
        }
    }

    // Class đại diện cho lần đặt giá
    static class Bid {
        private int bidId;
        private int auctionId;
        private int userId;
        private double bidAmount;

        public Bid(int bidId, int auctionId, int userId, double bidAmount) {
            this.bidId = bidId;
            this.auctionId = auctionId;
            this.userId = userId;
            this.bidAmount = bidAmount;
        }

        // Constructor không có bidId cho lần đặt giá mới
        public Bid(int auctionId, int userId, double bidAmount) {
            this.auctionId = auctionId;
            this.userId = userId;
            this.bidAmount = bidAmount;
        }

        // Getters và setters
        public int getBidId() {
            return bidId;
        }

        public void setBidId(int bidId) {
            this.bidId = bidId;
        }

        public int getAuctionId() {
            return auctionId;
        }

        public void setAuctionId(int auctionId) {
            this.auctionId = auctionId;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public double getBidAmount() {
            return bidAmount;
        }

        public void setBidAmount(double bidAmount) {
            this.bidAmount = bidAmount;
        }

        @Override
        public String toString() {
            return "Bid [bidId=" + bidId + ", auctionId=" + auctionId +
                    ", userId=" + userId + ", bidAmount=" + bidAmount + "]";
        }
    }

    // Class tiện ích quản lý kết nối database
    static class DBConnection {
        // Thông tin kết nối database
        private static final String URL = "jdbc:mysql://localhost:3306/user";
        private static final String USER = "root";
        private static final String PASSWORD = "13012005";

        // Phương thức lấy kết nối
        public static Connection getConnection() throws SQLException {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Database driver not found", e);
            }
        }

        // Phương thức đóng kết nối
        public static void closeConnection(Connection conn) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    // Class service xử lý đấu giá với transaction
    static class AuctionService {

        /**
         * Phương thức đặt giá sử dụng Transaction
         */
        public boolean placeBid(int auctionId, int userId, double bidAmount) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            boolean success = false;

            try {
                // Lấy kết nối
                conn = DBConnection.getConnection();

                // Tắt auto commit để bắt đầu transaction
                conn.setAutoCommit(false);

                // Thiết lập isolation level là SERIALIZABLE để đảm bảo tính toàn vẹn dữ liệu
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

                // Bước 1: Kiểm tra số dư của người dùng
                String checkBalanceSQL = "SELECT balance FROM users WHERE user_id = ?";
                pstmt = conn.prepareStatement(checkBalanceSQL);
                pstmt.setInt(1, userId);
                rs = pstmt.executeQuery();

                if (!rs.next() || rs.getDouble("balance") < bidAmount) {
                    // Ghi lại thông tin đấu giá thất bại
                    logFailedBid(conn, auctionId, userId, bidAmount, "Không đủ số dư");
                    conn.commit();
                    return false;
                }

                // Bước 2: Kiểm tra giá cao nhất hiện tại của phiên đấu giá
                String checkAuctionSQL = "SELECT highest_bid, status FROM auctions WHERE auction_id = ?";
                pstmt = conn.prepareStatement(checkAuctionSQL);
                pstmt.setInt(1, auctionId);
                rs = pstmt.executeQuery();

                if (!rs.next()) {
                    logFailedBid(conn, auctionId, userId, bidAmount, "Phiên đấu giá không tồn tại");
                    conn.commit();
                    return false;
                }

                double currentHighestBid = rs.getDouble("highest_bid");
                String auctionStatus = rs.getString("status");

                if (!auctionStatus.equals("ACTIVE")) {
                    logFailedBid(conn, auctionId, userId, bidAmount, "Phiên đấu giá không còn hoạt động");
                    conn.commit();
                    return false;
                }

                if (bidAmount <= currentHighestBid) {
                    logFailedBid(conn, auctionId, userId, bidAmount,
                            "Giá đặt thấp hơn giá cao nhất hiện tại: " + currentHighestBid);
                    conn.commit();
                    return false;
                }

                // Bước 3: Cập nhật giá cao nhất trong bảng auctions
                String updateAuctionSQL = "UPDATE auctions SET highest_bid = ? WHERE auction_id = ?";
                pstmt = conn.prepareStatement(updateAuctionSQL);
                pstmt.setDouble(1, bidAmount);
                pstmt.setInt(2, auctionId);
                pstmt.executeUpdate();

                // Bước 4: Lưu thông tin đặt giá vào bảng bids
                String insertBidSQL = "INSERT INTO bids (auction_id, user_id, bid_amount) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(insertBidSQL);
                pstmt.setInt(1, auctionId);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, bidAmount);
                pstmt.executeUpdate();

                // Commit nếu tất cả thành công
                conn.commit();
                success = true;

            } catch (SQLException e) {
                System.out.println("Error in placeBid: " + e.getMessage());
                try {
                    if (conn != null) {
                        conn.rollback(); // Rollback nếu có lỗi
                    }
                } catch (SQLException ex) {
                    System.out.println("Error during rollback: " + ex.getMessage());
                }
            } finally {
                closeResources(pstmt, rs);
                DBConnection.closeConnection(conn);
            }

            return success;
        }

        /**
         * Ghi lại thông tin đấu giá thất bại
         */
        private void logFailedBid(Connection conn, int auctionId, int userId,
                                  double bidAmount, String reason) throws SQLException {
            String sql = "INSERT INTO failed_bids (auction_id, user_id, bid_amount, failure_reason) " +
                    "VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = null;

            try {
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, auctionId);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, bidAmount);
                pstmt.setString(4, reason);
                pstmt.executeUpdate();
            } finally {
                if (pstmt != null) {
                    pstmt.close();
                }
            }
        }

        /**
         * Lấy thông tin phiên đấu giá
         */
        public Auction getAuction(int auctionId) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            Auction auction = null;

            try {
                conn = DBConnection.getConnection();
                String sql = "SELECT * FROM auctions WHERE auction_id = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, auctionId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    auction = new Auction(
                            rs.getInt("auction_id"),
                            rs.getString("item_name"),
                            rs.getDouble("highest_bid"),
                            rs.getString("status")
                    );
                }

            } catch (SQLException e) {
                System.out.println("Error getting auction: " + e.getMessage());
            } finally {
                closeResources(pstmt, rs);
                DBConnection.closeConnection(conn);
            }

            return auction;
        }

        /**
         * Lấy thông tin người dùng
         */
        public User getUser(int userId) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            User user = null;

            try {
                conn = DBConnection.getConnection();
                String sql = "SELECT * FROM users WHERE user_id = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, userId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    user = new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getDouble("balance")
                    );
                }

            } catch (SQLException e) {
                System.out.println("Error getting user: " + e.getMessage());
            } finally {
                closeResources(pstmt, rs);
                DBConnection.closeConnection(conn);
            }

            return user;
        }

        /**
         * Phương thức đóng tài nguyên
         */
        private void closeResources(PreparedStatement pstmt, ResultSet rs) {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }


    public static void main(String[] args) {
        AuctionService auctionService = new AuctionService();

        // Lấy thông tin phiên đấu giá và người dùng
        Auction auction = auctionService.getAuction(1);
        User user1 = auctionService.getUser(1);
        User user2 = auctionService.getUser(2);

        System.out.println("--- THÔNG TIN BAN ĐẦU ---");
        System.out.println("Phiên đấu giá: " + auction);
        System.out.println("Người dùng 1: " + user1);
        System.out.println("Người dùng 2: " + user2);

        System.out.println("\n--- TÌNH HUỐNG 1: ĐẶT GIÁ THÀNH CÔNG ---");
        // Đặt giá cao hơn giá hiện tại và đủ tiền
        boolean result1 = auctionService.placeBid(1, 1, 500.0);
        System.out.println("Kết quả đặt giá: " + (result1 ? "Thành công" : "Thất bại"));

        // Kiểm tra thông tin sau khi đặt giá
        auction = auctionService.getAuction(1);
        System.out.println("Giá cao nhất mới: " + auction.getHighestBid());

        System.out.println("\n--- TÌNH HUỐNG 2: ĐẶT GIÁ THẤP HƠN GIÁ HIỆN TẠI ---");
        // Đặt giá thấp hơn giá hiện tại
        boolean result2 = auctionService.placeBid(1, 2, 350.0);
        System.out.println("Kết quả đặt giá: " + (result2 ? "Thành công" : "Thất bại"));

        System.out.println("\n--- TÌNH HUỐNG 3: SỐ DƯ KHÔNG ĐỦ ---");
        // Đặt giá cao nhưng số dư không đủ
        boolean result3 = auctionService.placeBid(1, 2, 2000.0);
        System.out.println("Kết quả đặt giá: " + (result3 ? "Thành công" : "Thất bại"));

        // Kiểm tra thông tin cuối cùng
        auction = auctionService.getAuction(1);
        System.out.println("\n--- THÔNG TIN SAU CÙNG ---");
        System.out.println("Phiên đấu giá: " + auction);
    }
}

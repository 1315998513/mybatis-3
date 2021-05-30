package org.apache.ibatis.demo.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;

public class Jdbc {
  public static void main(String[] args) {
    /**
     * String url = "jdbc:mysql://localhost:3306/test_db";
     *     String userName = "root";
     *     String password = "123";
     *     try (Connection conn = DriverManager.getConnection(url, userName, password)) {
     *       try (PreparedStatment ps = conn.preparedStatement("SELECT * FROM test_table WHERE name=?")) {
     *         ps.setObject(1, "rick");
     *         try (ResultSet rs = ps.excuteQuery()) {
     *           while (rs.next()) {
     *             System.out.println(rs.getString("name"));
     *           }
     *         }
     *       }
     *     }
     *
     * String url = "jdbc:mysql://localhost:3306/test_db";
     * String userName = "root";
     * String password = "123";
     * try(Connection conn = DriverManager.getConnection(url, userName,password)) {
     *     try(PreparedStatment ps = conn.preparedStatement("UPDATE test_table SET age = ? WHERE name=?")) {
     *         ps.setObject(2, "rick");
     *         ps.setInt(1, 18);
     *         int n = ps.excuteQuery(); //成功执行的数据条数
     *     }
     * }
     */
  }
}

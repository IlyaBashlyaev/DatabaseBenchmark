/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package main.databasebenchmark;
import java.sql.*;

public class DatabaseBenchmark {
    public static void main(String[] args) {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Keine Treiber-Klasse!");
            return;
        }
        
        try (
                Connection con = DriverManager.getConnection(
                        "jdbc:hsqldb:file:./data/mydb", "SA", ""
                );
                Statement stmt = con.createStatement();
        ) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM CUSTOMER"
            );
            
            while (rs.next()) {
                System.out.printf(
                        "%d. %s %s%n",
                        rs.getInt("ID"),
                        rs.getString(2),
                        rs.getString(3)
                );
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

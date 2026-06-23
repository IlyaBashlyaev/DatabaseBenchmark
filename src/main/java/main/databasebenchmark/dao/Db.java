package main.databasebenchmark.dao;

import java.sql.*;

public class Db implements AutoCloseable {

    public String driver = "org.hsqldb.jdbc.JDBCDriver";
    public String url    = "jdbc:hsqldb:file:./data/mydb";
    public String usr    = "SA";
    public String passwd = "";

    private Connection con = null;

    public Connection getCon() {
        try {
            Class.forName(driver);
            if (con == null || !con.isValid(0)) {
                con = DriverManager.getConnection(url, usr, passwd);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Connection error [" + url + "]: " + e.getMessage());
        }
        return con;
    }

    @Override
    public void close() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                System.err.println("Close error: " + e.getMessage());
            } finally {
                con = null;
            }
        }
    }
}

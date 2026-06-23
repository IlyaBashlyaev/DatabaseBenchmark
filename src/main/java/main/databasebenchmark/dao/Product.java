package main.databasebenchmark.dao;

import java.math.BigDecimal;
import java.sql.*;

public class Product {

    public int        id;
    public String     name;
    public BigDecimal price;

    @Override
    public String toString() {
        return String.format("%5d  %-20s  %8.2f", id, name, price);
    }

    public static Product create(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.id    = rs.getInt("ID");
        p.name  = rs.getString("NAME");
        p.price = rs.getBigDecimal("PRICE");
        return p;
    }
}

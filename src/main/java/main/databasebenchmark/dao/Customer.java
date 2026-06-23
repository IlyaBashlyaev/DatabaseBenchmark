package main.databasebenchmark.dao;

import java.sql.*;

public class Customer {

    public int    id;
    public String firstName;
    public String lastName;
    public String street;
    public String city;

    @Override
    public String toString() {
        return String.format("%5d  %-15s %-15s %-20s %-15s",
                id, firstName, lastName, street, city);
    }

    public static Customer create(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.id        = rs.getInt("ID");
        c.firstName = rs.getString("FIRSTNAME");
        c.lastName  = rs.getString("LASTNAME");
        c.street    = rs.getString("STREET");
        c.city      = rs.getString("CITY");
        return c;
    }
}

package main.databasebenchmark.benchmark;

import java.sql.*;
import java.util.Random;

public class DataGenerator {

    private static final Random RNG = new Random(42);

    private static final String[] FIRST_NAMES = {
        "Anna", "Bob", "Carl", "Diana", "Eva", "Frank", "Greta", "Hans",
        "Iris", "Jan", "Kate", "Leo", "Maria", "Nick", "Olga", "Paul",
        "Rita", "Stefan", "Tina", "Uwe", "Vera", "Werner", "Xena", "Yannick"
    };

    private static final String[] LAST_NAMES = {
        "Mueller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer",
        "Wagner", "Becker", "Schulz", "Hoffmann", "Koch", "Bauer",
        "Richter", "Klein", "Wolf", "Schroeder", "Neumann", "Schwarz",
        "Zimmermann", "Braun", "Krause", "Hartmann", "Lange", "Werner"
    };

    private static final String[] CITIES = {
        "Berlin", "Hamburg", "Munich", "Cologne", "Frankfurt", "Stuttgart",
        "Dusseldorf", "Leipzig", "Dortmund", "Essen", "Bremen", "Dresden",
        "Hanover", "Nuremberg", "Bochum", "Wuppertal", "Bielefeld", "Bonn"
    };

    private static final String[] STREET_NAMES = {
        "Main St", "Oak Ave", "Elm St", "Park Rd", "River Rd", "Hill Rd",
        "Lake Dr", "Forest Ave", "Church St", "School Rd", "Station Rd",
        "Castle St", "Garden Rd", "Market St", "Bridge Rd", "Mill Lane"
    };

    private static final String[] PRODUCT_ADJ = {
        "Iron", "Ice", "Gold", "Silver", "Blue", "Red", "Smart", "Classic",
        "Premium", "Basic", "Pro", "Mini", "Mega", "Super", "Ultra", "Eco"
    };

    private static final String[] PRODUCT_NOUNS = {
        "Clock", "Chair", "Shoe", "Phone", "Lamp", "Table", "Cup",
        "Pen", "Bag", "Book", "Box", "Fan", "Rack", "Stand", "Pad"
    };

    public static void createSchema(Connection con, boolean isHsqldb) throws SQLException {
        String t = isHsqldb ? "CACHED " : "";
        try (Statement s = con.createStatement()) {
            s.execute(
                "CREATE " + t + "TABLE IF NOT EXISTS CUSTOMER (" +
                "ID INTEGER PRIMARY KEY, FIRSTNAME VARCHAR(20), LASTNAME VARCHAR(20), " +
                "STREET VARCHAR(20), CITY VARCHAR(20))"
            );
            s.execute(
                "CREATE " + t + "TABLE IF NOT EXISTS PRODUCT (" +
                "ID INTEGER PRIMARY KEY, NAME VARCHAR(20), PRICE DECIMAL(10,2))"
            );
            s.execute(
                "CREATE " + t + "TABLE IF NOT EXISTS INVOICE (" +
                "ID INTEGER PRIMARY KEY, CUSTOMERID INTEGER, TOTAL DECIMAL(10,2), " +
                "FOREIGN KEY (CUSTOMERID) REFERENCES CUSTOMER(ID) ON DELETE CASCADE)"
            );
            s.execute(
                "CREATE " + t + "TABLE IF NOT EXISTS ITEM (" +
                "INVOICEID INTEGER, ITEM INTEGER, PRODUCTID INTEGER, " +
                "QUANTITY INTEGER, COST DECIMAL(10,2), " +
                "PRIMARY KEY (INVOICEID, ITEM), " +
                "FOREIGN KEY (INVOICEID) REFERENCES INVOICE(ID) ON DELETE CASCADE, " +
                "FOREIGN KEY (PRODUCTID) REFERENCES PRODUCT(ID) ON DELETE CASCADE)"
            );
            s.execute(
                "CREATE " + t + "TABLE IF NOT EXISTS FLAT_SALES (" +
                "CUSTOMERID INTEGER, FIRSTNAME VARCHAR(20), LASTNAME VARCHAR(20), " +
                "CITY VARCHAR(20), INVOICEID INTEGER, INVOICE_TOTAL DECIMAL(10,2), " +
                "PRODUCT_NAME VARCHAR(20), QUANTITY INTEGER, COST DECIMAL(10,2))"
            );
        }
    }

    public static void deleteAll(Connection con) throws SQLException {
        boolean prev = con.getAutoCommit();
        con.setAutoCommit(false);
        try (Statement s = con.createStatement()) {
            s.execute("DELETE FROM FLAT_SALES");
            s.execute("DELETE FROM ITEM");
            s.execute("DELETE FROM INVOICE");
            s.execute("DELETE FROM PRODUCT");
            s.execute("DELETE FROM CUSTOMER");
            con.commit();
        } finally {
            con.setAutoCommit(prev);
        }
    }

    public static String firstName()    { return FIRST_NAMES[RNG.nextInt(FIRST_NAMES.length)]; }
    public static String lastName()     { return LAST_NAMES[RNG.nextInt(LAST_NAMES.length)]; }
    public static String city()         { return CITIES[RNG.nextInt(CITIES.length)]; }
    public static String productName()  { return PRODUCT_ADJ[RNG.nextInt(PRODUCT_ADJ.length)] + " " + PRODUCT_NOUNS[RNG.nextInt(PRODUCT_NOUNS.length)]; }

    public static String street() {
        return (RNG.nextInt(999) + 1) + " " + STREET_NAMES[RNG.nextInt(STREET_NAMES.length)];
    }

    public static double price()    { return Math.round((1.0  + RNG.nextDouble() * 99.0) * 100.0) / 100.0; }
    public static double cost()     { return Math.round((0.5  + RNG.nextDouble() * 50.0) * 100.0) / 100.0; }
    public static int    quantity() { return RNG.nextInt(20) + 1; }
}

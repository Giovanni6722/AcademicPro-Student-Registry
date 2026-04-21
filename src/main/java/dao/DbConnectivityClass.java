package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Major;
import model.Person;
import service.MyLogger;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data-access layer for the AcademicPro Student Registry.
 *
 * Connection strategy:
 *  1. Attempt to connect to Azure MariaDB / MySQL.
 *  2. If that fails, transparently fall back to an embedded Apache Derby
 *     database stored in the user's home directory.
 */
public class DbConnectivityClass {

    // Azure / MySQL configuration
    //leaving these blank and using db, but you could use an azure server by changing these if u wanted
    private static final String DB_NAME        = "CSC311_BD_TEMP";
    private static final String SQL_SERVER_URL = "jdbc:mysql://localhost:3306";
    private static final String DB_URL         = SQL_SERVER_URL + "/" + DB_NAME;
    private static final String USERNAME       = "root";
    private static final String PASSWORD       = "password";

    /* ── Derby fallback configuration ─────────────────────────────────────── */
    private static final String DERBY_HOME = System.getProperty("user.home")
            + File.separator + "AcademicProDB";
    private static final String DERBY_URL  = "jdbc:derby:" + DERBY_HOME + ";create=true";

    /* ── State ─────────────────────────────────────────────────────────────── */
    private boolean useDerby = false;
    private final ObservableList<Person> data = FXCollections.observableArrayList();

    // ── Constructor ──────────────────────────────────────────────────────────

    public DbConnectivityClass() {
        useDerby = !connectToDatabase();
        if (useDerby) {
            MyLogger.makeLog("Azure unavailable – switching to embedded Derby.");
            connectToDerby();
        }
    }

    // ── Connection helpers ───────────────────────────────────────────────────

    private Connection getConnection() throws SQLException {
        if (useDerby) {
            return DriverManager.getConnection(DERBY_URL);
        }
        return DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
    }

    /**
     * Attempts MySQL/MariaDB connection and creates DB + table if absent.
     * @return true if connection succeeded, false otherwise.
     */
    public boolean connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(SQL_SERVER_URL, USERNAME, PASSWORD);
            conn.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            conn.close();

            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            createUsersTableMySQL(conn);
            conn.close();
            return true;
        } catch (Exception e) {
            MyLogger.makeLog("MySQL connect failed: " + e.getMessage());
            return false;
        }
    }

    private void createUsersTableMySQL(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id          INT(10)      NOT NULL PRIMARY KEY AUTO_INCREMENT,"
                + "first_name  VARCHAR(200) NOT NULL,"
                + "last_name   VARCHAR(200) NOT NULL,"
                + "department  VARCHAR(200),"
                + "major       VARCHAR(200),"
                + "email       VARCHAR(200) NOT NULL UNIQUE,"
                + "imageURL    VARCHAR(200))";
        conn.createStatement().executeUpdate(sql);
    }

    private void connectToDerby() {
        try {
            Connection conn = DriverManager.getConnection(DERBY_URL);
            createUsersTableDerby(conn);
            conn.close();
            MyLogger.makeLog("Derby database ready at: " + DERBY_HOME);
        } catch (SQLException e) {
            MyLogger.makeLog("Derby init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createUsersTableDerby(Connection conn) throws SQLException {
        try {
            String sql = "CREATE TABLE users ("
                    + "id         INT NOT NULL GENERATED ALWAYS AS IDENTITY"
                    + "           (START WITH 1, INCREMENT BY 1) PRIMARY KEY,"
                    + "first_name VARCHAR(200) NOT NULL,"
                    + "last_name  VARCHAR(200) NOT NULL,"
                    + "department VARCHAR(200),"
                    + "major      VARCHAR(200),"
                    + "email      VARCHAR(200) NOT NULL UNIQUE,"
                    + "imageURL   VARCHAR(200))";
            conn.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e; // X0Y32 = table already exists
        }
    }

    // ── CRUD operations ──────────────────────────────────────────────────────

    public ObservableList<Person> getData() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users");
             ResultSet rs = ps.executeQuery()) {

            if (!rs.isBeforeFirst()) { MyLogger.makeLog("No data found."); }
            while (rs.next()) {
                data.add(new Person(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("department"),
                        rs.getString("major"),
                        rs.getString("email"),
                        rs.getString("imageURL")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public void insertUser(Person person) {
        String sql = "INSERT INTO users (first_name, last_name, department, major, email, imageURL)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, person.getFirstName());
            ps.setString(2, person.getLastName());
            ps.setString(3, person.getDepartment());
            ps.setString(4, person.getMajor());
            ps.setString(5, person.getEmail());
            ps.setString(6, person.getImageURL());
            int rows = ps.executeUpdate();
            if (rows > 0) MyLogger.makeLog("Inserted: " + person.getEmail());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void editUser(int id, Person p) {
        String sql = "UPDATE users SET first_name=?, last_name=?, department=?,"
                + " major=?, email=?, imageURL=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setString(3, p.getDepartment());
            ps.setString(4, p.getMajor());
            ps.setString(5, p.getEmail());
            ps.setString(6, p.getImageURL());
            ps.setInt(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRecord(Person person) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, person.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int retrieveId(Person p) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM users WHERE email=?")) {
            ps.setString(1, p.getEmail());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public void queryUserByLastName(String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM users WHERE last_name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MyLogger.makeLog("Found: " + rs.getString("first_name")
                        + " " + rs.getString("last_name"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void listAllUsers() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MyLogger.makeLog("ID:" + rs.getInt("id") + " "
                        + rs.getString("first_name") + " " + rs.getString("last_name"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── CSV Import / Export ──────────────────────────────────────────────────

    public void exportToCSV(String filePath, ObservableList<Person> people) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("id,first_name,last_name,department,major,email,imageURL");
            writer.newLine();
            for (Person p : people) {
                writer.write(String.join(",",
                        String.valueOf(p.getId()),
                        escapeCSV(p.getFirstName()),
                        escapeCSV(p.getLastName()),
                        escapeCSV(p.getDepartment()),
                        escapeCSV(p.getMajor()),
                        escapeCSV(p.getEmail()),
                        escapeCSV(p.getImageURL() != null ? p.getImageURL() : "")));
                writer.newLine();
            }
        }
        MyLogger.makeLog("Exported " + people.size() + " records to " + filePath);
    }

    public ObservableList<Person> importFromCSV(String filePath) throws IOException {
        ObservableList<Person> imported = FXCollections.observableArrayList();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = parseCSVLine(line);
                if (parts.length >= 6) {
                    imported.add(new Person(
                            parts.length > 1 ? parts[1].trim() : "",
                            parts.length > 2 ? parts[2].trim() : "",
                            parts.length > 3 ? parts[3].trim() : "",
                            parts.length > 4 ? parts[4].trim() : Major.UNDECIDED.name(),
                            parts.length > 5 ? parts[5].trim() : "",
                            parts.length > 6 ? parts[6].trim() : ""));
                }
            }
        }
        MyLogger.makeLog("Imported " + imported.size() + " records from " + filePath);
        return imported;
    }

    //PDF Report (Extra Credit)

    /**
     * Generates a PDF report using iText 5 accessed via reflection so the
     * library stays in the unnamed module (classpath) without needing a
     * 'requires' directive in module-info.java.
     */
    @SuppressWarnings("unchecked")
    public void generatePDFReport(String filePath, ObservableList<Person> people)
            throws Exception {

        Class<?> docClass     = Class.forName("com.itextpdf.text.Document");
        Class<?> writerClass  = Class.forName("com.itextpdf.text.pdf.PdfWriter");
        Class<?> paraClass    = Class.forName("com.itextpdf.text.Paragraph");
        Class<?> tableClass   = Class.forName("com.itextpdf.text.pdf.PdfPTable");
        Class<?> fontClass    = Class.forName("com.itextpdf.text.Font");
        Class<?> fontFamClass = Class.forName("com.itextpdf.text.Font$FontFamily");
        Class<?> phraseClass  = Class.forName("com.itextpdf.text.Phrase");
        Class<?> cellClass    = Class.forName("com.itextpdf.text.pdf.PdfPCell");
        Class<?> elementClass = Class.forName("com.itextpdf.text.Element");

        Object doc = docClass.getDeclaredConstructor().newInstance();
        writerClass.getMethod("getInstance", docClass, OutputStream.class)
                .invoke(null, doc, new FileOutputStream(filePath));
        docClass.getMethod("open").invoke(doc);

        // Title
        Object boldFont = fontClass.getDeclaredConstructor(fontFamClass, float.class, int.class)
                .newInstance(Enum.valueOf((Class<Enum>) fontFamClass, "HELVETICA"),
                        20f, fontClass.getField("BOLD").getInt(null));
        Object titlePara = paraClass.getDeclaredConstructor(String.class, fontClass)
                .newInstance("AcademicPro Student Registry – Major Report\n\n", boldFont);
        paraClass.getMethod("setAlignment", int.class)
                .invoke(titlePara, elementClass.getField("ALIGN_CENTER").getInt(null));
        docClass.getMethod("add", Class.forName("com.itextpdf.text.Element"))
                .invoke(doc, titlePara);

        // Date + total
        Object bodyFont = fontClass.getDeclaredConstructor(fontFamClass, float.class, int.class)
                .newInstance(Enum.valueOf((Class<Enum>) fontFamClass, "HELVETICA"),
                        11f, fontClass.getField("NORMAL").getInt(null));
        Object datePara = paraClass.getDeclaredConstructor(String.class, fontClass)
                .newInstance("Generated: " + LocalDate.now()
                        + "    Total Students: " + people.size() + "\n\n", bodyFont);
        docClass.getMethod("add", Class.forName("com.itextpdf.text.Element"))
                .invoke(doc, datePara);

        // Count by major table
        Map<String, Long> countByMajor = people.stream()
                .collect(Collectors.groupingBy(Person::getMajor, Collectors.counting()));

        Object summaryTable = tableClass.getDeclaredConstructor(int.class).newInstance(2);
        tableClass.getMethod("setWidthPercentage", float.class).invoke(summaryTable, 55f);
        addCell(summaryTable, cellClass, phraseClass, fontClass, "Major",   boldFont);
        addCell(summaryTable, cellClass, phraseClass, fontClass, "Count",   boldFont);
        for (Map.Entry<String, Long> e : countByMajor.entrySet()) {
            addCell(summaryTable, cellClass, phraseClass, fontClass, e.getKey(),             bodyFont);
            addCell(summaryTable, cellClass, phraseClass, fontClass, String.valueOf(e.getValue()), bodyFont);
        }
        docClass.getMethod("add", Class.forName("com.itextpdf.text.Element"))
                .invoke(doc, summaryTable);

        // Full student list
        Object listHeader = paraClass.getDeclaredConstructor(String.class, fontClass)
                .newInstance("\n\nFull Student List\n\n", boldFont);
        docClass.getMethod("add", Class.forName("com.itextpdf.text.Element"))
                .invoke(doc, listHeader);

        Object fullTable = tableClass.getDeclaredConstructor(int.class).newInstance(4);
        tableClass.getMethod("setWidthPercentage", float.class).invoke(fullTable, 100f);
        for (String h : new String[]{"Name", "Department", "Major", "Email"}) {
            addCell(fullTable, cellClass, phraseClass, fontClass, h, boldFont);
        }
        for (Person p : people) {
            addCell(fullTable, cellClass, phraseClass, fontClass,
                    p.getFirstName() + " " + p.getLastName(), bodyFont);
            addCell(fullTable, cellClass, phraseClass, fontClass,
                    p.getDepartment() != null ? p.getDepartment() : "", bodyFont);
            addCell(fullTable, cellClass, phraseClass, fontClass,
                    p.getMajor() != null ? p.getMajor() : "", bodyFont);
            addCell(fullTable, cellClass, phraseClass, fontClass,
                    p.getEmail(), bodyFont);
        }
        docClass.getMethod("add", Class.forName("com.itextpdf.text.Element"))
                .invoke(doc, fullTable);

        docClass.getMethod("close").invoke(doc);
        MyLogger.makeLog("PDF report generated: " + filePath);
    }

    @SuppressWarnings("unchecked")
    private void addCell(Object table, Class<?> cellClass, Class<?> phraseClass,
                         Class<?> fontClass, String text, Object font) throws Exception {
        Object phrase = phraseClass.getDeclaredConstructor(String.class, fontClass)
                .newInstance(text, font);
        Object cell   = cellClass.getDeclaredConstructor(phraseClass).newInstance(phrase);
        table.getClass().getMethod("addCell", cellClass).invoke(table, cell);
    }

    // ── CSV helpers ──────────────────────────────────────────────────────────

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    public boolean isUsingDerby() { return useDerby; }
}
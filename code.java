import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;


public class code {


    // establishing connection
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/";
    private static final Map<String, String> DATABASE_TO_CSV = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    static {
        // Mapping each database name to its corresponding CSV file
        DATABASE_TO_CSV.put("Mix", "38618354.csv");
        DATABASE_TO_CSV.put("Jazz_funk", "38770229.csv");
        DATABASE_TO_CSV.put("Rock_pop", "38054124.csv");
    }

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL)) {
            for (String databaseName : DATABASE_TO_CSV.keySet()) {
                String csvFilePath = DATABASE_TO_CSV.get(databaseName);
                initializeDatabase(connection, databaseName);
                processCSVAndPopulateTables(connection, csvFilePath, databaseName);
                executeDeletionQueries(connection, databaseName);
                executeGroupByHavingQueries(connection, databaseName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DML STATEMENTS
    private static void executeDeletionQueries(Connection connection, String databaseName) throws SQLException {
        String deleteArtistSql = "DELETE FROM Artist WHERE Artist_ID = ?";
        String deleteAlbumSql = "DELETE FROM Album WHERE Album_ID = ?";

        try (PreparedStatement psArtist = connection.prepareStatement(deleteArtistSql);
             PreparedStatement psAlbum = connection.prepareStatement(deleteAlbumSql)) {

            // Attempt to delete an artist and an album by ID
            psArtist.setInt(1, 1);
            psAlbum.setInt(1, 1);

            try {
                psArtist.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Artist deletion failed due to referential integrity: " + e.getMessage());
            }

            try {
                psAlbum.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Album deletion failed due to referential integrity: " + e.getMessage());
            }
        }
    }

    //personal  - GROUB BY 
    private static void executeGroupByHavingQueries(Connection connection, String databaseName) throws SQLException {
        String countSongsPerAlbum = "SELECT Album_ID, COUNT(*) FROM Song GROUP BY Album_ID HAVING COUNT(*) > 1";
        String countAlbumsPerArtist = "SELECT Artist_ID, COUNT(*) FROM Album GROUP BY Artist_ID HAVING COUNT(*) > 1";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rsSongs = stmt.executeQuery(countSongsPerAlbum);
            while (rsSongs.next()) {
                System.out.println("Album ID " + rsSongs.getInt(1) + " has " + rsSongs.getInt(2) + " songs");
            }

            ResultSet rsAlbums = stmt.executeQuery(countAlbumsPerArtist);
            while (rsAlbums.next()) {
                System.out.println("Artist ID " + rsAlbums.getInt(1) + " has " + rsAlbums.getInt(2) + " albums");
            }
        }
    }

    //DDL STATEMENTS
    private static void initializeDatabase(Connection connection, String databaseName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + databaseName);
            statement.execute("USE " + databaseName);

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Artist (Artist_ID INT AUTO_INCREMENT PRIMARY KEY, Artist_name VARCHAR(255) UNIQUE NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Album (Album_ID INT AUTO_INCREMENT PRIMARY KEY, Album_name VARCHAR(255) UNIQUE NOT NULL, Artist_ID INT, FOREIGN KEY (Artist_ID) REFERENCES Artist(Artist_ID))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Song (Song_ID INT AUTO_INCREMENT PRIMARY KEY, Song_name VARCHAR(255), Genre VARCHAR(50), Artist_ID INT, Album_ID INT, Duration VARCHAR(5), Release_date DATE, FOREIGN KEY (Artist_ID) REFERENCES Artist(Artist_ID), FOREIGN KEY (Album_ID) REFERENCES Album(Album_ID))");
        }
    }


    //populating table
    private static void processCSVAndPopulateTables(Connection connection, String csvFilePath, String databaseName) {
        Map<String, Integer> artistIds = new HashMap<>();
        Map<String, Integer> albumIds = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            br.readLine(); // Skip header

            while (br.ready()) {
                String line = br.readLine();
                if (line != null) {
                    String[] data = line.split(",");
                    String artistName = data[3].trim();  
                    String albumName = data[4].trim();

                    int artistId = getOrCreateArtist(connection, artistName, artistIds);
                    int albumId = getOrCreateAlbum(connection, albumName, artistId, albumIds);

                    insertSong(connection, data, artistId, albumId);
                }
            }

            System.out.println("Data successfully inserted for database: " + databaseName);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getOrCreateArtist(Connection connection, String artistName, Map<String, Integer> artistIds) throws SQLException {
        if (artistIds.containsKey(artistName)) {
            return artistIds.get(artistName);
        }
    
        String selectArtistSql = "SELECT Artist_ID FROM Artist WHERE Artist_name = ?";
        try (PreparedStatement selectPs = connection.prepareStatement(selectArtistSql)) {
            selectPs.setString(1, artistName);
            ResultSet rs = selectPs.executeQuery();
            if (rs.next()) {
                int artistId = rs.getInt("Artist_ID");
                artistIds.put(artistName, artistId);
                return artistId;
            }
        }


        // DML statement (insert)
        String insertArtistSql = "INSERT INTO Artist (Artist_name) VALUES (?)";
        try (PreparedStatement insertPs = connection.prepareStatement(insertArtistSql, Statement.RETURN_GENERATED_KEYS)) {
            insertPs.setString(1, artistName);
            insertPs.executeUpdate();
            try (ResultSet rs = insertPs.getGeneratedKeys()) {
                if (rs.next()) {
                    int artistId = rs.getInt(1);
                    artistIds.put(artistName, artistId);
                    return artistId;
                }
            }
        }
        throw new SQLException("Failed to insert artist or retrieve its ID.");
    }

    private static int getOrCreateAlbum(Connection connection, String albumName, int artistId, Map<String, Integer> albumIds) throws SQLException {
        if (albumIds.containsKey(albumName)) {
            return albumIds.get(albumName);
        }
    
        String selectAlbumSql = "SELECT Album_ID FROM Album WHERE Album_name = ? AND Artist_ID = ?";
        try (PreparedStatement selectPs = connection.prepareStatement(selectAlbumSql)) {
            selectPs.setString(1, albumName);
            selectPs.setInt(2, artistId);
            ResultSet rs = selectPs.executeQuery();
            if (rs.next()) {
                int albumId = rs.getInt("Album_ID");
                albumIds.put(albumName, albumId);
                return albumId;
            }
        } catch (SQLException e) {
            System.out.println("Checking for existing album failed: " + e.getMessage());
            throw e;  // Re-throw the exception after logging it.
        }
        
        //inseting albums (DML)
        String insertAlbumSql = "INSERT INTO Album (Album_name, Artist_ID) VALUES (?, ?)";
        try (PreparedStatement insertPs = connection.prepareStatement(insertAlbumSql, Statement.RETURN_GENERATED_KEYS)) {
            insertPs.setString(1, albumName);
            insertPs.setInt(2, artistId);
            insertPs.executeUpdate();
            try (ResultSet rs = insertPs.getGeneratedKeys()) {
                if (rs.next()) {
                    int albumId = rs.getInt(1);
                    albumIds.put(albumName, albumId);
                    return albumId;
                }
            }
        } catch (SQLException e) {
            System.out.println("Inserting new album failed: " + e.getMessage());
            throw e;  // Re-throw the exception after logging it.
        }
    
        throw new SQLException("Failed to insert album or retrieve its ID.");
    }
    
    //insertion of songs
    private static void insertSong(Connection connection, String[] data, int artistId, int albumId) throws SQLException {
        String insertQuery = "INSERT INTO Song (Song_name, Genre, Artist_ID, Album_ID, Duration, Release_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
            ps.setString(1, data[1].trim());
            ps.setString(2, data[2].trim());
            ps.setInt(3, artistId);
            ps.setInt(4, albumId);
            ps.setString(5, data[5].trim());
            ps.setDate(6, new java.sql.Date(DATE_FORMAT.parse(data[6].trim()).getTime()));
            ps.executeUpdate();
        } catch (ParseException e) {
            throw new SQLException("Failed to parse the release date.");
        }
    }
}
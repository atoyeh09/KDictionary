package DAL;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JOptionPane;

public class DbDao implements IDbDao {

	private String URL;
	private String USER;
	private String PASSWORD;
	private static final Logger logger = LogManager.getLogger(DbDao.class);
	Connection connection;

	public DbDao(String PATH) {
		logger.debug("Initialization");

		Properties properties = new Properties();
		try (FileInputStream inputStream = new FileInputStream(PATH + "/config.properties")) {
			properties.load(inputStream);
			URL = properties.getProperty("db.url");
			USER = properties.getProperty("db.user");
			PASSWORD = properties.getProperty("db.password");
			logger.info("DBDAO initialized and configuration loaded.");
		} catch (IOException e) {
			logger.error("Error reading properties file", e);
		}

		try {
			connection = DriverManager.getConnection(URL, USER, PASSWORD);

			String query = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'kdictionary'";
			try (Statement stmt = connection.createStatement(); ResultSet resultSet = stmt.executeQuery(query)) {
				if (!resultSet.next()) {
					String createDbQuery = "CREATE DATABASE IF NOT EXISTS kdictionary";
					stmt.executeUpdate(createDbQuery);
					logger.info("Database 'kdictionary' created or already exists.");
				}
			}

			connection.setCatalog("kdictionary");

			String createArabicTable = "CREATE TABLE IF NOT EXISTS arabic ("
					+ "arid INT AUTO_INCREMENT PRIMARY KEY, "
					+ "arabicWord VARCHAR(255) UNIQUE)";
			String createPersianTable = "CREATE TABLE IF NOT EXISTS persian ("
					+ "arid INT, "
					+ "persianWord VARCHAR(255), "
					+ "FOREIGN KEY (arid) REFERENCES arabic(arid) ON DELETE CASCADE)";
			String createUrduTable = "CREATE TABLE IF NOT EXISTS urdu ("
					+ "arid INT, "
					+ "urduWord VARCHAR(255), "
					+ "FOREIGN KEY (arid) REFERENCES arabic(arid) ON DELETE CASCADE)";
			String createFavoriteTable = "CREATE TABLE IF NOT EXISTS favorite ("
					+ "favid INT PRIMARY KEY AUTO_INCREMENT, "
					+ "arabic VARCHAR(255) NOT NULL)";
			String createHistoryTable = "CREATE TABLE IF NOT EXISTS history ("
					+ "history_id INT AUTO_INCREMENT PRIMARY KEY, "
					+ "searchedWord VARCHAR(255), "
					+ "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

			try (Statement stmt = connection.createStatement()) {
				stmt.executeUpdate(createArabicTable);
				stmt.executeUpdate(createPersianTable);
				stmt.executeUpdate(createUrduTable);
				stmt.executeUpdate(createFavoriteTable);
				stmt.executeUpdate(createHistoryTable);
				logger.info("Tables created or already exist.");
			}
		} catch (SQLException e) {
			logger.error("Error establishing connection or creating database/tables", e);
		}
	}

	@Override
	public boolean isDatabaseConnected() {
		try {
			boolean isConnected = connection != null && !connection.isClosed();
			if (isConnected) {
				logger.debug("Database is connected.");
			} else {
				logger.warn("Database is not connected.");
			}
			return isConnected;
		} catch (SQLException e) {
			logger.error("Error checking database connection", e);
			return false;
		}
	}

	@Override
	public void InsertToHistory(String searchedWord) {
		if (!isDatabaseConnected()) {
			logger.error("Failed to insert to history. Database not connected.");
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String wordToAdd = searchedWord;
		String insertHistory = "INSERT INTO history (searchedWord) VALUES (?)";
		try (PreparedStatement stmt = connection.prepareStatement(insertHistory)) {
			stmt.setString(1, wordToAdd);
			stmt.executeUpdate();
			logger.info("Inserted word into history: {}", wordToAdd);
		} catch (SQLException e) {
			logger.error("Error inserting into history", e);
			JOptionPane.showMessageDialog(null, "Error inserting into history.", "Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public String[][] ReadHistoryWords() {
		if (!isDatabaseConnected()) {
			logger.error("Failed to read history. Database not connected.");
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return new String[0][0];
		}
		List<String[]> wordList = new ArrayList<>();
		String query = "SELECT searchedWord FROM history ORDER BY timestamp DESC";

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(query)) {

			logger.debug("Executing query to fetch history words: {}", query);
			while (resultSet.next()) {
				String searchedWord = resultSet.getString("searchedWord");
				wordList.add(new String[]{searchedWord});
			}
			logger.info("Fetched {} history words from the database.", wordList.size());
		} catch (SQLException e) {
			logger.error("Error fetching history words from the database", e);
			JOptionPane.showMessageDialog(null, "Error fetching history words from the database.",
					"Database Error", JOptionPane.ERROR_MESSAGE);
			return new String[0][0];
		}

		String[][] data = new String[wordList.size()][1];
		for (int i = 0; i < wordList.size(); i++) {
			data[i] = wordList.get(i);
		}

		logger.debug("Returning {} history words to the caller.", wordList.size());
		return data;
	}

	@Override
	public String InsertWord(String arabic, String[] persian, String[] urdu) {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}
		try {
			int arid = GetArabicId(arabic);
			if (arid == -1) {
				String insertArabic = "INSERT INTO arabic (arabicWord) VALUES (?)";
				try (PreparedStatement stmt = connection.prepareStatement(insertArabic, Statement.RETURN_GENERATED_KEYS)) {
					stmt.setString(1, arabic);
					int rowsInserted = stmt.executeUpdate();
					if (rowsInserted > 0) {
						ResultSet rs = stmt.getGeneratedKeys();
						if (rs.next()) {
							arid = rs.getInt(1);
						}
					}
				}
			}

			String insertPersian = "INSERT INTO persian (arid, persianWord) VALUES (?, ?)";
			try (PreparedStatement stmt = connection.prepareStatement(insertPersian)) {
				for (String p : persian) {
					stmt.setInt(1, arid);
					stmt.setString(2, p);
					stmt.addBatch();
				}
				stmt.executeBatch();
			}

			String insertUrdu = "INSERT INTO urdu (arid, urduWord) VALUES (?, ?)";
			try (PreparedStatement stmt = connection.prepareStatement(insertUrdu)) {
				for (String u : urdu) {
					stmt.setInt(1, arid);
					stmt.setString(2, u);
					stmt.addBatch();
				}
				stmt.executeBatch();
			}

			return "Inserted: " + arabic + " <- " + String.join(", ", persian) + " <- " + String.join(", ", urdu);

		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error inserting word into the database", e);
			return "Insertion failed: " + e.getMessage();
		}
	}

	private int GetArabicId(String arabic) {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return -1;
		}
		logger.debug("Getting Arabic word id");
		String sql = "SELECT arid FROM arabic WHERE arabicWord = ?";
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, arabic);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("arid");
			}
			logger.info("Successfully got id");
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error fetching Arabic ID", e);
		}
		return -1;
	}

	@Override
	public String[][] ReadAllWords() {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return new String[0][0];
		}
		logger.debug("Fetching data from database...");
		List<String[]> wordList = new ArrayList<>();
		String query = "SELECT arabicWord FROM arabic GROUP BY arid ";

		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(query)) {

			while (resultSet.next()) {
				String arabic = resultSet.getString("arabicWord");
				wordList.add(new String[]{arabic});
			}
			logger.info("Data fetched successfully.");
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error fetching all words from the database", e);
		}

		String[][] data = new String[wordList.size()][1];
		for (int i = 0; i < wordList.size(); i++) {
			data[i] = wordList.get(i);
		}

		return data;
	}

	@Override
	public String SearchByArabic(String arabic) {
		if (!isDatabaseConnected()) {
			logger.error("SearchByArabic failed. Database not connected.");
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}

		String sql = "SELECT a.arabicWord, p.persianWord, u.urduWord FROM arabic a " +
				"JOIN persian p ON a.arid = p.arid " +
				"JOIN urdu u ON a.arid = u.arid WHERE a.arabicWord = ?";
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			logger.debug("Executing query to search by Arabic word: {}", sql);
			stmt.setString(1, arabic);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String persian = rs.getString("persianWord");
				String urdu = rs.getString("urduWord");
				logger.info("Search successful for Arabic word: {}. Results: Persian={}, Urdu={}", arabic, persian, urdu);
				return "Searched: " + arabic + " <- " + persian + " <- " + urdu;
			} else {
				logger.warn("Arabic word '{}' not found in the database.", arabic);
				return "Arabic word not found.";
			}
		} catch (SQLException e) {
			logger.error("Error executing search for Arabic word: {}", arabic, e);
			return "Error searching for the word: " + e.getMessage();
		}
	}

	@Override
	public String SearchByPersian(String persian) {
		if (!isDatabaseConnected()) {
			logger.error("SearchByPersian failed. Database not connected.");
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}

		String sql = "SELECT a.arabicWord, p.persianWord, u.urduWord FROM arabic a " +
				"JOIN persian p ON a.arid = p.arid " +
				"JOIN urdu u ON a.arid = u.arid WHERE p.persianWord = ?";
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			logger.debug("Executing query to search by Persian word: {}", sql);
			stmt.setString(1, persian);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String arabic = rs.getString("arabicWord");
				String urdu = rs.getString("urduWord");
				logger.info("Search successful for Persian word: {}. Results: Arabic={}, Urdu={}", persian, arabic, urdu);
				return "Searched: " + arabic + " <- " + persian + " <- " + urdu;
			} else {
				logger.warn("Persian word '{}' not found in the database.", persian);
				return "Persian word not found.";
			}
		} catch (SQLException e) {
			logger.error("Error executing search for Persian word: {}", persian, e);
			return "Error searching for the meaning: " + e.getMessage();
		}
	}


	@Override
	public String SearchByUrdu(String urdu) {
		if (!isDatabaseConnected()) {
			logger.error("SearchByUrdu failed. Database not connected.");
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}

		String sql = "SELECT a.arabicWord, p.persianWord, u.urduWord FROM arabic a " +
				"JOIN persian p ON a.arid = p.arid " +
				"JOIN urdu u ON a.arid = u.arid WHERE u.urduWord = ?";
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			logger.debug("Executing query to search by Urdu word: {}", sql);
			stmt.setString(1, urdu);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String arabic = rs.getString("arabicWord");
				String persian = rs.getString("persianWord");
				logger.info("Search successful for Urdu word: {}. Results: Arabic={}, Persian={}", urdu, arabic, persian);
				return "Searched: " + arabic + " <- " + persian + " <- " + urdu;
			} else {
				logger.warn("Urdu word '{}' not found in the database.", urdu);
				return "Urdu word not found.";
			}
		} catch (SQLException e) {
			logger.error("Error searching for Urdu word: {}", urdu, e);
			return "Error searching for the meaning: " + e.getMessage();
		}
	}

	@Override
	public String updateWord(String oldArabic, String newArabic, String persian, String urdu) {
		if (!isDatabaseConnected()) {
			logger.error("updateWord failed. Database not connected.");
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}

		try {
			logger.debug("Attempting to update word. OldArabic: {}, NewArabic: {}, Persian: {}, Urdu: {}",
					oldArabic, newArabic, persian, urdu);

			int arid = GetArabicId(oldArabic);
			if (arid == -1) {
				logger.warn("Arabic word '{}' not found in the database.", oldArabic);
				return "Arabic word not found.";
			}

			// Update Arabic word if changed
			if (!newArabic.isEmpty() && !oldArabic.equals(newArabic)) {
				String updateArabic = "UPDATE arabic SET arabicWord = ? WHERE arid = ?";
				try (PreparedStatement stmt = connection.prepareStatement(updateArabic)) {
					stmt.setString(1, newArabic);
					stmt.setInt(2, arid);
					stmt.executeUpdate();
					logger.info("Updated Arabic word. Old: {}, New: {}", oldArabic, newArabic);
				}
			}

			// Update Persian word
			String updateDict = "UPDATE persian SET persianWord = ? WHERE arid = ?";
			try (PreparedStatement stmt = connection.prepareStatement(updateDict)) {
				stmt.setString(1, persian.isEmpty() ? null : persian);
				stmt.setInt(2, arid);
				stmt.executeUpdate();
				logger.info("Updated Persian word for arid {}: {}", arid, persian);
			}

			// Update Urdu word
			String updateUrdu = "UPDATE urdu SET urduWord = ? WHERE arid = ?";
			try (PreparedStatement stmt = connection.prepareStatement(updateUrdu)) {
				stmt.setString(1, urdu.isEmpty() ? null : urdu);
				stmt.setInt(2, arid);
				stmt.executeUpdate();
				logger.info("Updated Urdu word for arid {}: {}", arid, urdu);
			}

			logger.info("Word update completed successfully for OldArabic: {}", oldArabic);
			return "Word updated successfully.";

		} catch (SQLException e) {
			logger.error("Error updating word in the database. OldArabic: {}, NewArabic: {}, Persian: {}, Urdu: {}",
					oldArabic, newArabic, persian, urdu, e);
			return "Error updating the word: " + e.getMessage();
		}
	}

	@Override
	public String DeleteWordByArabic(String arabic) {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}
		logger.debug("Deleting word: " + arabic);
		try {
			int arid = GetArabicId(arabic);
			if (arid == -1) {
				return "Arabic word not found.";
			}

			String deletePersian = "DELETE FROM persian WHERE arid = ?";
			try (PreparedStatement stmt = connection.prepareStatement(deletePersian)) {
				stmt.setInt(1, arid);
				stmt.executeUpdate();
			}

			String deleteUrdu = "DELETE FROM urdu WHERE arid = ?";
			try (PreparedStatement stmt = connection.prepareStatement(deleteUrdu)) {
				stmt.setInt(1, arid);
				stmt.executeUpdate();
			}

			String deleteArabic = "DELETE FROM arabic WHERE arid = ?";
			try (PreparedStatement stmt = connection.prepareStatement(deleteArabic)) {
				stmt.setInt(1, arid);
				stmt.executeUpdate();
			}

			logger.debug("Word deleted successfully.");
			return "Word deleted successfully.";
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("Error deleting word from the database", e);
			return "Error deleting the word: " + e.getMessage();
		}
	}

	@Override
	public String GetWordMeaning(String word) throws Exception {
		return "";
	}

	@Override
	public String AddFavoriteWord(String arabicWord) {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}
		String insertFavorite = "INSERT INTO favorite (arabic) VALUES (?)";
		try (PreparedStatement stmt = connection.prepareStatement(insertFavorite)) {
			stmt.setString(1, arabicWord);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return "Error adding word to favorites: " + e.getMessage();
		}
		return "Word added successfully.";
	}

	@Override
	public String DeleteFavoriteWord(String arabicWord) {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return "DB not connected";
		}
		String deleteFavorite = "DELETE FROM favorite WHERE arabic = ?";
		try (PreparedStatement stmt = connection.prepareStatement(deleteFavorite)) {
			stmt.setString(1, arabicWord);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return "Error removing word from favorites: " + e.getMessage();
		}
		return "Word deleted successfully.";
	}

	@Override
	public String[][] AllFavoriteWords() {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return new String[0][0];
		}
		List<String[]> favoriteList = new ArrayList<>();
		String query = "SELECT arabic FROM favorite";

		try (Statement stmt = connection.createStatement();
			 ResultSet rs = stmt.executeQuery(query)) {

			while (rs.next()) {
				String arabicWord = rs.getString("arabic");
				favoriteList.add(new String[]{arabicWord});
			}
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error fetching favorite words from the database.",
					"Database Error", JOptionPane.ERROR_MESSAGE);
			return new String[0][0];
		}

		String[][] data = new String[favoriteList.size()][1];
		for (int i = 0; i < favoriteList.size(); i++) {
			data[i] = favoriteList.get(i);
		}

		return data;
	}

	@Override
	public String[][] favoriteWords(String arabic, int choice) {
		if (!isDatabaseConnected()) {
			JOptionPane.showMessageDialog(null, "DB not connected", "Database Error", JOptionPane.ERROR_MESSAGE);
			return new String[][] {{"DB not connected"}};
		}

		String checkSql = "SELECT COUNT(*) FROM favorite WHERE arabic = ?";
		String insertSql = "INSERT INTO favorite(arabic) VALUES (?);";
		String deleteSql = "DELETE FROM favorite WHERE arabic = ?;";
		String selectSql = "SELECT arabic FROM favorite";

		try {
			if (choice == 0) {
				try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
					ResultSet rs = selectStmt.executeQuery();
					List<String[]> resultList = new ArrayList<>();

					while (rs.next()) {
						resultList.add(new String[]{rs.getString("arabic")});
					}

					String[][] resultArray = new String[resultList.size()][1];
					resultList.toArray(resultArray);
					return resultArray;
				}
			} else if (choice == 1) {
				try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
					checkStmt.setString(1, arabic);
					ResultSet rs = checkStmt.executeQuery();

					if (rs.next() && rs.getInt(1) > 0) {
						return new String[][] {{"This word is already in your Favorites."}};
					}

					try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
						insertStmt.setString(1, arabic);
						int rowsAffected = insertStmt.executeUpdate();

						if (rowsAffected > 0) {
							return new String[][] {{"Added to Favorites"}};
						} else {
							return new String[][] {{"Failed to insert into Favorites."}};
						}
					}
				}
			} else if (choice == 2) {
				try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
					checkStmt.setString(1, arabic);
					ResultSet rs = checkStmt.executeQuery();

					if (rs.next() && rs.getInt(1) == 0) {
						return new String[][] {{"This word is not in your Favorites."}};
					}

					try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
						deleteStmt.setString(1, arabic);
						int rowsAffected = deleteStmt.executeUpdate();

						if (rowsAffected > 0) {
							return new String[][] {{"Deleted from Favorites"}};
						} else {
							return new String[][] {{"Failed to delete from Favorites."}};
						}
					}
				}
			} else {
				return new String[][] {{"Invalid choice."}};
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return new String[][] {{"Database error."}};
		}
	}
}

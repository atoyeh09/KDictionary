package DAL;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.ArrayList;


public class FascadeDao implements IFascadeDao {
    IDbDao dbDAO;

    public FascadeDao(String PATH) {
        dbDAO = new DbDao(PATH);
    }

    @Override
    public String InsertWord(String arabic, String[] urdu, String[] persian) {
        return dbDAO.InsertWord(arabic, urdu, persian);
    }

    @Override
    public String[][] ReadAllWords() {
        return dbDAO.ReadAllWords();
    }

    @Override
    public void InsertToHistory(String searchedWord) {
        dbDAO.InsertToHistory(searchedWord);
    }

    @Override
    public String[][] ReadHistoryWords() {
        return dbDAO.ReadHistoryWords();
    }

    @Override
    public String SearchByArabic(String arabic) {
        return dbDAO.SearchByArabic(arabic);
    }

    @Override
    public String SearchByPersian(String persian) {
        return dbDAO.SearchByPersian(persian);
    }

    @Override
    public String SearchByUrdu(String urdu) {
        return dbDAO.SearchByUrdu(urdu);
    }

    @Override
    public String updateWord(String oldArabic, String newArabic, String persian, String urdu) {
        return dbDAO.updateWord(oldArabic, newArabic, persian, urdu);
    }

    @Override
    public String DeleteWordByArabic(String arabic) {
        return dbDAO.DeleteWordByArabic(arabic);
    }

    @Override
    public String[] InsertWordScrape(String arabic, String persian, String urdu) {
        String[] result = new String[3];

        try {
            String[] persianArray = { persian };
            String[] urduArray = { urdu };

            String insertionResult = dbDAO.InsertWord(arabic, persianArray, urduArray);

            if (insertionResult.startsWith("Inserted")) {
                result[0] = "Success";
                result[1] = arabic;
                result[2] = persian;
            } else {
                result[0] = "Failure";
                result[1] = "Insertion failed";
                result[2] = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            result[0] = "Error";
            result[1] = "Error inserting the word";
            result[2] = e.getMessage();
        }

        return result;
    }

    @Override
    public String insertWordsFromCSV(String csvFilePath) {
        StringBuilder result = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            String arabic = null, persian = null, urdu = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.equals("(") || line.equals(")")) {
                    continue;
                }

                if (line.startsWith("A:")) {
                    arabic = line.substring(2).trim();
                } else if (line.startsWith("P:")) {
                    persian = line.substring(2).trim();
                } else if (line.startsWith("U:")) {
                    urdu = line.substring(2).trim();
                }

                if (arabic != null && persian != null && urdu != null) {
                    List<String> persianWords = new ArrayList<>();
                    List<String> urduWords = new ArrayList<>();

                    for (String word : persian.split("\\s*,\\s*")) {
                        persianWords.add(word.trim());
                    }

                    for (String word : urdu.split("\\s*,\\s*")) {
                        urduWords.add(word.trim());
                    }

                    if (persianWords.size() == urduWords.size()) {
                        for (int i = 0; i < persianWords.size(); i++) {
                            String[] pWord = new String[] { persianWords.get(i).trim() };
                            String[] uWord = new String[] { urduWords.get(i).trim() };

                            String insertionResult = InsertWord(arabic, pWord, uWord);
                            result.append(insertionResult).append("\n");
                        }
                    } else {
                        result.append("Skipping invalid entry (mismatched Persian and Urdu words): ")
                                .append("A: ").append(arabic)
                                .append(", P: ").append(persian)
                                .append(", U: ").append(urdu)
                                .append("\n");
                    }

                    arabic = null;
                    persian = null;
                    urdu = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading the file: " + e.getMessage();
        }

        return result.toString().trim();
    }

    @Override
    public String GetWordMeaning(String word) throws Exception {
        return dbDAO.GetWordMeaning(word);
    }

    @Override
    public String AddFavoriteWord(String arabic) {
        return dbDAO.AddFavoriteWord(arabic);
    }

    @Override
    public String DeleteFavoriteWord(String arabic) {
        return dbDAO.DeleteFavoriteWord(arabic);
    }

    @Override
    public String[][] AllFavoriteWords() {
        return dbDAO.AllFavoriteWords();
    }

    @Override
    public boolean isDatabaseConnected() {
        try {
            return dbDAO.isDatabaseConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String[][] favoriteWords(String arabic, int choice) {
        return new String[0][];
    }
}

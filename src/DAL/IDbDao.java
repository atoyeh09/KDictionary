package DAL;

public interface IDbDao {
    String InsertWord(String arabic, String[] persian, String[] urdu);
    String[][] ReadAllWords();
    String SearchByArabic(String arabic);
    String SearchByPersian(String persian);
    String SearchByUrdu(String urdu);
    String updateWord(String oldArabic, String newArabic, String persian, String urdu);
    String DeleteWordByArabic(String arabic);
    String GetWordMeaning(String word) throws Exception;
    String AddFavoriteWord(String arabic);
    String DeleteFavoriteWord(String arabic);
    String[][] AllFavoriteWords();
	void InsertToHistory(String searchedWord);
	String[][] ReadHistoryWords();

    boolean isDatabaseConnected();

    String[][] favoriteWords(String arabic, int choice);
}

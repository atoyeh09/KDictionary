package BLL;

public interface IDictionaryBo {
	String AddWord(String arabic, String persian, String urdu);
	String UpdateWord(String oldArabic, String newArabic, String persian, String urdu);
	String DeleteWord(String arabic);
	String[][] ReadAllWords();
	String SearchArabicWord(String arabic);
	String SearchPersianWord(String persian);
	String SearchUrduWord(String urdu);
	String PosTagging(String arabic);
	String RootWord(String arabic);
	String ImportCsv(String csvFilePath);
	String WordLemmatization(String arabic);
	Object[] InsertFromScrape(String arabic, String urdu, String persian);
	void ReadFileAndCheckMeanings(String filePath) throws Exception;
	void AddToHistory(String searchedWord);
	String[][] ReadHistoryWords();
	String[][] ManageFavoriteWords(String arabic, int i);
}

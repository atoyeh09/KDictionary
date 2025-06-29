package DAL;

public interface IFascadeDao extends IDbDao {
    String insertWordsFromCSV(String csvFilePath);
    String[] InsertWordScrape(String arabic, String persian, String urdu);
    boolean isDatabaseConnected();
}

//package test.java;
//
//import org.testng.*;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import DAL.DbDao;
//
//public class TestC {
//
//    private DbDao dbDao;
//
//    @BeforeClass
//    public void setUp() {
//        dbDao = new DbDao("/resources/config.properties");
//    }
//
//    @Test
//    public void testDatabaseConnection() {
//        boolean isConnected = dbDao.isDatabaseConnected();
//        Assert.assertTrue(isConnected, "Database should be connected.");
//    }
//
//    @Test
//    public void testInsertToHistory() {
//        String testWord = "TestHistoryWord";
//        dbDao.InsertToHistory(testWord);
//
//        String[][] history = dbDao.ReadHistoryWords();
//        boolean found = false;
//
//        for (String[] entry : history) {
//            if (entry[0].equals(testWord)) {
//                found = true;
//                break;
//            }
//        }
//        Assert.assertTrue(found, "History should contain the inserted word.");
//    }
//
//    @Test
//    public void testReadHistoryWords() {
//        String[][] history = dbDao.ReadHistoryWords();
//        Assert.assertNotEquals(history.length, 0, "History should not be empty.");
//    }
//
//    @Test
//    public void testInsertWord() {
//        String arabic = "TestArabic";
//        String[] persian = {"TestPersian1", "TestPersian2"};
//        String[] urdu = {"TestUrdu1", "TestUrdu2"};
//
//        String result = dbDao.InsertWord(arabic, persian, urdu);
//        Assert.assertTrue(result.contains("Inserted"), "Word should be inserted successfully.");
//    }
//
//    @Test
//    public void testSearchByArabic() {
//        String arabic = "TestArabic";
//
//        dbDao.InsertWord(arabic, new String[]{"TestPersian1"}, new String[]{"TestUrdu1"});
//
//        String result = dbDao.SearchByArabic(arabic);
//        Assert.assertTrue(result.contains(arabic), "Search result should contain the inserted Arabic word.");
//    }
//
//    @Test
//    public void testUpdateWord() {
//        String oldArabic = "TestArabic";
//        String newArabic = "UpdatedArabic";
//        String persian = "UpdatedPersian";
//        String urdu = "UpdatedUrdu";
//
//        dbDao.InsertWord(oldArabic, new String[]{"TestPersian1"}, new String[]{"TestUrdu1"});
//
//        String result = dbDao.updateWord(oldArabic, newArabic, persian, urdu);
//        Assert.assertTrue(result.contains("updated"), "Word should be updated successfully.");
//
//        String updatedResult = dbDao.SearchByArabic(newArabic);
//        Assert.assertTrue(updatedResult.contains(newArabic), "Search result should reflect the updated word.");
//    }
//
//    @Test
//    public void testDeleteWordByArabic() {
//        String arabic = "UpdatedArabic";
//
//        dbDao.InsertWord(arabic, new String[]{"UpdatedPersian"}, new String[]{"UpdatedUrdu"});
//
//        String result = dbDao.DeleteWordByArabic(arabic);
//        Assert.assertTrue(result.contains("deleted"), "Word should be deleted successfully.");
//
//        String searchResult = dbDao.SearchByArabic(arabic);
//        Assert.assertTrue(searchResult.contains("not found"), "Deleted word should no longer exist in the database.");
//    }
//
//    @Test
//    public void testAddFavoriteWord() {
//        String arabic = "FavoriteArabic";
//
//        dbDao.InsertWord(arabic, new String[]{"FavoritePersian"}, new String[]{"FavoriteUrdu"});
//
//        String result = dbDao.AddFavoriteWord(arabic);
//        Assert.assertTrue(result.contains("successfully"), "Word should be added to favorites.");
//    }
//
//    @Test
//    public void testDeleteFavoriteWord() {
//        String arabic = "FavoriteArabic";
//
//        dbDao.InsertWord(arabic, new String[]{"FavoritePersian"}, new String[]{"FavoriteUrdu"});
//        dbDao.AddFavoriteWord(arabic);
//
//        String result = dbDao.DeleteFavoriteWord(arabic);
//        Assert.assertTrue(result.contains("successfully"), "Favorite word should be deleted successfully.");
//    }
//}

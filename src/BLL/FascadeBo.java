package BLL;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import DAL.FascadeDao;
import DAL.IFascadeDao;

public class FascadeBo implements IFascadeBo {
    private IDictionaryBo dictionaryBO;
    private final IFascadeDao facadeDAO;

    public FascadeBo(String PATH) {
        this.facadeDAO = new FascadeDao(PATH);
        this.dictionaryBO = new DictionaryBo(PATH);
    }

    @Override
    public String AddWord(String arabic, String persian, String urdu) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        if (arabic.isEmpty()) {
            return "Arabic word must be provided.";
        }
        return dictionaryBO.AddWord(arabic, persian, urdu);
    }

    @Override
    public void AddToHistory(String searchedWord) {
        if (!facadeDAO.isDatabaseConnected()) {
            return;
        }
        dictionaryBO.AddToHistory(searchedWord);
    }

    @Override
    public String[][] ReadHistoryWords() {
        if (!facadeDAO.isDatabaseConnected()) {
            return new String[0][0];
        }
        return dictionaryBO.ReadHistoryWords();
    }

    @Override
    public String[][] ManageFavoriteWords(String arabic, int i) {
        if (!facadeDAO.isDatabaseConnected()) {
            return new String[][] {{"DB not connected"}};
        }
        return dictionaryBO.ManageFavoriteWords(arabic, i);
    }

    @Override
    public String UpdateWord(String oldArabic, String newArabic, String persian, String urdu) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        if (oldArabic.isEmpty()) {
            return "Arabic word must be provided.";
        }
        return dictionaryBO.UpdateWord(oldArabic, newArabic, persian, urdu);
    }

    @Override
    public String DeleteWord(String arabic) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        if (arabic.isEmpty()) {
            return "Arabic word must be provided.";
        }
        return dictionaryBO.DeleteWord(arabic);
    }

    @Override
    public String[][] ReadAllWords() {
        if (!facadeDAO.isDatabaseConnected()) {
            return new String[0][0];
        }
        return dictionaryBO.ReadAllWords();
    }

    @Override
    public String SearchArabicWord(String arabic) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        if (arabic.isEmpty()) {
            return "Arabic word must be provided.";
        }
        return dictionaryBO.SearchArabicWord(arabic);
    }

    @Override
    public String SearchPersianWord(String persian) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        if (persian.isEmpty()) {
            return "Persian word must be provided.";
        }
        return dictionaryBO.SearchPersianWord(persian);
    }

    @Override
    public String SearchUrduWord(String urdu) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        if (urdu.isEmpty()) {
            return "Urdu word must be provided.";
        }
        return dictionaryBO.SearchUrduWord(urdu);
    }

    @Override
    public String PosTagging(String arabic) {
        return dictionaryBO.PosTagging(arabic);
    }

    @Override
    public String RootWord(String arabic) {
        return dictionaryBO.RootWord(arabic);
    }

    @Override
    public Object[] InsertFromScrape(String arabic, String urdu, String persian) {
        if (!facadeDAO.isDatabaseConnected()) {
            return new Object[] {"DB not connected", null};
        }
        return dictionaryBO.InsertFromScrape(arabic, urdu, persian);
    }

    @Override
    public String ImportCsv(String csvFilePath) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        if (csvFilePath.isEmpty()) {
            return "File path must be provided.";
        }
        return dictionaryBO.ImportCsv(csvFilePath);
    }

    @Override
    public String WordLemmatization(String arabic) {
        return dictionaryBO.WordLemmatization(arabic);
    }

    @Override
    public void ReadFileAndCheckMeanings(String filePath) throws Exception {
        if (!facadeDAO.isDatabaseConnected()) {
            throw new Exception("DB not connected");
        }
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        StringBuilder content = new StringBuilder();
        for (String line : lines) {
            content.append(line).append(" ");
        }
        String[] words = content.toString().split("\\s+");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = new ArrayList<>();
        for (String word : words) {
            futures.add(executor.submit(() -> ProcessWord(word.trim())));
        }
        for (Future<String> future : futures) {
            try {
                System.out.println(future.get());
            } catch (Exception e) {
                System.err.println("Error processing word: " + e.getMessage());
            }
        }
        executor.shutdown();
    }

    private String ProcessWord(String word) {
        if (!facadeDAO.isDatabaseConnected()) {
            return "DB not connected";
        }
        String threadName = Thread.currentThread().getName();
        try {
            System.out.println(threadName + " started processing word: " + word);
            String meaning = facadeDAO.GetWordMeaning(word);
            System.out.println(threadName + " finished processing word: " + word);
            return meaning != null
                    ? "Word: " + word + ", Meaning: " + meaning
                    : "Word: " + word + " has no meaning in the database.";
        } catch (Exception e) {
            return "Error fetching meaning for word: " + word + " -> " + e.getMessage();
        }
    }

    @Override
    public String ReadFileAndGenerateMeanings(String filePath) throws Exception {
        if (!facadeDAO.isDatabaseConnected()) {
            throw new Exception("DB not connected");
        }
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        StringBuilder content = new StringBuilder();
        for (String line : lines) {
            content.append(line).append(" ");
        }
        String[] words = content.toString().split("\\s+");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = new ArrayList<>();
        StringBuilder results = new StringBuilder();
        for (String word : words) {
            futures.add(executor.submit(() -> {
                String threadInfo = "Thread " + Thread.currentThread().getId() + " STARTED processing word: " + word;
                synchronized (results) {
                    results.append(threadInfo).append("\n");
                }
                String meaning = facadeDAO.GetWordMeaning(word.trim());
                String result = meaning != null
                        ? "Word: " + word + ", Meaning: " + meaning
                        : "Word: " + word + " has no meaning in the database.";
                synchronized (results) {
                    results.append("Thread ").append(Thread.currentThread().getId()).append(" COMPLETED processing word: ").append(word).append("\n");
                }
                return result;
            }));
        }
        for (Future<String> future : futures) {
            results.append(future.get()).append("\n");
        }
        executor.shutdown();
        results.append("All threads completed execution.\n");
        return results.toString();
    }

    @Override
    public void SaveResultsToFile(String results, String outputFilePath) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write(results);
        } catch (IOException e) {
            throw new Exception("Error writing results to file: " + e.getMessage(), e);
        }
    }

    @Override
    public void readFileAndSaveMeanings(String inputFilePath, String outputFilePath) throws Exception {
        if (!facadeDAO.isDatabaseConnected()) {
            throw new Exception("DB not connected");
        }
        List<String> lines = Files.readAllLines(Paths.get(inputFilePath), StandardCharsets.UTF_8);
        StringBuilder content = new StringBuilder();
        for (String line : lines) {
            content.append(line).append(" ");
        }
        String[] words = content.toString().split("\\s+");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            for (String word : words) {
                String meaning = facadeDAO.GetWordMeaning(word.trim());
                if (meaning != null) {
                    writer.write("Word: " + word + ", Meaning: " + meaning);
                } else {
                    writer.write("Word: " + word + " has no meaning in the database.");
                }
                writer.newLine();
            }
        } catch (IOException e) {
            throw new Exception("Error writing to file: " + e.getMessage(), e);
        }
    }
}

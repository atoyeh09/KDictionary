package BLL;

import net.oujda_nlp_team.AlKhalil2Analyzer;
import net.oujda_nlp_team.entity.Result;
import com.qcri.farasa.segmenter.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import DAL.FascadeDao;
import DAL.IFascadeDao;

public class DictionaryBo implements IDictionaryBo {
	private IFascadeDao FascadeDao;
	private Farasa farasa;

	public DictionaryBo(String PATH) {
		try {
			this.FascadeDao = new FascadeDao(PATH);
			this.farasa = new Farasa();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String AddWord(String arabic, String persian, String urdu) {
		if (!FascadeDao.isDatabaseConnected()) {
			return "DB not connected";
		}
		if (arabic.isEmpty()) {
			return "Arabic word must be provided.";
		}
		String[] persianWords = persian.trim().isEmpty() ? new String[0] : persian.split("\\s*,\\s*");
		String[] urduWords = urdu.trim().isEmpty() ? new String[0] : urdu.split("\\s*,\\s*");
		return FascadeDao.InsertWord(arabic, persianWords, urduWords);
	}

	@Override
	public void AddToHistory(String searchedWord) {
		if (!FascadeDao.isDatabaseConnected()) {
			return;
		}
		FascadeDao.InsertToHistory(searchedWord);
	}

	@Override
	public String[][] ReadHistoryWords() {
		if (!FascadeDao.isDatabaseConnected()) {
			return new String[0][0];
		}
		return FascadeDao.ReadHistoryWords();
	}

	@Override
	public String[][] ManageFavoriteWords(String arabic, int i) {
		if (!FascadeDao.isDatabaseConnected()) {
			return new String[][] {{"DB not connected"}};
		}
		return new String[0][];
	}

	@Override
	public String UpdateWord(String oldArabic, String newArabic, String persian, String urdu) {
		if (!FascadeDao.isDatabaseConnected()) {
			return "DB not connected";
		}
		if (oldArabic.isEmpty()) {
			return "Old Arabic word must be provided.";
		}
		return FascadeDao.updateWord(oldArabic, newArabic, persian, urdu);
	}

	@Override
	public String DeleteWord(String arabic) {
		if (!FascadeDao.isDatabaseConnected()) {
			return "DB not connected";
		}
		if (arabic.isEmpty()) {
			return "Arabic word must be provided.";
		}
		return FascadeDao.DeleteWordByArabic(arabic);
	}

	@Override
	public String[][] ReadAllWords() {
		if (!FascadeDao.isDatabaseConnected()) {
			return new String[0][0];
		}
		return FascadeDao.ReadAllWords();
	}

	@Override
	public String SearchArabicWord(String arabic) {
		if (!FascadeDao.isDatabaseConnected()) {
			return "DB not connected";
		}
		if (arabic.isEmpty()) {
			return "Arabic word must be provided.";
		}
		return FascadeDao.SearchByArabic(arabic);
	}

	@Override
	public String SearchPersianWord(String persian) {
		if (!FascadeDao.isDatabaseConnected()) {
			return "DB not connected";
		}
		if (persian.isEmpty()) {
			return "Persian word must be provided.";
		}
		return FascadeDao.SearchByPersian(persian);
	}

	@Override
	public String SearchUrduWord(String urdu) {
		if (!FascadeDao.isDatabaseConnected()) {
			return "DB not connected";
		}
		if (urdu.isEmpty()) {
			return "Urdu word must be provided.";
		}
		return FascadeDao.SearchByUrdu(urdu);
	}

	@Override
	public String PosTagging(String arabic) {
		StringBuilder posTags = new StringBuilder();
		try {
			List<Result> str = AlKhalil2Analyzer.getInstance().processToken(arabic).getAllResults();
			if (str != null && !str.isEmpty()) {
				String[] splitWords = str.get(0).getPartOfSpeech().split("\\|");
				for (int i = 0; i < splitWords.length; i++) {
					posTags.append(splitWords[i]);
					if (i < splitWords.length - 1) {
						posTags.append(", ");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return posTags.toString();
	}

	@Override
	public String RootWord(String arabic) {
		try {
			AlKhalil2Analyzer analyzer = AlKhalil2Analyzer.getInstance();
			if (analyzer != null) {
				return analyzer.processToken(arabic).getAllRootString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Root word not found";
	}

	public String WordLemmatization(String arabic) {
		try {
			ArrayList<String> lemmatizedWords = farasa.lemmatizeLine(arabic);
			String result = lemmatizedWords.toString();
			if (result.length() > 2) {
				result = result.substring(1, result.length() - 1);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public Object[] InsertFromScrape(String arabic, String urdu, String persian) {
		if (!FascadeDao.isDatabaseConnected()) {
			return new Object[] {"DB not connected", null};
		}
		if (arabic.isEmpty() || urdu.isEmpty() || persian.isEmpty()) {
			return new Object[] {"Failure: All fields must be filled out.", null};
		}
		String[] result = FascadeDao.InsertWordScrape(arabic, persian, urdu);
		if (result[0].equals("Failure")) {
			return new Object[] {result[1], null};
		}
		return new Object[] {"Word added: " + result[1], new Object[] {arabic, urdu, persian}};
	}

	@Override
	public String ImportCsv(String csvFilePath) {
		if (!FascadeDao.isDatabaseConnected()) {
			return "DB not connected";
		}
		if (csvFilePath.isEmpty()) {
			return "File path must be provided.";
		}
		return FascadeDao.insertWordsFromCSV(csvFilePath);
	}

	@Override
	public void ReadFileAndCheckMeanings(String filePath) throws Exception {
		if (!FascadeDao.isDatabaseConnected()) {
			throw new Exception("DB not connected");
		}
		List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
		StringBuilder content = new StringBuilder();
		for (String line : lines) {
			content.append(line).append(" ");
		}
		String[] words = content.toString().split("\\s+");
		for (String word : words) {
			String meaning = FascadeDao.GetWordMeaning(word.trim());
			if (meaning != null) {
				System.out.println("Word: " + word + ", Meaning: " + meaning);
			} else {
				System.out.println("Word: " + word + " has no meaning in the database.");
			}
		}
	}
}

package BLL;

public interface IFascadeBo extends IDictionaryBo {
	void ReadFileAndCheckMeanings(String filePath) throws Exception;

	void readFileAndSaveMeanings(String inputFilePath, String outputFilePath) throws Exception;
	String ReadFileAndGenerateMeanings(String inputFilePath) throws Exception;
	void SaveResultsToFile(String results, String outputFilePath) throws Exception;

}

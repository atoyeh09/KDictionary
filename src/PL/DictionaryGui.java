package PL;

import DAL.DbDao;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.qcri.farasa.segmenter.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import BLL.IFascadeBo;

public class DictionaryGui {
	JFrame frame;
	IFascadeBo fascadeBO;
	private JTable dictionaryTable;
	private final Farasa farasaSegmenter;
	DbDao dbDAO;

	public DictionaryGui(IFascadeBo fascadeBO, String PATH) {
		dbDAO = new DbDao(PATH);
		frame = new JFrame("Dictionary");
		this.fascadeBO = fascadeBO;

		try {
			this.farasaSegmenter = new Farasa();
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize Farasa segmenter.", e);
		}
		frame.setLayout(new BorderLayout());

		frame.add(SetLogoPanel(), BorderLayout.NORTH);
		frame.add(SetDictionaryTablePanel(), BorderLayout.CENTER);
		frame.add(SetButtonPanel(), BorderLayout.SOUTH);

		LoadWordsToTable();

		frame.setTitle("Arabic Dictionary");
		frame.setSize(500, 600);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	JPanel SetLogoPanel() {
		ImageIcon Icon = new ImageIcon("resources/Black Illustrative Education Logo.png");
		Image logoImage = Icon.getImage().getScaledInstance(150, 100, Image.SCALE_SMOOTH);
		JLabel logoLabel = new JLabel(new ImageIcon(logoImage));
		logoLabel.setBorder(BorderFactory.createLineBorder(new Color(139, 69, 19), 2));
		logoLabel.setPreferredSize(new Dimension(130, 100));

		JPanel logoPanel = new JPanel();
		logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		logoPanel.add(logoLabel);

		return logoPanel;
	}

	JPanel SetDictionaryTablePanel() {
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Arabic");

		dictionaryTable = new JTable(model);
		dictionaryTable.setFont(new Font("Arial", Font.BOLD, 19));
		dictionaryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane tableScrollPane = new JScrollPane(dictionaryTable);

		dictionaryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int selectedRow = dictionaryTable.getSelectedRow();
					if (selectedRow != -1) {
						String arabicWord = (String) dictionaryTable.getValueAt(selectedRow, 0);
						ShowWordDetails(arabicWord);
					}
				}
			}
		});

		JPanel tablePanel = new JPanel();
		tablePanel.setBackground(new Color(139, 69, 19));
		tablePanel.setLayout(new BorderLayout());
		tablePanel.add(tableScrollPane, BorderLayout.CENTER);

		return tablePanel;
	}

	private void ShowWordDetails(String arabicWord) {
		String message = "<html><b>Arabic:</b> " + arabicWord + "<br>" +
				"<b>POS:</b> " + fascadeBO.PosTagging(arabicWord) + "<br>" +
				"<b>Root:</b> " + fascadeBO.RootWord(arabicWord) + "<br>" +
				"<b>Lemmatization:</b> "+ fascadeBO.WordLemmatization(arabicWord)  + "<br></html>";

		int option = JOptionPane.showConfirmDialog(frame, message + "Would you like to add this word to favorites?", "Word Details", JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.YES_OPTION) {
			dbDAO.AddFavoriteWord(arabicWord);
		}
	}

	private void ManageFavorites() {
		String[][] favorites = dbDAO.AllFavoriteWords();

		if (favorites == null || favorites.length == 0) {
			JOptionPane.showMessageDialog(frame, "No favorites found.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		String[] columnNames = {"Favorite Words"};
		DefaultTableModel model = new DefaultTableModel(favorites, columnNames);
		JTable favoritesTable = new JTable(model);

		favoritesTable.setPreferredScrollableViewportSize(new Dimension(300, 150));
		JScrollPane scrollPane = new JScrollPane(favoritesTable);

		int option = JOptionPane.showConfirmDialog(frame, scrollPane, "Favorites", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {
			int selectedRow = favoritesTable.getSelectedRow();
			if (selectedRow != -1) {
				String wordToDelete = (String) favoritesTable.getValueAt(selectedRow, 0);
				dbDAO.DeleteFavoriteWord(wordToDelete);
			}
		}
	}



	JButton CreateButton(String text, String actionCommand, ActionListener listener) {
		JButton button = new JButton(text);
		button.setBackground(new Color(139, 69, 19));
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		button.setFont(new Font("Arial", Font.BOLD, 14));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setActionCommand(actionCommand);

		button.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				button.setBackground(new Color(112, 57, 17));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				button.setBackground(new Color(160, 82, 45));
			}
		});

		button.addActionListener(listener);

		return button;
	}

	JPanel SetButtonPanel() {
		JPanel buttonPanel = new JPanel();

		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		ActionListener buttonListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String actionCommand = e.getActionCommand();
				switch (actionCommand) {
					case "SEARCH":
						OpenSearchMenu();
						break;
					case "ADD_WORD":
						AddWord();
						break;
					case "DELETE_WORD":
						DeleteWord();
						break;
					case "UPDATE_WORD":
						UpdateWord();
						break;
					case "IMPORT_CSV":
						ImportCsv();
						break;
					case "FAVORITES":
						ManageFavorites();
						break;
					case "WebScraping":
						ScrapeFromWeb();
						break;
					case "CUSTOM_DICTIONARY":
						OpenCustomDictionary();
						break;
					case "History":
						ViewHistory();
						break;
				}
				LoadWordsToTable();
			}
		};

		// Add buttons with a consistent font size
		buttonPanel.add(CreateButton("Search", "SEARCH", buttonListener));
		buttonPanel.add(CreateButton("Add Word", "ADD_WORD", buttonListener));
		buttonPanel.add(CreateButton("Delete Word", "DELETE_WORD", buttonListener));
		buttonPanel.add(CreateButton("Update Word", "UPDATE_WORD", buttonListener));
		buttonPanel.add(CreateButton("Import CSV", "IMPORT_CSV", buttonListener));
		buttonPanel.add(CreateButton("Favorites", "FAVORITES", buttonListener));
		buttonPanel.add(CreateButton("Web Scrape", "WebScraping", buttonListener));
		buttonPanel.add(CreateButton("Custom Dictionary", "CUSTOM_DICTIONARY", buttonListener));
		buttonPanel.add(CreateButton("View History", "History", buttonListener));


		buttonPanel.setPreferredSize(new Dimension(500, 100));
		return buttonPanel;
	}

	private void OpenCustomDictionary() {
		JDialog customDictionaryDialog = new JDialog(frame, "Custom Dictionary", true);
		customDictionaryDialog.setLayout(new FlowLayout());
		customDictionaryDialog.setSize(400, 200);

		JLabel instructionLabel = new JLabel("<html>"
				+ "<h3>Process a File to Get Word Meanings</h3>"
				+ "<p>Select a text file to process and get meanings of words.</p>"
				+ "<p>You can save the results anywhere on your device.</p>"
				+ "</html>");
		customDictionaryDialog.add(instructionLabel);

		JTextField filePathField = new JTextField(25);
		filePathField.setText("");
		customDictionaryDialog.add(new JLabel("Select Input File:"));
		customDictionaryDialog.add(filePathField);

		JButton openFileButton = new JButton("Browse...");
		openFileButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			int returnValue = fileChooser.showOpenDialog(frame);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fileChooser.getSelectedFile();
				filePathField.setText(selectedFile.getAbsolutePath());
			}
		});
		customDictionaryDialog.add(openFileButton);

		JButton processButton = new JButton("Process File");
		processButton.addActionListener(e -> {
			String inputFilePath = filePathField.getText();

			if (inputFilePath.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Please select a file to process.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			try {
				JDialog progressDialog = new JDialog(frame, "Processing File...", true);
				progressDialog.setLayout(new FlowLayout());
				progressDialog.setSize(300, 100);
				progressDialog.add(new JLabel("Verifying Thread Execution..."));
				progressDialog.setLocationRelativeTo(frame);

				SwingWorker<String, String> worker = new SwingWorker<String, String>() {
					@Override
					protected String doInBackground() throws Exception {
						StringBuilder results = new StringBuilder();

						String meanings = fascadeBO.ReadFileAndGenerateMeanings(inputFilePath);
						results.append(meanings); // Store results (meanings)

						JFileChooser saveFileChooser = new JFileChooser();
						saveFileChooser.setDialogTitle("Save Results As");
						saveFileChooser.setSelectedFile(new File("Processed_Meanings.txt"));

						int saveResult = saveFileChooser.showSaveDialog(frame);
						if (saveResult == JFileChooser.APPROVE_OPTION) {
							File saveFile = saveFileChooser.getSelectedFile();
							String saveFilePath = saveFile.getAbsolutePath();
							fascadeBO.SaveResultsToFile(meanings, saveFilePath);
						}

						return results.toString();
					}

					@Override
					protected void process(List<String> chunks) {
						if (!chunks.isEmpty()) {
							String message = chunks.get(chunks.size() - 1);
							progressDialog.setTitle("Verifying Thread Execution");
							((JLabel) progressDialog.getContentPane().getComponent(0)).setText(message);
						}
					}

					@Override
					protected void done() {
						try {
							progressDialog.dispose();
							String results = get();
							JDialog resultsDialog = new JDialog(frame, "Processed Results", true);
							resultsDialog.setLayout(new BorderLayout());
							resultsDialog.setSize(400, 300);

							JTextArea resultsArea = new JTextArea(results);
							resultsArea.setEditable(false);
							JScrollPane scrollPane = new JScrollPane(resultsArea);
							resultsDialog.add(scrollPane, BorderLayout.CENTER);

							JButton closeButton = new JButton("Close");
							closeButton.addActionListener(event -> resultsDialog.dispose());
							resultsDialog.add(closeButton, BorderLayout.SOUTH);

							resultsDialog.setVisible(true);
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(frame, "Error displaying results: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				};

				worker.execute();
				progressDialog.setVisible(true); // Show progress dialog

			} catch (Exception ex) {
				JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		});

		customDictionaryDialog.add(processButton);
		customDictionaryDialog.setVisible(true);
	}









	private void LoadWordsToTable() {
		String[][] words = fascadeBO.ReadAllWords();
		DefaultTableModel model = (DefaultTableModel) dictionaryTable.getModel();
		model.setRowCount(0);
		for (String[] word : words) {
			model.addRow(word);
		}
	}

	public void ViewHistory()
	{
		String[][] historyData = fascadeBO.ReadHistoryWords();


		String[] columnNames = {"History"};
		DefaultTableModel model = new DefaultTableModel(historyData, columnNames);
		JTable historyTable = new JTable(model);

		historyTable.setPreferredScrollableViewportSize(new Dimension(300, 150));
		JScrollPane scrollPane = new JScrollPane(historyTable);


		JOptionPane.showMessageDialog(frame, scrollPane, "History", JOptionPane.INFORMATION_MESSAGE);
	}


	public void AddToHistory(String searchedWord)
	{
		String wordToAdd = searchedWord;
		fascadeBO.AddToHistory(wordToAdd);
	}

	private void OpenSearchMenu() {
		JDialog searchDialog = new JDialog(frame, "Search Options", true);
		searchDialog.setLayout(new FlowLayout());
		searchDialog.setSize(300, 200);

		String[] options = {
				"Search by Arabic",
				"Search by Persian",
				"Search by Urdu",
				"Lemmatization",
		};

		JComboBox<String> optionMenu = new JComboBox<>(options);
		JTextField searchField = new JTextField(10);

		JButton searchSubmitButton = new JButton("Search");

		searchSubmitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedOption = (String) optionMenu.getSelectedItem();
				String searchText = searchField.getText().trim();
				String resultMessage = "";

				if (searchText.isEmpty()) {
					JOptionPane.showMessageDialog(searchDialog, "Please enter a word to search.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				AddToHistory(searchText);
				switch (selectedOption) {
					case "Search by Arabic":
						resultMessage = fascadeBO.SearchArabicWord(searchText);
						break;
					case "Search by Persian":
						resultMessage = fascadeBO.SearchPersianWord(searchText);
						break;
					case "Search by Urdu":
						resultMessage = fascadeBO.SearchUrduWord(searchText);
						break;
					case "Lemmatization":
						resultMessage = "Lemmatized: " + fascadeBO.WordLemmatization(searchText);
						break;
				}

				if (resultMessage != null && resultMessage.contains("Arabic word not found.")) {
					int choice = JOptionPane.showConfirmDialog(
							searchDialog,
							"Word not found. Would you like to view its segmentation?",
							"Word Not Found",
							JOptionPane.YES_NO_OPTION
					);

					if (choice == JOptionPane.YES_OPTION) {
						List<String> segmentedWords = SegmentArabicText(searchText);

						if (segmentedWords != null && !segmentedWords.isEmpty()) {
							String[] columnNames = {"Segmented Words"};
							String[][] data = new String[segmentedWords.size()][1];
							for (int i = 0; i < segmentedWords.size(); i++) {
								data[i][0] = segmentedWords.get(i);
							}

							JTable segmentationTable = new JTable(data, columnNames);
							segmentationTable.setRowHeight(20);
							segmentationTable.setFont(new Font("Arial", Font.PLAIN, 12));
							segmentationTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

							JScrollPane scrollPane = new JScrollPane(segmentationTable);
							scrollPane.setPreferredSize(new Dimension(200, 150));

							JOptionPane.showMessageDialog(searchDialog, scrollPane, "Segmentation Result", JOptionPane.INFORMATION_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(searchDialog, "No segmentation results found.", "Segmentation Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				} else if (resultMessage != null && resultMessage.contains("Persian word not found.")) {
					int choice = JOptionPane.showConfirmDialog(
							searchDialog,
							"Word not found. Would you like to view its segmentation?",
							"Word Not Found",
							JOptionPane.YES_NO_OPTION
					);

					if (choice == JOptionPane.YES_OPTION) {
						List<String> segmentedWords = SegmentPersianText(searchText);

						if (segmentedWords != null && !segmentedWords.isEmpty()) {
							String[] columnNames = {"Segmented Words"};
							String[][] data = new String[segmentedWords.size()][1];
							for (int i = 0; i < segmentedWords.size(); i++) {
								data[i][0] = segmentedWords.get(i);
							}

							JTable segmentationTable = new JTable(data, columnNames);
							segmentationTable.setRowHeight(20);
							segmentationTable.setFont(new Font("Arial", Font.PLAIN, 12));
							segmentationTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

							JScrollPane scrollPane = new JScrollPane(segmentationTable);
							scrollPane.setPreferredSize(new Dimension(200, 150));

							JOptionPane.showMessageDialog(searchDialog, scrollPane, "Segmentation Result", JOptionPane.INFORMATION_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(searchDialog, "No segmentation results found.", "Segmentation Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				} else if (resultMessage != null && resultMessage.contains("Urdu word not found.")) {
					int choice = JOptionPane.showConfirmDialog(
							searchDialog,
							"Word not found. Would you like to view its segmentation?",
							"Word Not Found",
							JOptionPane.YES_NO_OPTION
					);

					if (choice == JOptionPane.YES_OPTION) {
						List<String> segmentedWords = SegmentUrduText(searchText);

						if (segmentedWords != null && !segmentedWords.isEmpty()) {
							String[] columnNames = {"Segmented Words"};
							String[][] data = new String[segmentedWords.size()][1];
							for (int i = 0; i < segmentedWords.size(); i++) {
								data[i][0] = segmentedWords.get(i);
							}

							JTable segmentationTable = new JTable(data, columnNames);
							segmentationTable.setRowHeight(20);
							segmentationTable.setFont(new Font("Arial", Font.PLAIN, 12));
							segmentationTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

							JScrollPane scrollPane = new JScrollPane(segmentationTable);
							scrollPane.setPreferredSize(new Dimension(200, 150));

							JOptionPane.showMessageDialog(searchDialog, scrollPane, "Segmentation Result", JOptionPane.INFORMATION_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(searchDialog, "No segmentation results found.", "Segmentation Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				} else {
					JOptionPane.showMessageDialog(searchDialog, resultMessage, "Search Result", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});

		searchDialog.add(optionMenu);
		searchDialog.add(searchField);
		searchDialog.add(searchSubmitButton);
		searchDialog.setVisible(true);
	}

	// Arabic Segmentation
	public List<String> SegmentArabicText(String text) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("Input text cannot be null or empty.");
		}

		try {

			String cleanedText = text.replaceAll("[^\\p{IsArabic}\\s]", "").trim();
			List<String> segmentedWords = farasaSegmenter.segmentLine(cleanedText);

			return segmentedWords.stream()
					.filter(word -> word != null && !word.trim().isEmpty())
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw new RuntimeException("Error during Farasa segmentation", e);
		}
	}

	public List<String> SegmentPersianText(String text) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("Input text cannot be null or empty.");
		}

		try {
			String cleanedText = text.replaceAll("[^\\u0600-\\u06FF\\s]", "").trim();
			Farasa persianSegmenter = new Farasa();
			List<String> segmentedWords = persianSegmenter.segmentLine(cleanedText);


			return segmentedWords.stream()
					.filter(word -> word != null && !word.trim().isEmpty())
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw new RuntimeException("Error during Persian segmentation", e);
		}
	}


	public List<String> SegmentUrduText(String text) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("Input text cannot be null or empty.");
		}

		try {
			String cleanedText = text.replaceAll("[^\\u0600-\\u06FF\\u0750-\\u077F\\s]", "").trim();

			Farasa urduSegmenter = new Farasa();

			List<String> segmentedWords = urduSegmenter.segmentLine(cleanedText);
			return segmentedWords.stream()
					.filter(word -> word != null && !word.trim().isEmpty())
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw new RuntimeException("Error during Urdu segmentation", e);
		}
	}



	private void ImportCsv() {
		JFileChooser fileChooser = new JFileChooser();
		int returnValue = fileChooser.showOpenDialog(frame);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			String filePath = selectedFile.getAbsolutePath();
			String result = fascadeBO.ImportCsv(filePath);
			JOptionPane.showMessageDialog(frame, result);
		}
	}

	private void AddWord() {
		String arabic = JOptionPane.showInputDialog(frame, "Enter Arabic word:");
		if (arabic.trim().isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Arabic word cannot be empty.");
			return;
		}
		String persian = JOptionPane.showInputDialog(frame, "Enter Persian translation (Leave empty if not available):");
		String urdu = JOptionPane.showInputDialog(frame, "Enter Urdu translation (Leave empty if not available):");

		JOptionPane.showMessageDialog(frame, fascadeBO.AddWord(arabic, persian, urdu));
	}

	private void DeleteWord() {
		String arabic = JOptionPane.showInputDialog(frame, "Enter Arabic word to delete:");
		if (arabic.trim().isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Arabic word cannot be empty.");
			return;
		}
		JOptionPane.showMessageDialog(frame, fascadeBO.DeleteWord(arabic));
	}

	private void UpdateWord() {
		String oldArabic = JOptionPane.showInputDialog(frame, "Enter Arabic word:");
		if (oldArabic.trim().isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Arabic word and meanings not found.");
			return;
		}
		String newArabic = JOptionPane.showInputDialog(frame, "Enter new Arabic translation (Leave if no change):");
		String persian = JOptionPane.showInputDialog(frame, "Enter Persian translation (Leave empty if not available):");
		String urdu = JOptionPane.showInputDialog(frame, "Enter Urdu translation (Leave empty if not available):");

		JOptionPane.showMessageDialog(frame, fascadeBO.UpdateWord(oldArabic, newArabic, persian, urdu));
	}


	private String CleanMeaning(String meaning) {

		String[] parts = meaning.split("[،\\(\\)]");
		if (parts.length > 0) {
			meaning = parts[0].trim();
		}


		meaning = meaning.replaceAll("\\(.*?\\)", "").trim();
		meaning = meaning.replaceAll("\\[.*?\\]", "").trim();

		return meaning;
	}


	private void ScrapeFromWeb() {
		try {

			String word = JOptionPane.showInputDialog(frame, "Enter an Arabic word:");

			if (word == null || word.trim().isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Please enter a valid Arabic word.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
				return;
			}


			String urduUrl = "https://www.almaany.com/ur/dict/ar-ur/" + word + "/";
			String persianUrl = "https://www.almaany.com/fa/dict/ar-fa/" + word + "/";


			String urduMeaning = null;
			String persianMeaning = null;
			String arabicScrapedWord = null;


			try {
				Document urduDoc = Jsoup.connect(urduUrl)
						.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:40.0) Gecko/20100101 Firefox/128.0")
						.get();
				Element urduTbody = urduDoc.select("#meaning > div.panel-body > table:nth-child(1) > tbody").first();
				if (urduTbody != null) {
					Elements urduRows = urduTbody.select("tr");
					for (Element row : urduRows) {
						arabicScrapedWord = row.select("td").get(0).text();
						urduMeaning = row.select("td").get(1).text();
						break;
					}
				}
			} catch (IOException e) {
				urduMeaning = "Error fetching Urdu meaning";
				arabicScrapedWord = "Error fetching Arabic word";
			}


			try {
				Document persianDoc = Jsoup.connect(persianUrl)
						.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:40.0) Gecko/20100101 Firefox/40.0")
						.get();
				Element persianTbody = persianDoc.select("#meaning > div.panel-body > table:nth-child(1) > tbody").first();
				if (persianTbody != null) {
					Elements persianRows = persianTbody.select("tr");
					for (Element row : persianRows) {
						persianMeaning = row.select("td").get(1).text();
						break;
					}
				}
			} catch (IOException e) {
				persianMeaning = "Error fetching Persian meaning";
			}


			urduMeaning = CleanMeaning(urduMeaning);
			persianMeaning = CleanMeaning(persianMeaning);
			arabicScrapedWord = CleanMeaning(arabicScrapedWord);


			if (urduMeaning != null && persianMeaning != null && arabicScrapedWord != null) {
				Object[][] data = {{word, arabicScrapedWord, urduMeaning, persianMeaning}};
				String[] columns = {"Arabic Word", "Arabic Scraped Word", "Urdu Meaning", "Persian Meaning"};


				DefaultTableModel tableModel = new DefaultTableModel(data, columns);
				JTable resultTable = new JTable(tableModel);
				resultTable.setRowHeight(25);
				resultTable.setPreferredScrollableViewportSize(new Dimension(650, 100));
				resultTable.setFillsViewportHeight(true);


				JScrollPane scrollPane = new JScrollPane(resultTable);


				JCheckBox insertCheckBox = new JCheckBox("Insert word into dictionary");


				JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				panel.add(scrollPane);
				panel.add(Box.createRigidArea(new Dimension(0, 10)));
				panel.add(insertCheckBox);

				int userChoice = JOptionPane.showConfirmDialog(frame, panel, "Scraped Meanings", JOptionPane.YES_NO_OPTION);

				if (userChoice == JOptionPane.YES_OPTION && insertCheckBox.isSelected()) {

					Object[] response = fascadeBO.InsertFromScrape(word,  urduMeaning, persianMeaning);

					String message = (String) response[0];
					Object[] tableData = (Object[]) response[1];

					if (tableData != null && tableData.length > 0) {
						String[][] insertData = {{(String) tableData[0], (String) tableData[1], (String) tableData[2]}};
						String[] columnNames = {"Arabic", "Urdu", "Persian"};

						JTable dictionaryTable = new JTable(insertData, columnNames);
						JScrollPane tableScrollPane = new JScrollPane(dictionaryTable);
						tableScrollPane.setPreferredSize(new Dimension(300, 70));

						JOptionPane.showMessageDialog(frame, tableScrollPane, "Word Added Successfully", JOptionPane.INFORMATION_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(frame, "Insertion failed.", "Error", JOptionPane.ERROR_MESSAGE);
					}

					JOptionPane.showMessageDialog(frame, message, "Add Word", JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(frame, "Word not inserted.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
				}

			} else {
				JOptionPane.showMessageDialog(frame, "No meanings found for the word: " + word, "Scraping Error", JOptionPane.ERROR_MESSAGE);
			}

		} catch (Exception ex) {

			JOptionPane.showMessageDialog(frame, "Error during scraping: " + ex.getMessage(), "Scraping Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}


import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuiMain extends Application{

   private static Button search, porterbutton, switchindex;
   private static Button vocab, changedir, switchbiword, switchmain, documentbutton;
   private Stage window;
   private Scene mainscene;
   private static StringBuffer outputcontent = new StringBuffer();
   private static TextField searchbox;
   private static TextArea output;

   //set this to the current path of your default corpus.
   private static Path currentWorkingPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

   //list of modes available
   private final String[] modelist = {"Boolean", "Ranked"};
   private final String[] menulist = {"Read", "Write"};
   private final String[] indexmodelist = {"Build a Disk Index", "Load Existing Disk Index"};
   private static boolean isRanked = false;
   private static String menuoption = "";

   // the inverted index
   static DiskInvertedIndex diskindex;
   // the list of file names that were processed
   static List<String> fileNames = new ArrayList<String>();

   //GUI STARTS HERE
   @Override
   public void start(Stage primaryStage) throws Exception{

      // Dialog Box to choose Querying mode 1)Boolean Retrieval 2) Ranked retrieval
      Dialog modes = new ChoiceDialog<>(modelist[0], modelist);
      modes.setTitle("Vsion Search Mode Selector");
      modes.setHeaderText(null);
      modes.setResizable(true);
      modes.getDialogPane().setPrefSize(350, 120);
      modes.setContentText("Select a Mode: ");

      // Dialog box to choose indexing mode
      Dialog menu = new ChoiceDialog<>(menulist[0], menulist);
      menu.setTitle("Vsion Search Main Menu");
      menu.setHeaderText(null);
      menu.setResizable(true);
      menu.getDialogPane().setPrefSize(350, 120);
      menu.setContentText("Indexing Mode: ");

      chooseQueryMode(modes);
      chooseMenuOption(menu);

      window = primaryStage;
      window.setTitle("Search Engine - Milestone 1");

      //Main Scene
      BorderPane mainlayout = new BorderPane();
      mainlayout.getStyleClass().add("background");

      //top of borderpane
      Label label = new Label("Vsion Search");
      label.setMinSize(1010,50);
      label.setId("topbar");
      mainlayout.setTop(label);

      //Middle Area will be a GridPane with 2 columns: 1)Output 2)Document Preview
      GridPane middle = new GridPane();
      middle.setMinSize(1000,600);
      ColumnConstraints col1 = new ColumnConstraints();
      col1.setPercentWidth(50);
      ColumnConstraints col2 = new ColumnConstraints();
      col2.setPercentWidth(50);
      middle.getColumnConstraints().addAll(col1,col2);

      output = new TextArea("Search Vsion : \n START USER QUERY BELOW");
      output.setMinSize(500,610);
      output.setEditable(false);
      output.getStyleClass().add("output");

      TextArea preview = new TextArea("Document Preview:\n\n" +
              "Click on Document Preview button below");
      preview.setEditable(false);
      preview.setWrapText(true);
      preview.getStyleClass().add("preview");
      preview.setMinSize(500,610);

      middle.add(output,0,0);
      middle.add(preview,1,0);
      mainlayout.setCenter(middle);

      //Bottom Search Bar
      HBox searchbar = new HBox();
      searchbar.setStyle("-fx-padding:10px;");
      searchbox = new TextField();
      searchbox.setMinWidth(100);
      search = new Button();
      search.setText("Search");
      search.getStyleClass().add("buttons");
      //when user clicks on Search button it queries based on query mode
      search.setOnAction(e -> {
         userQuery();
      });
      // Queries when enter is pressed on the search bar
      searchbox.setOnKeyPressed(new EventHandler<KeyEvent>()
      {
         @Override
         public void handle(KeyEvent ke)
         {
            if (ke.getCode().equals(KeyCode.ENTER))
            {
               search.fire();
            }
         }
      });

      //Button to porterstem a string
      porterbutton = new Button("Porter Stem");
      porterbutton.getStyleClass().add("buttons");
      porterbutton.setOnAction(e -> {
         String stemmed = PorterStemmer.processToken(searchbox.getText());
         outputcontent.append("\n\nPorter Stemming...\n" +
                 searchbox.getText()+ " -> "+stemmed+"\n");
         output.setText(outputcontent.toString());
      });
      //Button to preview a document. Enter document name in searchbar.
      documentbutton = new Button("Document Preview");
      documentbutton.getStyleClass().add("buttons");
      documentbutton.setOnAction(e -> {
         String filepath = currentWorkingPath + "\\"+searchbox.getText();
         System.out.println(filepath);
         preview.setText(BodyOutput.getBodyString(filepath));
      });
      //Button to select a new corpus directory
      changedir = new Button("Change Corpus");
      changedir.getStyleClass().add("buttons");
      changedir.setOnAction(e -> {
         currentWorkingPath = chooseFolder(currentWorkingPath.toFile());
      });



      searchbar.getChildren().addAll(searchbox, search, porterbutton, documentbutton, changedir);
      searchbar.setId("bottombar");
      searchbar.setMinHeight(50);
      mainlayout.setBottom(searchbar);


      mainscene = new Scene(mainlayout, 1000,700);
      mainscene.getStylesheets().add("style.css");

      //main
      window.setScene(mainscene);
      window.setResizable(false);
      window.show();
   }

   //Queries The User Input in the searchbar.
   private static void userQuery(){
      String userinput = searchbox.getText();
      outputcontent = new StringBuffer("Query: "+userinput+"\n\n");
      DocumentProcessing processor = new DocumentProcessing();
      RankedQueryParser rankedparser = new RankedQueryParser(diskindex, fileNames.size());
      boolean biwordfail = true; // checks if biword finds the query

      List<Integer> results = new ArrayList<>();
      // Ranked results is a List of  List<DocID, score>
      List<ScoredDocument> rankedresults = new ArrayList<>();

      // we have exactly 2 words, use biword index
      if(userinput.split(" ").length == 2 && !isRanked){
         String[] inputsize = userinput.split(" ");
         String SearchBWord = processor.normalizeToken(inputsize[0])+" "+processor.normalizeToken(inputsize[1]);
         outputcontent.append("\nSearching Biword index...\n");
         outputcontent.append(SearchBWord+ "\n\t");
         results = diskindex.GetBwordPostings(SearchBWord);
         if (results!=null && results.size() > 0) {
            biwordfail = false;
            for (Integer i : results) {
               outputcontent.append("\n"+fileNames.get(i));
            }
         }
      }

      // otherwise, use positional inverted index
      if(biwordfail) {
         // choose querying mode accordingly
         if (isRanked){
            rankedresults = rankedparser.rankDocuments(userinput);
         }
         //regular boolean query
         else
            results = QueryParser.parseQuery(userinput, diskindex);

         if (results!=null && results.size() > 0) {
            outputcontent.append("\nSearching Positional Inverted Index...\n" + userinput + " :");
            for (Integer i : results) {
               outputcontent.append("\n"+fileNames.get(i));
            }
         }
         else if(rankedresults.size() > 0){
            outputcontent.append("\nSearching Positional Inverted Index (ranked)...\n" + userinput + " :");
            for (ScoredDocument i : rankedresults) {
               outputcontent.append("\n"+fileNames.get(i.getId()) +"\t\tScore: "+ i.getScore());
            }
         }
      }
      if(isRanked)
         outputcontent.append("\nResults Returned: "+ rankedresults.size());
      else
         outputcontent.append("\nResults Returned: "+ results.size());

      //if no results are found
      if(results == null || (results.isEmpty()&& rankedresults.isEmpty()))
         outputcontent.append("\n\tNo Results Found.");

      output.setText(outputcontent.toString());
   }


   public static void main(String[] args) throws IOException{
      launch(args);
   }

   // Methods for UI Buttons and Functionality

   // Dialogbox that makes user choose ranked or boolean mode
   private static void chooseQueryMode(Dialog modes){
      Optional<String> result = modes.showAndWait();
      String selected = "cancelled";

      if(result.isPresent()){
         selected = result.get();   // retrieves user selection from dialog
      }

      if(selected.equals("Ranked")){
         System.out.println("You Selected Ranked Retrieval...");
         isRanked = true;
      }
      else if(selected.equals("Boolean")){
         System.out.println("You Selected Boolean Retrieval...");
         isRanked = false;
      }
   }

   // Dialogbox that chooses whether to read or write an index
   private static void chooseMenuOption(Dialog modes) throws Exception{
      Optional<String> result = modes.showAndWait();
      String selected = "cancelled";

      if(result.isPresent()){
         selected = result.get();   // retrieves user selection from dialog
      }

      menuoption = selected;
      if(menuoption.equals("Read")){
         currentWorkingPath = chooseFolder(currentWorkingPath.toFile());
         diskindex = new DiskInvertedIndex(currentWorkingPath.toString());
         fileNames = diskindex.getFileNames();
      }
      else if(menuoption.equals("Write")){
         currentWorkingPath = chooseFolder(currentWorkingPath.toFile());
         IndexWriter writer = new IndexWriter(currentWorkingPath.toString());
         writer.buildIndex();

         if(promptToRead()) {
            diskindex = new DiskInvertedIndex(currentWorkingPath.toString());
            fileNames = diskindex.getFileNames();
         }
      }
   }

   private static boolean promptToRead(){
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      String s = "Read into the same directory?";
      alert.setContentText(s);
      Optional<ButtonType> result = alert.showAndWait();

      if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
         return true;
      }
      return false;
   }

   // Shows up dialog box to choose a file directory
   private static Path chooseFolder(File file) {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle("Choose a Directory");
      if (file != null) {
         directoryChooser.setInitialDirectory(file);
      }
      return directoryChooser.showDialog(null).toPath();
   }


}

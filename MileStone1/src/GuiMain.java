import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class GuiMain extends Application{

   private static Button search, porterbutton, switchindex;
   private static Button vocab, changedir, switchbiword, switchmain, documentbutton;
   private Stage window;
   private Scene mainscene, indexscene;
   private static String outputcontent = "";
   private static TextField searchbox;
   private static TextArea output;
   private static Path currentWorkingPath = Paths.get("C:\\Users\\Mark\\Documents\\CSULB\\CECS_429 - Search Engine\\Corpus15000").toAbsolutePath();
   // the inverted index
   final static PositionalInvertedIndex index = new PositionalInvertedIndex();
   // the list of file names that were processed
   final static List<String> fileNames = new ArrayList<String>();
   //the BiwordIndex
   final static BiwordIndex bindex = new BiwordIndex();

   //GUI STARTS HERE
   @Override
   public void start(Stage primaryStage) throws Exception{
      // Choose Corpus Directory
      currentWorkingPath = chooseFolder(currentWorkingPath.toFile());
      // Index the Corpus
      index();

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
      search.setOnAction(e -> {
         userQuery();
      });
      //when user clicks enter to query...
      searchbox.setOnKeyPressed(new EventHandler<KeyEvent>()
      {
         @Override
         public void handle(KeyEvent ke)
         {
            if (ke.getCode().equals(KeyCode.ENTER))
            {
               userQuery();
            }
         }
      });
      //Button to porterstem a string
      porterbutton = new Button("Porter Stem");
      porterbutton.getStyleClass().add("buttons");
      porterbutton.setOnAction(e -> {
         // check for stem and quit command
         String stemmed = PorterStemmer.processToken(searchbox.getText());
         outputcontent += "\n\nPorter Stemming...\n" +
                 searchbox.getText()+ " -> "+stemmed+"\n";
         output.setText(outputcontent);
      });
      //Button to preview a document
      documentbutton = new Button("Document Preview");
      documentbutton.getStyleClass().add("buttons");
      documentbutton.setOnAction(e -> {
         String filepath = currentWorkingPath + "\\"+searchbox.getText();
         System.out.println(filepath);
         preview.setText(BodyOutPut.getBodyString(filepath));
      });
      //Button to index directory
      changedir = new Button("Directory");
      changedir.getStyleClass().add("buttons");
      changedir.setOnAction(e -> {
         currentWorkingPath = chooseFolder(currentWorkingPath.toFile());
         try{
            index();
         }catch(IOException ex){
            System.out.println(ex.toString());
         }
      });
      //Button to display vocabulary of index
      vocab = new Button("Vocab");
      vocab.getStyleClass().add("buttons");
      vocab.setOnAction(e -> {
         outputcontent = getVocab();
         output.setText(outputcontent);
      });
      //Button to display positional index
      switchindex = new Button("Positional Index");
      switchindex.getStyleClass().add("buttons");
      switchindex.setOnAction(e -> {
         output.setText(printResults(index, fileNames));
//         window.setScene(indexscene);
      });
      //Button to display biword index
      switchbiword = new Button("Biword Index");
      switchbiword.getStyleClass().add("buttons");
      switchbiword.setOnAction(e -> {
         output.setText(printBiwordResults(bindex, fileNames));
      });

      searchbar.getChildren().addAll(searchbox, search, porterbutton, documentbutton,
              vocab, switchindex, switchbiword, changedir);
      searchbar.setId("bottombar");
      searchbar.setMinHeight(50);
      mainlayout.setBottom(searchbar);


      mainscene = new Scene(mainlayout, 1000,700);
      mainscene.getStylesheets().add("style.css");

      //Index Scene
      StackPane indexlayout = new StackPane();

      switchmain = new Button("Back to Search");
      switchmain.setOnAction(e -> window.setScene(mainscene));

      indexlayout.getChildren().addAll(switchmain);
      indexscene = mainscene;

      //main
      window.setScene(mainscene);
      window.setResizable(false);
      window.show();
   }

   //Queries The User Input in the searchbar.
   private static void userQuery(){
      String userinput = searchbox.getText();
      outputcontent = "Query: "+userinput+"\n\n";
      DocumentProcessing processor = new DocumentProcessing();
      PorterStemmer porter = new PorterStemmer();
      boolean biwordfail = true; // checks if biword finds the query

      // we have exactly 2 words, use biword index
      List<Integer> results = new ArrayList<>();
      if(userinput.split(" ").length == 2 ){
         String[] inputsize = userinput.split(" ");
         String SearchBWord = processor.normalizeToken(inputsize[0])+" "+processor.normalizeToken(inputsize[1]);
         outputcontent+="\nSearching Biword index...\n";
         outputcontent+=SearchBWord+ "\n\t";
         results = bindex.getPostings(SearchBWord);
         if (results!=null && results.size()>0) {
            biwordfail = false;
            for (Integer i : results) {
               outputcontent += "\n\t" + fileNames.get(i);
            }
         }
      }

      // otherwise, use positional inverted index
      if(biwordfail) {
         results = QueryParser.parseQuery(userinput, index);
         if (results.size() > 0) {
            outputcontent += "\nSearching Positional Inverted Index...\n" + userinput + " :";
            for (Integer i : results) {
               outputcontent += "\n\t" + fileNames.get(i);
            }
         }
      }
      outputcontent += "\nResults Returned: "+ results.size();
      if(results.size()==0 || results==null)
         outputcontent+="\n\tTerm not found in the index";
      output.setText(outputcontent);
   }
   // Shows up dialog box to choose corpus
   private static Path chooseFolder(File file) {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle("Choose a Corpus");
      if (file != null) {
         directoryChooser.setInitialDirectory(file);
      }
      return directoryChooser.showDialog(null).toPath();
   }

   private static String getVocab(){
      String[] dictionary = index.getDictionary();
      int count = index.getTermCount();
      String vocab = "Index Dictionary: \n";
      for(String s : dictionary){
         vocab += s + "\n";
      }
      vocab += "Index Term Count: " + count;
      return vocab;
   }

   public static void main(String[] args) throws IOException{
      launch(args);
   }

   public static void index()throws IOException{
      // This is our standard "walk through all .txt files" code.
      System.out.println(currentWorkingPath);
      Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {
         int mDocumentID  = 0;

         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // make sure we only process the current working directory
            if (currentWorkingPath.equals(dir)) {
               return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
         }

         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            // only process .txt files
            if (file.toString().endsWith(".txt")) {
               // we have found a .txt file; add its name to the fileName list,
               // then index the file and increase the document ID counter.
//               System.out.println("Indexing file " + file.getFileName());
//               System.out.println(mDocumentID);

               fileNames.add(file.getFileName().toString());
               // how to get position of the term in the docid
               indexFile(file.toFile(), index, mDocumentID, bindex);
               mDocumentID++;
            }
            //if file is JSON
            if (file.toString().endsWith(".json")) {
//               System.out.println("Indexing JSON file: "+file.getFileName());
//               System.out.println(mDocumentID);
               fileNames.add(file.getFileName().toString());
               indexFile(file.toFile(), index, mDocumentID, bindex);
               mDocumentID++;
            }

            return FileVisitResult.CONTINUE;
         }

         // don't throw exceptions if files are locked/other errors occur
         public FileVisitResult visitFileFailed(Path file, IOException e) {
            return FileVisitResult.CONTINUE;
         }
      });
   }

   private static void indexFile(File file, PositionalInvertedIndex index, int docID, BiwordIndex Bindex) {
      // TO-DO: finish this method for indexing a particular file.
      // Construct a SimpleTokenStream for the given File.
      // Read each token from the stream and add it to the index.
      int Position  = 0;
      // biword index persistent term
      String Biterm = "";
      // biword index counter
      int counter = 0;
      boolean check = false;
      // special biterm for hyphen case 2
      String Sterm = "";

      try {
         TokenStream mTStream = new TokenStream() {
            @Override
            public String nextToken() {
               return null;
            }

            @Override
            public boolean hasNextToken() {
               return false;
            }
         };
         if(file.toString().endsWith(".txt")){
            mTStream = new SimpleTokenStream(file);
         }
         if(file.toString().endsWith(".json")){
            String jsonbody = BodyOutPut.getBodyString(file.toString());
            mTStream = new SimpleTokenStream(jsonbody);
         }
         PorterStemmer porter = new PorterStemmer();
         DocumentProcessing SimplifyTerm = new DocumentProcessing();
         while (mTStream.hasNextToken()){
            // take the next token and put it into term variable
            String term = mTStream.nextToken();
            // this line deal with all aspostrophy and non-alphanumeric
            // also guarantee we get lowercase back
            term  = SimplifyTerm.normalizeIndexedToken(term);

            // remove hyphen
            if(term.contains("-")){
               // grab the hyphen splited
               List<String> splitedTerm = SimplifyTerm.SplitHyphenWord(term);
               // pick up the 3rd item which is the combined word
               // put it in fulterm
               String fulterm = splitedTerm.get(2);

               // create an array of string size 2 assign it value
               // with the 1st and 2nd word from hyphen split
               String[] listterm = new String[2];
               listterm[0] = splitedTerm.get(0);
               listterm[1] = splitedTerm.get(1);

               // PROCESS THE COMBINED TERM FIRST FOR BOTH POSITIONAL AND BIWORD
               // add combined term in POSITIONAL INDEX
               index.addTerm(porter.processToken(fulterm), docID, Position);

               // add combined term in BIWORD INDEX
               // execute 1 time only for 1 case that is
               // we have 1st term in file as a hyphen term
               if(counter == 0 && check == false){
                  Biterm = fulterm;
                  counter++;
               }
               // first term in file is not hyphen
               // and all other hyphen term
               else{
                  Biterm = porter.processToken(Biterm)+" "+porter.processToken(fulterm);
                  Bindex.addTerm(Biterm, docID);
                  Biterm = fulterm;
                  counter = 0;
                  check = true;
               }

               // NOW MOVE ON TO THE SPLITTED PORTION
               for(int i = 0; i < listterm.length; i++){
                  // add to POSITIONAL INDEX
                  index.addTerm(porter.processToken(listterm[i]), docID, Position);
                  // APPEND THE 2 WORD WITH SPACE AT THE END
                  Sterm = Sterm+porter.processToken(listterm[i])+" ";
                  Position++;
               }
               // NOW TRIM THE TRAILING SPACE
               Sterm = Sterm.trim();
               // ADD TO BIWORD INDEX
               Bindex.addTerm(Sterm, docID);
               // reset STERM back to empty string FOR NEXT LOOP AROUND
               Sterm = "";
               // end here loop around at while
               continue;
            }
            index.addTerm( porter.processToken(term), docID, Position);
            Position++;

            // execute at the first read (1st term)
            // only execute 1 time first word
            if(counter == 0 && check == false){
               Biterm = term;
               counter++;
            }
            // this is the condition for the rest of the loop
            // remaining Biterm
            else if(counter == 0 && check == true){
               Biterm = porter.processToken(Biterm)+" "+porter.processToken(term);
               Bindex.addTerm(Biterm, docID);
               Biterm = term;
            }
            // second term also only execute 1 time
            else {
               Biterm = porter.processToken(Biterm)+" "+porter.processToken(term);
               Bindex.addTerm(Biterm, docID);
               Biterm = term;
               counter = 0;
               check = true;
            }
         }
      }catch(FileNotFoundException ex){
         System.out.println("File Not Found");
      }
   }

   private static String printResults(PositionalInvertedIndex index, List<String> fileNames) {
      // print the inverted index.
      String printed = "";
      String[] mDictionary = index.getDictionary();
      for (String s : mDictionary){
         printed+=s+":\n";
         for (PositionArray i : index.getPostings(s) ){
            printed+="\t\t\t"+fileNames.get(i.getDocID());
            printed+="\t\t\t"+ i.getListofPos()+"\n";
         }
      }
      return printed;
   }

   private static String printBiwordResults(BiwordIndex biword, List<String> fileNames) {
      // print the Biwordindex.
      String printed = "";
      String[] mDictionary = biword.getDictionary();
      for (String s : mDictionary){
         printed += s+":\n";
         for (Integer i : biword.getPostings(s) ){
            printed += "\t\t\t"+ fileNames.get(i)+"\n";
         }
      }
      return printed;
   }


}

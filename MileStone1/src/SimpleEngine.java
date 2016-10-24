
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
THIS IS THE PROGRAM WITHOUT GUI. Some parts of this code are not updated up to date.
 Please use GuiMain instead.
*/
public class SimpleEngine {

   public static void main2(String[] args) throws IOException {
      final Path currentWorkingPath = Paths.get("C:\\Users\\Mark\\Documents\\CSULB\\CECS_429 - Search Engine\\Homework\\Homework1\\MobyDick10Chapters").toAbsolutePath();
      
      // the inverted index
      final PositionalInvertedIndex index = new PositionalInvertedIndex();
      
      // the list of file names that were processed
      final List<String> fileNames = new ArrayList<String>();
      
      //the BiwordIndex
      final BiwordIndex bindex = new BiwordIndex();

      
      // This is our standard "walk through all .txt files" code.
      Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {
         int mDocumentID  = 0;
         
         public FileVisitResult preVisitDirectory(Path dir,
          BasicFileAttributes attrs) {
            // make sure we only process the current working directory
            if (currentWorkingPath.equals(dir)) {
               return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
         }

         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            // we have found a .txt file or .json; add its name to the fileName list,
            // then index the file and increase the document ID counter.
            if (file.toString().endsWith(".json") || file.toString().endsWith(".txt")) {
               System.out.println(mDocumentID);
               fileNames.add(file.getFileName().toString());
               indexFile(file.toFile(), index, mDocumentID, bindex);
               mDocumentID++;
            }
            return FileVisitResult.CONTINUE;
         }

         // don't throw exceptions if files are locked/other errors occur
         public FileVisitResult visitFileFailed(Path file,
          IOException e) {

            return FileVisitResult.CONTINUE;
         }

      });
      
      printResults(index, fileNames);
      printBiwordResults(bindex);

      // Main Querying Begins.
      // Ask the user to search for a term. Stops when user enters 'quit'
      Scanner scan = new Scanner(System.in);
      String userinput;
      String[] mDictionary = index.getDictionary();
      DocumentProcessing processor = new DocumentProcessing();

      do{
    	 // pick up user input for query
         System.out.println("Enter a term to search for:  ");
         userinput = scan.nextLine();
         PorterStemmer porter = new PorterStemmer();

         // check for stem and quit command
         if(userinput.startsWith(":stem")){
            String stemmed = porter.processToken(userinput.substring(6,userinput.length()));
            System.out.print(" -> "+stemmed+"\n");
         }
         else if (userinput.startsWith(":quit")){
            //end loop and break
         }
         else{

            // we have more than 1 word
            if(userinput.contains(" ")){
             String[] inputsize = userinput.split(" ");
             // we have exactly 2 word
             if(inputsize.length == 2){
                String SearchBWord = processor.normalizeToken(inputsize[0])+" "+processor.normalizeToken(inputsize[1]);
                System.out.print(SearchBWord+ ":   DocID List : ");
                System.out.println(bindex.getPostings(SearchBWord));
                continue;
             }
            }
            // if we get here then it is not biword process with positional

            List<Integer> results = QueryParser.parseQuery(userinput, index);
            if(results.size()>0){
               System.out.printf("%-15s  ", userinput+":");
               for (Integer i : results ){
                  System.out.printf("%-15s", fileNames.get(i));
                  System.out.printf("%-18s", "\n");
               }
               System.out.print("\n");
            }
            else
               System.out.println("Term not found in the index");
         }
       } while ( !userinput.equals(":quit") );
      
   }

   /**
   Indexes a file by reading a series of tokens from the file, treating each 
   token as a term, and then adding the given document's ID to the inverted
   index for the term.
   @param file a File object for the document to index.
   @param index the current state of the index for the files that have already
   been processed.
   @param docID the integer ID of the current document, needed when indexing
   each term from the document.
   */
   private static void indexFile(File file, PositionalInvertedIndex index, 
    int docID, BiwordIndex Bindex) {
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
            String jsonbody = BodyOutput.getBodyString(file.toString());
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
           	 	
        		// NOW MOVE ON TO THE SPLITED PORTION
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

   private static void printResults(PositionalInvertedIndex index,
    List<String> fileNames) {
     
      // TO-DO: print the inverted index.
      // Retrieve the dictionary from the index. (It will already be sorted.)
      // For each term in the dictionary, retrieve the postings list for the
      // term. Use the postings list to print the list of document names that
      // contain the term. (The document ID in a postings list corresponds to 
      // an index in the fileNames list.)
      
      // Print the postings list so they are all left-aligned starting at the
      // same column, one space after the longest of the term lengths. Example:
      // 
      // as:      document0 document3 document4 document5
      // engines: document1
      // search:  document2 document4
      String[] mDictionary = index.getDictionary();
      for (String s : mDictionary){
         System.out.printf("%-15s  ", s+":");
         for (PositionArray i : index.getPostings(s) ){
            System.out.printf("%-15s", fileNames.get(i.getDocID()));
            System.out.printf("%-15s", i.getListofPos()  );
            System.out.printf("%-18s", "\n");
         }
         System.out.print("\n");
      }
   }
   
   private static void printBiwordResults(BiwordIndex biword) {

	      // TO-DO: print the inverted index.
	      // Retrieve the dictionary from the index. (It will already be sorted.)
	      // For each term in the dictionary, retrieve the postings list for the
	      // term. Use the postings list to print the list of document names that
	      // contain the term. (The document ID in a postings list corresponds to
	      // an index in the fileNames list.)

	      // Print the postings list so they are all left-aligned starting at the
	      // same column, one space after the longest of the term lengths. Example:
	      //
	      // as:      document0 document3 document4 document5
	      // engines: document1
	      // search:  document2 document4
	      String[] mDictionary = biword.getDictionary();
	      for (String s : mDictionary){
	         System.out.printf("%-15s  ", s+":");
	         for (Integer i : biword.getPostings(s) ){
	            System.out.printf("%-4s", i+ " ");
	         }
	         System.out.print("\n");
	      }
	   }
}

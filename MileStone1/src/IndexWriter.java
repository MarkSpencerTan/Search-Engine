import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
Writes an inverted indexing of a directory to disk.
*/
public class IndexWriter {

   private static String mFolderPath;
   private final static PositionalInvertedIndex index = new PositionalInvertedIndex();
   private final static BiwordIndex bindex = new BiwordIndex();
   
   public static void buildIndexs(String folder){
	   // Index the directory using a positional index
	   indexFiles(folder, index, bindex);
   }

   /**
   Constructs an IndexWriter object which is prepared to index the given folder.
   */
   public IndexWriter(String folderPath) {
      mFolderPath = folderPath;
   }

   /**
   Builds and writes an inverted index to disk. Creates three files: 
   vocab.bin, containing the vocabulary of the corpus; 
   postings.bin, containing the postings list of document IDs;
   vocabTable.bin, containing a table that maps vocab terms to postings locations
   */
   public void buildIndex() {
	   buildIndexs(mFolderPath);
      buildIndexForDirectory(mFolderPath);
      buildbwordIndexForDirectory(mFolderPath);
   }

   /**
   Builds the normal PositionalInvertedIndex for the folder.
   */
   private static void buildIndexForDirectory(String folder) {
      // at this point, "index" contains the in-memory inverted index
      // now we save the index to disk, building three files: the postings index,
      // the vocabulary list, and the vocabulary table.

      // the array of terms
      String[] dictionary = index.getDictionary();
      // an array of positions in the vocabulary file
      long[] vocabPositions = new long[dictionary.length];

      buildVocabFile(folder, dictionary, vocabPositions);
      buildPostingsFile(folder, index, dictionary, vocabPositions);
   }
   
   /**
   Builds the normal PositionalInvertedIndex for the folder.
   */
   private static void buildbwordIndexForDirectory(String folder) {
			// at this point, "index" contains the in-memory inverted index 
      // now we save the index to disk, building three files: the postings index,
      // the vocabulary list, and the vocabulary table.

      // the array of terms
      String[] dictionary = bindex.getDictionary();
      // an array of positions in the vocabulary file
      long[] bvocabPositions = new long[dictionary.length];

      buildbwordVocabFile(folder, dictionary, bvocabPositions);
      buildBwordPostingsFile(folder, bindex, dictionary, bvocabPositions);
   }

   /**
   Builds the postings.bin file for the indexed directory, using the given
   NaiveInvertedIndex of that directory.
   */
   private static void buildBwordPostingsFile(String folder, BiwordIndex index,
    String[] dictionary, long[] vocabPositions) {
      FileOutputStream postingsFile = null;
      try {
         postingsFile = new FileOutputStream(
          new File(folder, "bpostings.bin")
         );

         // simultaneously build the vocabulary table on disk, mapping a term index to a
         // file location in the postings file.
         FileOutputStream vocabTable = new FileOutputStream(
          new File(folder, "bvocabTable.bin")
         );

         // the first thing we must write to the vocabTable file is the number of vocab terms.
         byte[] tSize = ByteBuffer.allocate(4)
          .putInt(dictionary.length).array();
         vocabTable.write(tSize, 0, tSize.length);
         int vocabI = 0;
         for (String s : dictionary) {
            // for each String in dictionary, retrieve its postings.
            List<Integer> postings = index.getPostings(s);

            // write the vocab table entry for this term: the byte location of the term in the vocab list file,
            // and the byte location of the postings for the term in the postings file.
            byte[] vPositionBytes = ByteBuffer.allocate(8)
             .putLong(vocabPositions[vocabI]).array();
            vocabTable.write(vPositionBytes, 0, vPositionBytes.length);

            byte[] pPositionBytes = ByteBuffer.allocate(8)
             .putLong(postingsFile.getChannel().position()).array();
            vocabTable.write(pPositionBytes, 0, pPositionBytes.length);

            // write the postings file for this term. first, the document frequency for the term, then
            // the document IDs, encoded as gaps.
            byte[] docFreqBytes = ByteBuffer.allocate(4)
             .putInt(postings.size()).array();
            postingsFile.write(docFreqBytes, 0, docFreqBytes.length);

            int lastDocId = 0;
            for (int docId : postings) {
               byte[] docIdBytes = ByteBuffer.allocate(4)
                .putInt(docId - lastDocId).array(); // encode a gap, not a doc ID

               postingsFile.write(docIdBytes, 0, docIdBytes.length);
               lastDocId = docId;
            }

            vocabI++;
         }
         vocabTable.close();
         postingsFile.close();
      }
      catch (FileNotFoundException ex) {
    	  System.out.println(ex.toString() + "error code 1");
      }
      catch (IOException ex) {
    	  System.out.println(ex.toString() + "error code 2");
      }
      finally {
         try {
            postingsFile.close();
         }
         catch (IOException ex) {
        	 System.out.println(ex.toString() + "error code 3");
         }
      }
   }
   
   /**
   Builds the postings.bin file for the indexed directory, using the given
   NaiveInvertedIndex of that directory.
   */
   private static void buildPostingsFile(String folder, PositionalInvertedIndex index,
    String[] dictionary, long[] vocabPositions) {
      FileOutputStream postingsFile = null;
      try {
         postingsFile = new FileOutputStream(
          new File(folder, "postings.bin")
         );

         // simultaneously build the vocabulary table on disk, mapping a term index to a
         // file location in the postings file.
         FileOutputStream vocabTable = new FileOutputStream(
          new File(folder, "vocabTable.bin")
         );

         // the first thing we must write to the vocabTable file is the number of vocab terms.
         byte[] tSize = ByteBuffer.allocate(4)
          .putInt(dictionary.length).array();
         vocabTable.write(tSize, 0, tSize.length);
         int vocabI = 0;
         for (String s : dictionary) {
            // for each String in dictionary, retrieve its postings.
            //List<Integer> postings = index.getPostings(s);
/**/        List<PositionArray> postings = index.getPostings(s);
            
            // write the vocab table entry for this term: the byte location of the term in the vocab list file,
            // and the byte location of the postings for the term in the postings file.
            byte[] vPositionBytes = ByteBuffer.allocate(8)
             .putLong(vocabPositions[vocabI]).array();
            vocabTable.write(vPositionBytes, 0, vPositionBytes.length);

            byte[] pPositionBytes = ByteBuffer.allocate(8)
             .putLong(postingsFile.getChannel().position()).array();
            vocabTable.write(pPositionBytes, 0, pPositionBytes.length);

            // write the postings file for this term. first, the document frequency for the term, then
            // the document IDs, encoded as gaps.
            byte[] docFreqBytes = ByteBuffer.allocate(4)
             .putInt(postings.size()).array();
            postingsFile.write(docFreqBytes, 0, docFreqBytes.length);

            // the doc id / term freq/ pos writing happen here
            int lastDocId = 0;
            for (int i = 0; i < postings.size(); i++) {
               byte[] docIdBytes = ByteBuffer.allocate(4)
                .putInt(postings.get(i).getDocID() - lastDocId).array(); // encode a gap, not a doc ID

               postingsFile.write(docIdBytes, 0, docIdBytes.length);
               lastDocId = postings.get(i).getDocID();
               
               // now write the term frequency
               byte[] termFreqBytes = ByteBuffer.allocate(4)
                 .putInt(postings.get(i).getListofPos().size()).array();
               postingsFile.write(termFreqBytes, 0, docFreqBytes.length);
                 
               int lastposId = 0;  
               // use a for loop to write the position size of term frequency
               for (int j = 0; j < postings.get(i).getListofPos().size(); j++) {
                   byte[] posIdBytes = ByteBuffer.allocate(4)
                    .putInt(postings.get(i).getListofPos().get(j) - lastposId).array();
                   postingsFile.write(posIdBytes, 0, docFreqBytes.length);
                   lastposId = postings.get(i).getListofPos().get(j); 
               }
            }

            vocabI++;
         }
         vocabTable.close();
         postingsFile.close();
      }
      catch (FileNotFoundException ex) {
    	  System.out.println(ex.toString() + "error code 4");
      }
      catch (IOException ex) {
    	  System.out.println(ex.toString() + "error code 5");
      }
      finally {
         try {
            postingsFile.close();
         }
         catch (IOException ex) {
        	 System.out.println(ex.toString() + "error code 6");
         }
      }
   }

   private static void buildVocabFile(String folder, String[] dictionary,
    long[] vocabPositions) {
      OutputStreamWriter vocabList = null;
      try {
         // first build the vocabulary list: a file of each vocab word concatenated together.
         // also build an array associating each term with its byte location in this file.
         int vocabI = 0;
         vocabList = new OutputStreamWriter(
          new FileOutputStream(new File(folder, "vocab.bin")), "ASCII"
         );
         
         int vocabPos = 0;
         for (String vocabWord : dictionary) {
            // for each String in dictionary, save the byte position where that term will start in the vocab file.
            vocabPositions[vocabI] = vocabPos;
            vocabList.write(vocabWord); // then write the String
            vocabI++;
            vocabPos += vocabWord.length();
         }
      }
      catch (FileNotFoundException ex) {
    	  System.out.println(ex.toString() + "error code 7");
      }
      catch (UnsupportedEncodingException ex) {
    	  System.out.println(ex.toString() + "error code 8");
      }
      catch (IOException ex) {
    	  System.out.println(ex.toString() + "error code 9");
      }
      finally {
         try {
            vocabList.close();
         }
         catch (IOException ex) {
        	 System.out.println(ex.toString() + "error code 10");
         }
      }
   }
   
   private static void buildbwordVocabFile(String folder, String[] dictionary,
		    long[] vocabPositions) {
		      OutputStreamWriter vocabList = null;
		      try {
		         // first build the vocabulary list: a file of each vocab word concatenated together.
		         // also build an array associating each term with its byte location in this file.
		         int vocabI = 0;
		         vocabList = new OutputStreamWriter(
		          new FileOutputStream(new File(folder, "bvocab.bin")), "ASCII"
		         );
		         
		         int vocabPos = 0;
		         for (String vocabWord : dictionary) {
		            // for each String in dictionary, save the byte position where that term will start in the vocab file.
		            vocabPositions[vocabI] = vocabPos;
		            vocabList.write(vocabWord); // then write the String
		            vocabI++;
		            vocabPos += vocabWord.length();
		         }
		      }
		      catch (FileNotFoundException ex) {
		    	  System.out.println(ex.toString() + "error code 11");
		      }
		      catch (UnsupportedEncodingException ex) {
		         System.out.println(ex.toString() + "error code 12");
		      }
		      catch (IOException ex) {
		         System.out.println(ex.toString() + "error code 13");
		      }
		      finally {
		         try {
		            vocabList.close();
		         }
		         catch (IOException ex) {
		            System.out.println(ex.toString() + "error code 14");
		         }
		      }
		   }

   private static void indexFiles(String folder, final PositionalInvertedIndex index, final BiwordIndex bindex) {
      int documentID = 0;
      final Path currentWorkingPath = Paths.get(folder).toAbsolutePath();
      try {
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
            
            public FileVisitResult visitFile(Path file,
             BasicFileAttributes attrs) {
               if (file.toString().endsWith(".txt") || file.toString().endsWith(".json")) {
                  // we have found a .txt file; add its name to the fileName list,
                  // then index the file and increase the document ID counter.
                  
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

         // write the docweight into a binary file
         buildweightFile(index.getWeightArray());

      }
      catch (IOException ex) {
         Logger.getLogger(IndexWriter.class.getName()).log(Level.SEVERE, null, ex);
         System.out.println(ex.toString() + "error code 15");
      }

   }

   private static void indexFile(File file, PositionalInvertedIndex index, int docID, BiwordIndex Bindex) {
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
      // document weight
      double docweight = 0;

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
          // add the normalized term into the weight table
          index.addWeight(porter.processToken(term));
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

            if(fulterm.trim().length()>0)
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
                  if(Biterm.trim().length() > 0) //checks for whitespaces
                     Bindex.addTerm(Biterm, docID);
                  Biterm = fulterm;
                  counter = 0;
                  check = true;
               }

            // NOW MOVE ON TO THE SPLITED PORTION
            for(int i = 0; i < listterm.length; i++){
               String temp = porter.processToken(listterm[i]);
                  // add to POSITIONAL INDEX if no whitespaces
                  if(temp.trim().length()>0)
                  index.addTerm(temp, docID, Position);
                  // APPEND THE 2 WORD WITH SPACE AT THE END
                  Sterm = Sterm + temp + " ";
                  Position++;
            }
            // NOW TRIM THE TRAILING SPACE
            Sterm = Sterm.trim();
            // ADD TO BIWORD INDEX
            if(Sterm.trim().length() > 0)
               Bindex.addTerm(Sterm, docID);
            // reset STERM back to empty string FOR NEXT LOOP AROUND
            Sterm = "";
            // end here loop around at while
            continue;
          }
          if(term.trim().length()>0) {
             index.addTerm( porter.processToken(term), docID, Position);
          }
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
             if(Biterm.trim().length() > 0)
                Bindex.addTerm(Biterm, docID);
             Biterm = term;
          }
          // second term also only execute 1 time
          else {
             Biterm = porter.processToken(Biterm)+" "+porter.processToken(term);
             if(Biterm.trim().length() > 0)
                Bindex.addTerm(Biterm, docID);
             Biterm = term;
             counter = 0;
             check = true;
          }
         }
         // pick up the weight of the document
         docweight = index.getDocWeight();
         // clean up the hash table for the next document
         index.addWeightToArray(docweight);
         index.cleanWeighttable();

      }catch(FileNotFoundException ex){
         System.out.println("File Not Found");
         System.out.println(ex.toString() + "error code 16");
      }
   }
   private static void buildweightFile (ArrayList<Double> weightArr) {
	      FileOutputStream weightsFile = null;
	      //weightsFile.getChannel().position()
	      try {
	         weightsFile = new FileOutputStream(new File(mFolderPath, "weight.bin"));
            for(double w : weightArr){
               byte[] weightBytes = ByteBuffer.allocate(8)
                       .putDouble(w).array();
               weightsFile.write(weightBytes, 0, weightBytes.length);
            }
	      }
	      catch (FileNotFoundException ex) {
	       	  System.out.println(ex.toString() + "error code weight 1");
	      }
	      catch (IOException ex) {
	       	  System.out.println(ex.toString() + "error code weight 2");
	      }
	      finally {
	         try {
	        	 weightsFile.close();
	         }
	         catch (IOException ex) {
	           	 System.out.println(ex.toString() + "error code weight 3");
	         }
	      }
   }
}

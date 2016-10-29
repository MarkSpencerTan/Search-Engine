
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DiskInvertedIndex {

   private String mPath;
   private List<String> mFileNames;
   private List<Double> mWeights;
   
   private static RandomAccessFile mWeightlist;
   
   private RandomAccessFile mVocabList;
   private RandomAccessFile mPostings;
   private long[] mVocabTable;
   
   private RandomAccessFile mVocabListbiword;
   private RandomAccessFile mPostingsbiword;
   private long[] mVocabTablebiword;

   public DiskInvertedIndex(String path) {
      try {
         mPath = path;
         mFileNames = readFileNames(path);
         
         mWeightlist = new RandomAccessFile(new File(path, "weight.bin"), "r");

         mVocabList = new RandomAccessFile(new File(path, "vocab.bin"), "r");
         mPostings = new RandomAccessFile(new File(path, "postings.bin"), "r");
         mVocabTable = readVocabTable(path);
         
         mVocabListbiword = new RandomAccessFile(new File(path, "bvocab.bin"), "r");
         mPostingsbiword = new RandomAccessFile(new File(path, "bpostings.bin"), "r");
         mVocabTablebiword = readVocabTablebiword(path);

         // Fill in weights List


      }
      catch (FileNotFoundException ex) {
         System.out.println(ex.toString());
      }
   }
   // read from weight.bin each doc is 8 byte starting from doc 0 to doc max
   public static double readWeightFromFile(int docNumber) {
	   double weight = 0;
	   // doc number start at 0 so first doc will have doc number = 0
	   // second document will have doc number = 1
	   try {
         // offsets the pointer to the document location
		   mWeightlist.seek(docNumber*8);
         // read the 8 bytes for the weight
         byte[] buffer = new byte[8];
	         
         // read the weight
         mWeightlist.read(buffer, 0, buffer.length);
         // use ByteBuffer to convert the 8 bytes into a double.
         weight = ByteBuffer.wrap(buffer).getDouble();
	   } 
	   catch (IOException ex) {
	         System.out.println(ex.toString());
	   }

      System.out.println(docNumber + "weight: "+weight);
      return weight;
   }
   
   private static PostingResult readPostingsFromFile(RandomAccessFile postings, 
    long postingsPosition) {
      try {
    	   PostingResult result = new PostingResult();
         // seek to the position in the file where the postings start.
         postings.seek(postingsPosition);
         
         // read the 4 bytes for the document frequency
         byte[] buffer = new byte[4];
         
         // read the doc frequency (how many doc contain the term)
         postings.read(buffer, 0, buffer.length);

         // use ByteBuffer to convert the 4 bytes into an int.
         int documentFrequency = ByteBuffer.wrap(buffer).getInt();
         
         // initialize the array that will hold the postings. 
         //int[] docIds = new int[documentFrequency];
         int docIds = 0;

         // write the following code:
         // read 4 bytes at a time from the file, until you have read as many
         //    postings as the document frequency promised.
         //    
         // after each read, convert the bytes to an int posting. this value
         //    is the GAP since the last posting. decode the document ID from
         //    the gap and put it in the array.
         //
         // repeat until all postings are read.
         int previousID = 0;
         int positionSize = 0;
         for(int i = 0; i < documentFrequency; i++){
        	 // read the first doc id 
        	 postings.read(buffer, 0, buffer.length);
        	 docIds = ByteBuffer.wrap(buffer).getInt() + previousID;
        	 previousID = docIds;
        	 // now read the TFtd term frequency (# of occurence/positions)
           	 postings.read(buffer, 0, buffer.length);
           	 positionSize = ByteBuffer.wrap(buffer).getInt();
           	 
           	 // initialize the array that will hold the positions. 
             int[] posIds = new int[positionSize];
             int previousPos = 0;
             
           	 // now loop and read all of the position              
           	 for(int j = 0; j < positionSize; j++){
           		// read the first position
           		postings.read(buffer, 0, buffer.length);
           		// put it in an array
           		posIds[j] = ByteBuffer.wrap(buffer).getInt() + previousPos;
           		previousPos = posIds[j];
           	 }
           	 // now add the docIds into result of type PostingResult
           	 // as a key and then get that key and add the posIds in as 
           	 // a value array
           	 result.addTerm(docIds, posIds);
         }
         return result;
      }
      catch (IOException ex) {
         System.out.println(ex.toString());
      }
      return null;
   }

   public PostingResult getPostings(String term) {
      long postingsPosition = binarySearchVocabulary(term);
      if (postingsPosition >= 0) {
         return readPostingsFromFile(mPostings, postingsPosition);
      }
      return new PostingResult();
   }
   private static List<Integer> readBwordPostingsFromFile(RandomAccessFile postings,
                                                          long postingsPosition) {
      try {
         // seek to the position in the file where the postings start.
         postings.seek(postingsPosition);

         // read the 4 bytes for the document frequency
         byte[] buffer = new byte[4];
         postings.read(buffer, 0, buffer.length);

         // use ByteBuffer to convert the 4 bytes into an int.
         int documentFrequency = ByteBuffer.wrap(buffer).getInt();
         System.out.println("Doc Freq: " + documentFrequency);

         // initialize the array that will hold the postings.
         List<Integer> docIds = new ArrayList<>();

         // write the following code:
         // read 4 bytes at a time from the file, until you have read as many
         //    postings as the document frequency promised.
         //
         // after each read, convert the bytes to an int posting. this value
         //    is the GAP since the last posting. decode the document ID from
         //    the gap and put it in the array.
         //
         // repeat until all postings are read.
         int previousID = 0;
         for(int i = 0; i < documentFrequency; i++){
          postings.read(buffer, 0, buffer.length);
          docIds.add(i,ByteBuffer.wrap(buffer).getInt() + previousID);
          previousID = docIds.get(i);
         }
         return docIds;
      }
      catch (IOException ex) {
         System.out.println(ex.toString());
      }
      return null;
   }

   public List<Integer> GetBwordPostings(String term) {
	      long postingsPosition = BiwordbinarySearchVocabulary(term);
	      if (postingsPosition >= 0) {
	         return readBwordPostingsFromFile(mPostingsbiword, postingsPosition);
	      }
	      return null;
   }
   private long BiwordbinarySearchVocabulary(String term) {
	      // do a binary search over the vocabulary, using the vocabTable and the file vocabList.
	      int i = 0, j = mVocabTablebiword.length / 2 - 1;
	      while (i <= j) {
	         try {
	            int m = (i + j) / 2;
               System.out.println(m +" "+ mVocabTablebiword.length);
               long vListPosition = mVocabTablebiword[m * 2];
	            int termLength;
	            if (m == mVocabTablebiword.length / 2 - 1){
	               termLength = (int)(mVocabListbiword.length() - mVocabTablebiword[m*2]);
	            }
	            else {
	               termLength = (int) (mVocabTablebiword[(m + 1) * 2] - vListPosition);
	            }

	            mVocabListbiword.seek(vListPosition);

	            byte[] buffer = new byte[termLength];
	            mVocabListbiword.read(buffer, 0, termLength);
	            String fileTerm = new String(buffer, "ASCII");

	            int compareValue = term.compareTo(fileTerm);
	            if (compareValue == 0) {
	               // found it!
	               return mVocabTablebiword[m * 2 + 1];
	            }
	            else if (compareValue < 0) {
	               j = m - 1;
	            }
	            else {
	               i = m + 1;
	            }
	         }
	         catch (IOException ex) {
	            System.out.println(ex.toString());
	         }
	      }
	      return -1;
	   }
   private long binarySearchVocabulary(String term) {
      // do a binary search over the vocabulary, using the vocabTable and the file vocabList.
      int i = 0, j = mVocabTable.length / 2 - 1;
      while (i <= j) {
         try {
            int m = (i + j) / 2;
            long vListPosition = mVocabTable[m * 2];
            int termLength;
            if (m == mVocabTable.length / 2 - 1){
               termLength = (int)(mVocabList.length() - mVocabTable[m*2]);
            }
            else {
               termLength = (int) (mVocabTable[(m + 1) * 2] - vListPosition);
            }

            mVocabList.seek(vListPosition);

            byte[] buffer = new byte[termLength];
            mVocabList.read(buffer, 0, termLength);
            String fileTerm = new String(buffer, "ASCII");

            int compareValue = term.compareTo(fileTerm);
            if (compareValue == 0) {
               // found it!
               return mVocabTable[m * 2 + 1];
            }
            else if (compareValue < 0) {
               j = m - 1;
            }
            else {
               i = m + 1;
            }
         }
         catch (IOException ex) {
            System.out.println(ex.toString());
         }
      }
      return -1;
   }


   private static List<String> readFileNames(String indexName) {
      try {
         final List<String> names = new ArrayList<String>();
         final Path currentWorkingPath = Paths.get(indexName).toAbsolutePath();

         Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {
            int mDocumentID = 0;

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
               // only process .txt files
               if (file.toString().endsWith(".json")) {
                  names.add(file.toFile().getName());
               }

               return FileVisitResult.CONTINUE;
            }

            // don't throw exceptions if files are locked/other errors occur
            public FileVisitResult visitFileFailed(Path file,
             IOException e) {

               return FileVisitResult.CONTINUE;
            }

         });
         return names;
      }
      catch (IOException ex) {
         System.out.println(ex.toString());
      }
      return null;
   }

   private static long[] readVocabTable(String indexName) {
      try {
         long[] vocabTable;
         
         RandomAccessFile tableFile = new RandomAccessFile(
          new File(indexName, "vocabTable.bin"),
          "r");
         
         byte[] byteBuffer = new byte[4];
         tableFile.read(byteBuffer, 0, byteBuffer.length);
        
         int tableIndex = 0;
         vocabTable = new long[ByteBuffer.wrap(byteBuffer).getInt() * 2];
         byteBuffer = new byte[8];
         
         while (tableFile.read(byteBuffer, 0, byteBuffer.length) > 0) { // while we keep reading 4 bytes
            vocabTable[tableIndex] = ByteBuffer.wrap(byteBuffer).getLong();
            tableIndex++;
         }
         tableFile.close();
         return vocabTable;
      }
      catch (FileNotFoundException ex) {
         System.out.println(ex.toString());
      }
      catch (IOException ex) {
         System.out.println(ex.toString());
      }
      return null;
   }

   public List<String> getFileNames() {
      return mFileNames;
   }
   
   public int getTermCount() {
      return mVocabTable.length / 2;
   }
   
   private static long[] readVocabTablebiword(String indexName) {
	      try {
	         long[] vocabTable;
	         
	         RandomAccessFile tableFile = new RandomAccessFile(
	          new File(indexName, "bvocabTable.bin"),
	          "r");
	         
	         byte[] byteBuffer = new byte[4];
	         tableFile.read(byteBuffer, 0, byteBuffer.length);
	        
	         int tableIndex = 0;
	         vocabTable = new long[ByteBuffer.wrap(byteBuffer).getInt() * 2];
	         byteBuffer = new byte[8];
	         
	         while (tableFile.read(byteBuffer, 0, byteBuffer.length) > 0) { // while we keep reading 4 bytes
	            vocabTable[tableIndex] = ByteBuffer.wrap(byteBuffer).getLong();
	            tableIndex++;
	         }
	         tableFile.close();
	         return vocabTable;
	      }
	      catch (FileNotFoundException ex) {
	         System.out.println(ex.toString());
	      }
	      catch (IOException ex) {
	         System.out.println(ex.toString());
	      }
	      return null;
   }

}

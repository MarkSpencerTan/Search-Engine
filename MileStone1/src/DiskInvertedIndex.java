
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

      return weight;
   }

   //Slow Down Here
   private static List<Posting> readPostingsFromFile(RandomAccessFile postings,
                                                      long postingsPosition) {
      try {
         // seek to the position in the file where the postings start.
         postings.seek(postingsPosition);

         // read the 4 bytes for the document frequency
         byte[] buffer = new byte[4];

         // read the doc frequency (how many doc contain the term)
         postings.read(buffer, 0, buffer.length);

         // use ByteBuffer to convert the 4 bytes into an int.
         int documentFrequency = ByteBuffer.wrap(buffer).getInt();

         // initialize the array that will hold the postings.
         List<Posting> result = new ArrayList<>();

         // write the following code:
         // read 4 bytes at a time from the file, until you have read as many
         //    postings as the document frequency promised.
         //
         // after each read, convert the bytes to an int posting. this value
         //    is the GAP since the last posting. decode the document ID from
         //    the gap and put it in the array.
         //
         // repeat until all postings are read.

         int currentID = 0;
         int positionSize = 0;
         for(int i = 0; i < documentFrequency; i++){
            // read the gap
            postings.read(buffer, 0, buffer.length);
            int gap = ByteBuffer.wrap(buffer).getInt();
            currentID += gap;

            // now read the TFtd term frequency (# of occurence/positions)
            postings.read(buffer, 0, buffer.length);
            positionSize = ByteBuffer.wrap(buffer).getInt();

            // initialize the array that will hold the positions.
            List<Integer> positions = new ArrayList<>();
            int previousPos = 0;

            // now loop and read all of the position gaps
            for(int j = 0; j < positionSize; j++){
               // read the first position
               postings.read(buffer, 0, buffer.length);
               // put it in the position list
               int currentpos = ByteBuffer.wrap(buffer).getInt() + previousPos;
               positions.add(currentpos);
               previousPos = currentpos;
            }
            result.add(new Posting(currentID, positionSize, positions));
         }
         return result;
      }
      catch (IOException ex) {
         System.out.println(ex.toString());
      }
      return null;
   }

   // Same method as above but without positions for ranked.
   private static List<Posting> readPostingsNoPosition(RandomAccessFile postings, long postingsPosition) {
      try {
         // seek to the position in the file where the postings start.
         postings.seek(postingsPosition);

         // read the 4 bytes for the document frequency
         byte[] buffer = new byte[4];

         // read the doc frequency (how many doc contain the term)
         postings.read(buffer, 0, buffer.length);

         // use ByteBuffer to convert the 4 bytes into an int.
         int documentFrequency = ByteBuffer.wrap(buffer).getInt();

         // initialize the array that will hold the postings.
         List<Posting> result = new ArrayList<>();

         int currentID = 0;
         int positionSize = 0;
         for(int i = 0; i < documentFrequency; i++){
            // read the gap
            postings.read(buffer, 0, buffer.length);
            int gap = ByteBuffer.wrap(buffer).getInt();
            currentID += gap;

            // now read the TFtd term frequency (# of occurence/positions)
            postings.read(buffer, 0, buffer.length);
            positionSize = ByteBuffer.wrap(buffer).getInt();

            // Skip over the positions
            postings.skipBytes(buffer.length * positionSize);

            result.add(new Posting(currentID, positionSize));
         }
         return result;
      }
      catch (IOException ex) {
         System.out.println(ex.toString());
      }
      return null;
   }

   public List<Posting> getPostings(String term) {
      long postingsPosition = binarySearchVocabulary(term);
      if (postingsPosition >= 0) {
         return readPostingsFromFile(mPostings, postingsPosition);
      }
      return new ArrayList<>();
   }

   public List<Posting> getPostingsNoPosition(String term) {
      long postingsPosition = binarySearchVocabulary(term);
      if (postingsPosition >= 0) {
         return readPostingsNoPosition(mPostings, postingsPosition);
      }
      return new ArrayList<>();
   }


   private static List<Integer> readBwordPostingsFromFile(RandomAccessFile postings, long postingsPosition) {
      try {
         // seek to the position in the file where the postings start.
         postings.seek(postingsPosition);

         // read the 4 bytes for the document frequency
         byte[] buffer = new byte[4];
         postings.read(buffer, 0, buffer.length);

         // use ByteBuffer to convert the 4 bytes into an int.
         int documentFrequency = ByteBuffer.wrap(buffer).getInt();

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

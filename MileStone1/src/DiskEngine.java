import java.util.List;
import java.util.Scanner;


public class DiskEngine {

   public static void main(String[] args) {
      Scanner scan = new Scanner(System.in);

      System.out.println("Menu:");
      System.out.println("1) Build index");
      System.out.println("2) Read and query index");
      System.out.println("Choose a selection:");
      int menuChoice = scan.nextInt();
      scan.nextLine();

      switch (menuChoice) {
         case 1:
            System.out.println("Enter the name of a directory to index: ");
            String folder = scan.nextLine();

            IndexWriter writer = new IndexWriter(folder);
            writer.buildIndex();
            break;

         case 2:
            System.out.println("Enter the name of an index to read:");
            String indexName = scan.nextLine();

            DiskInvertedIndex index = new DiskInvertedIndex(indexName);

            while (true) {
               System.out.println("Enter one or more search terms, separated " +
                "by spaces:");
               String input = scan.nextLine();

               if (input.equals("EXIT")) {
                  break;
               }
               DocumentProcessing dp = new DocumentProcessing();
               input = dp.normalizeIndexedToken(input);

               System.out.println("Stemmed: "+PorterStemmer.processToken(input.toLowerCase()));
               
               /// positional
               PostingResult postingsList = index.getPostings(
                  PorterStemmer.processToken(input.toLowerCase())
               );

               if (postingsList == null) {
                  System.out.println("Term not found");
               }
               else {
                  System.out.print("Docs: ");
                  for (int post : postingsList.getDictionary()) {
                     System.out.print(index.getFileNames().get(post));
                     System.out.print(" positions list size is "+ postingsList.getPosition(post).length+ " positions list: ");
                     for(int x = 0; x < postingsList.getPosition(post).length; x++){                   	
                    	 System.out.print(postingsList.getPosition(post)[x]+",");
                     }
                  }
                  System.out.println();
                  System.out.println();
               }
               
               // biword	
               List<Integer> bwordpostingsList = index.GetBwordPostings(
                       PorterStemmer.processToken(input.toLowerCase())
                      );

               if (bwordpostingsList == null) {
            	   System.out.println("Term not found in bword");
               }
               else {
                   System.out.print("bword Docs: ");
                   for (int post : bwordpostingsList) {
                	   System.out.print(index.getFileNames().get(post) + " ");
                   }
                   System.out.println();
                   System.out.println();
               }
            }

            break;
      }
   }
}

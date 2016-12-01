import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MAPMain{


   private static StringBuffer outputcontent = new StringBuffer();

   private static Path currentWorkingPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

   private static String mFormula;
   private static String menuoption = "";

   // the inverted index
   private static DiskInvertedIndex diskindex;
   // the list of file names that were processed
   private static List<String> fileNames = new ArrayList<>();

   // Scanner
   private static Scanner scan = new Scanner(System.in);


   //Queries The User Input in the searchbar.
   private static void userQuery(){
      String userinput = scan.nextLine();
      outputcontent.append("Query: "+userinput+"\n\n");
      DocumentProcessing processor = new DocumentProcessing();
      RankedQueryParser rankedparser = new RankedQueryParser(diskindex);
      boolean biwordfail = true; // checks if biword finds the query

      List<Integer> results = new ArrayList<>();
      // Ranked results is a List of  List<DocID, score>
      List<ScoredDocument> rankedresults = new ArrayList<>();

      // PQ for ranked results
      PriorityQueue<ScoredDocument> docqueue = rankedparser.rankDocuments(userinput, mFormula);
      outputcontent.append("\nTotal Documents Returned: "+docqueue.size());

      List<ScoredDocument> top10 = new ArrayList<>();
      // remove the top 20 from the priority queue
      for(int i=0; i<20 && !docqueue.isEmpty(); i++){
         top10.add(docqueue.poll());
      }
      rankedresults = top10;

      if(rankedresults.size() > 0){
         outputcontent.append("\nSearching Disk Inverted Index (ranked)...\n" + userinput + " :");
         for (ScoredDocument i : rankedresults) {
            outputcontent.append("\n"+fileNames.get(i.getId()) +"\t\tScore: "+ i.getScore());
         }
      }

      outputcontent.append("\n"+ rankedresults.size()+" Results Returned");

      //if no results are found
      if(results == null || (results.isEmpty()&& rankedresults.isEmpty()))
         outputcontent.append("\n\tNo Results Found.");

      System.out.print(outputcontent.toString());
   }


   public static void main(String[] args){
      System.out.println("Choose a mode\n1)Build Disk Index \n2)Find M.A.P");
      int choice = scan.nextInt();
      scan.nextLine();
      switch(choice){
         case 1:
            System.out.println("Choose corpus to index into disk...");
            String p = chooseFolder(currentWorkingPath.toFile()).toString();
            IndexWriter writer = new IndexWriter(p);
            writer.buildIndex();

            if(promptToRead()) {
               diskindex = new DiskInvertedIndex(p);
               fileNames = diskindex.getFileNames();
            }
            break;

         case 2:
            chooseFormula();

            System.out.println("Choose a Disk Index to read...");
            String p1 = chooseFolder(currentWorkingPath.toFile()).toString();
            diskindex = new DiskInvertedIndex(p1);
            fileNames = diskindex.getFileNames();
            System.out.println("Disk index created at: " + p1);

            List<String> queries = getQueries();
            HashMap<Integer, List<Integer>> relevantdocs = getRelevance();

            System.out.println(relevantdocs.get(0));
            break;
      }
   }

   // Choose a formula for ranked
   private static void chooseFormula(){
      System.out.println("Choose a formula to evaluate: ");
      String[] formulas = {"Default", "tf-idf", "Okapi BM25", "Wacky"};

      for(int i=0; i<4; i++){
         System.out.println((i+1) + ")" + formulas[i]);
      }

      int selected = scan.nextInt();
      scan.nextLine();

      if(selected==1){
         mFormula = "Default";
      }
      else if(selected==2){
         mFormula = "tf-idf";
      }
      else if(selected==3){
         mFormula = "Okapi BM25";
      }
      else if(selected==4){
         mFormula = "Wacky";
      }
   }

   // prompts if user wants to read on the same directory after writing index
   private static boolean promptToRead(){
      System.out.println("Read into the same directory? (y/n)");
      String result = scan.nextLine();
      if (result.equals("y")) {
         return true;
      }
      return false;
   }

   // Shows up dialog box to choose a file directory
   private static Path chooseFolder(File file) {
      System.out.println("Enter name of the folder: ");
      String stringpath = file.toString() + "\\" + scan.nextLine();
      return Paths.get(stringpath);
   }

   private static List<String> getQueries(){
      List<String> queries = new ArrayList<String>();
      try
      {
         BufferedReader reader = new BufferedReader(new FileReader("queries.txt"));
         String line;
         while ((line = reader.readLine()) != null)
         {
            queries.add(line);
         }
         reader.close();
         return queries;
      }
      catch (Exception e)
      {
         System.err.format("Exception occurred trying to read '%s'.", "queries.txt");
         e.printStackTrace();
         return null;
      }
   }

   private static HashMap<Integer, List<Integer>> getRelevance(){
      HashMap<Integer, List<Integer>> relevance = new HashMap<>();
      try
      {
         BufferedReader reader = new BufferedReader(new FileReader("relevance.txt"));
         String line;
         int docid = 0;
         while ((line = reader.readLine()) != null) {
            List<Integer> relevantdocs = new ArrayList<>();
            // Scan the line for integers
            Scanner s = new Scanner(line);
            while(s.hasNext()) {
               relevantdocs.add(s.nextInt());
            }
            // add the docid and relevant doc list to hashmap
            relevance.put(docid, relevantdocs);
            docid++;
            s.close();
         }
         reader.close();
         return relevance;
      }
      catch (Exception e)
      {
         System.err.format("Exception occurred trying to read '%s'.", "relevance.txt");
         e.printStackTrace();
         return null;
      }
   }


}

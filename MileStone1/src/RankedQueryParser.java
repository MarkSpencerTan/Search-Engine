import java.util.*;

public class RankedQueryParser extends QueryParser {

   // Rank the documents returned from a boolean query by score
   public static List<ScoredDocument> rankDocuments(String query, PositionalInvertedIndex index){
      //Create a Priority Queue to sort ranked documents
      PriorityQueue<ScoredDocument> docqueue = new PriorityQueue<ScoredDocument>(10, new Comparator<ScoredDocument>() {
         public int compare(ScoredDocument doc1, ScoredDocument doc2) {
            if (doc1.getScore() < doc2.getScore())
            {
               return -1;
            }
            else if (doc1.getScore() > doc2.getScore())
            {
               return 1;
            }
            return 0;
         }
      });

      List<Integer> results = orQuery(query, index);
      for(int docId : results){
         //score each document
         float score = score(docId, index);
         //add doc and score to the PQ as a list
         docqueue.add(new ScoredDocument(docId, score));
      }

      List<ScoredDocument> top10 = new ArrayList<>();
      // remove the top 10 from the priority queue
      for(int i=0; i<10 && i<docqueue.size(); i++){
         top10.add(i, docqueue.remove()); //add docId to top 10
      }
      return top10;
   }

   //Scores a document
   private static float score(int docId, PositionalInvertedIndex index){

      return 1;
   }

   //retrieves a list of documents that have at least 1 of the terms in the query
   private static List<Integer> orQuery(String query, PositionalInvertedIndex index){
      DocumentProcessing dp = new DocumentProcessing();
      List<Integer> postings = new ArrayList<>();     //final merged postings list
      String[] orsplit = query.split("\\+");    //splits the query by + if there's any

      //loops through the orsplit list
      for(int i=0; i< orsplit.length; i++){
         String[] andsplit = splitQuotes(orsplit[i]); //splits each element from orsplit on quotes and spaces

         // Normalizes items on orsplit, only if it's not a phrase
         for( int j =0; j<andsplit.length;j++){
            if (andsplit[j].split(" ").length==1)
               andsplit[j] = dp.normalizeToken(andsplit[j]);
         }

         //will contain document IDs of the current string in andsplit
         List<Integer> ormerge;
         if (andsplit[0].split(" ").length==1){
            ormerge = getDocList(index.getPostings(andsplit[0]));    // single word found, get postings list
         }
         else
            ormerge =  phraseParser(andsplit[0].split(" "), index);  // phrase detected in andsplit, use phraseparser

         // perform an or-merge on the doclist of each string in andsplit
         for(int j=1; j<andsplit.length; j++){
            if (andsplit[j].split(" ").length==1)
               ormerge = orMerge(getDocList(index.getPostings(andsplit[j])), ormerge);
            else  //parse phrase query
               ormerge =  orMerge(phraseParser(andsplit[j].split(" "), index), ormerge);
         }
         // Or-merge the final lists together
         postings = orMerge(ormerge, postings);
      }
      return postings;
   }
}

import java.util.*;

public class RankedQueryParser extends QueryParser {
   private static DocumentProcessing dp = new DocumentProcessing();
   private static double size;
   private static DiskInvertedIndex index;

   public RankedQueryParser(DiskInvertedIndex i, double corpus_size){
      index = i;
      size = corpus_size;
   }

   // Rank the documents returned from a boolean query by score
   public static List<ScoredDocument> rankDocuments(String query){
      //Create a Priority Queue to sort ranked documents
      PriorityQueue<ScoredDocument> docqueue = new PriorityQueue<ScoredDocument>(1, new Comparator<ScoredDocument>() {
         public int compare(ScoredDocument doc1, ScoredDocument doc2) {
            if (doc1.getScore() < doc2.getScore())
            {
               return 1;
            }
            else if (doc1.getScore() > doc2.getScore())
            {
               return -1;
            }
            return 0;
         }
      });

      String[] terms = query.split("\\s+"); //split the query into an array
      //Normalize and Porter Stem each term
      for(int i=0; i<terms.length ; i++){
         terms[i] = dp.normalizeToken(terms[i]);
      }

      // Results is a sublist of the corpus that is a list of documents that contain
      // at least one of the terms in the query.
      List<Integer> results = orQuery(terms);

      for(int docId : results){
         //score each document
         double score = score(docId, terms);
         //add Scored Document to the PQ
         if(score > 0) {
            docqueue.add(new ScoredDocument(docId, score));
         }
      }

      List<ScoredDocument> top10 = new ArrayList<>();
      // remove the top 10 from the priority queue
      for(int i=0;i<10 && !docqueue.isEmpty(); i++){
         top10.add(docqueue.poll());
      }
      return top10;
   }

   //Scores a document
   private static double score(int docId, String[] terms){
      double ad=0;  //accumulator

      for(String term : terms){
         double Wqt =0;
         double Wdt =0;
         PostingResult postings = index.getPostings(term);

         if(postings!=null) {
            // Wqt = (ln(1 + N/(Amount of Documents the term appeared in))
            Wqt = Math.log(1 + (size / postings.size()));

            int[] pos = postings.getPosition(docId);
            if(pos!=null){
               // Tftd = Term frequency in the document
               double tftd = pos.length;
               Wdt = 1 + Math.log(tftd);
            }
         }
         // increment the accumulator value
         ad += Wqt * Wdt;
      }
      //Ld is the weight of the document
      double Ld = index.readWeightFromFile(docId);

      ad /= Ld;
      return ad;
   }

   //retrieves a list of documents that have at least 1 of the terms in the query
   private static List<Integer> orQuery(String[] query){
      List<Integer> postings = new ArrayList<>();     //final merged postings list

      for(String s : query){
         if(index.getPostings(s)!=null)
            postings = orMerge(postings, index.getPostings(s).getDocs());
      }

      return postings;
   }
}

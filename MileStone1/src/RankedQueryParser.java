import java.util.*;

public class RankedQueryParser extends QueryParser {
   private static DocumentProcessing dp = new DocumentProcessing();
   private static DiskInvertedIndex index;

   public RankedQueryParser(DiskInvertedIndex i){
      index = i;
   }

   // Rank the documents returned from a boolean query by score
   public static List<ScoredDocument> rankDocuments(String query, String formula){
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

      //split the query into an array
      String[] terms = query.split("\\s+");
      //Normalize and Porter Stem each term
      for(int i=0; i<terms.length ; i++){
         terms[i] = dp.normalizeToken(terms[i]);
      }

      // Hashmap to keep the saved document scores
      HashMap<Integer, ScoredDocument> accumulatorMap = new HashMap<>();

      // Go Through each term in the query
      for (String term : terms){
         List<Posting> postings = index.getPostings(term);
         List<Integer> postingDocIds = getDocList(postings);

         // Wqt = (ln(1 + N/(Amount of Documents the term appeared in))
         Double Wqt = calcWqt(postings, formula);

         // For each document in posting list
         for(int doc : postingDocIds){
            //Initialize the accumulator value
            double score = 0;

            // Tftd = Term frequency in the document
            int tftd = getPostingTftd(postings, doc);
            double Wdt = calcWdt(tftd, formula);

            score += (Wdt * Wqt);

            if(accumulatorMap.containsKey(doc)){
               score += accumulatorMap.get(doc).getScore();
               // update the score of the doc in the hashmap
               accumulatorMap.get(doc).setScore(score);
            }
            else  //add the new doc to the hashmap
               accumulatorMap.put(doc, new ScoredDocument(doc,score));
         }
      }

      // Divide scores by docWeights and then add to Priority Queue
      for(Integer docId : accumulatorMap.keySet()){
         ScoredDocument doc = accumulatorMap.get(docId);

         //Ld is the weight of the document
         double Ld = index.readWeightFromFile(docId);
         // divide the doc's score by the doc weight
         doc.setScore(doc.getScore()/Ld);
         docqueue.add(doc);
      }

      List<ScoredDocument> top10 = new ArrayList<>();
      // remove the top 10 from the priority queue
      for(int i=0; i<10 && !docqueue.isEmpty(); i++){
         top10.add(docqueue.poll());
      }
      return top10;
   }

   private static double calcWqt(List<Posting> postings, String formula){
      int N = index.getFileNames().size();
      int dft = postings.size();

      if(formula.equals("Default")){
         return Math.log(1 + (N / dft));
      }
      else if(formula.equals("tf-idf")){
         return Math.log(N / dft);
      }
      else if(formula.equals("Okapi BM25")){
         return Math.max(0.1, Math.log( (N-dft+.5) / (dft+.5) ));
      }
      else if(formula.equals("Wacky")){
         return Math.max(0, Math.log((N-dft)/dft));
      }
      return 0;
   }

   private static double calcWdt(int tftd, String formula){
      if(formula.equals("Default")){
         return 1.0 + Math.log(tftd);
      }
      else if(formula.equals("tf-idf")){
         return tftd;
      }
      else if(formula.equals("Okapi BM25")){
         return Math.max(0.1, Math.log( (N-dft+.5) / (dft+.5) ));
      }
      else if(formula.equals("Wacky")){
         return Math.max(0, Math.log((N-dft)/dft));
      }
      return 0;
   }

      // Divide scores by docWeights and then add to Priority Queue
      for(Integer docId : accumulatorMap.keySet()){
         ScoredDocument doc = accumulatorMap.get(docId);

         //Ld is the weight of the document
         double Ld = index.readWeightFromFile(docId);
         // divide the doc's score by the doc weight
         doc.setScore(doc.getScore()/Ld);
         docqueue.add(doc);
      }
   }

   private static void okapiCalc(String[] terms, HashMap<Integer, ScoredDocument> accumulatorMap, PriorityQueue docqueue){

   }

   private static void wackyCalc(String[] terms, HashMap<Integer, ScoredDocument> accumulatorMap, PriorityQueue docqueue){

   }


   // Given a list of postings and a docid,
   // finds the tftd of the docId in the postings of that document.
   private static int getPostingTftd(List<Posting> postings, int id){
      for(Posting p: postings){
         if(p.getDocId() == id)
            return p.getTftd();
      }
      return 0;
   }
}





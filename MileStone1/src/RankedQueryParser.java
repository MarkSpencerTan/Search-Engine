import java.util.*;

public class RankedQueryParser extends QueryParser {
   private static DocumentProcessing dp = new DocumentProcessing();
   private static DiskInvertedIndex index;

   public RankedQueryParser(DiskInvertedIndex i){
      index = i;
   }

   // Rank the documents returned from a boolean query by score
   public static PriorityQueue<ScoredDocument> rankDocuments(String query, String formula){
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
         List<Posting> postings = index.getPostingsNoPosition(term);
         List<Integer> postingDocIds = getDocList(postings);

         // Wqt = (ln(1 + N/(Amount of Documents the term appeared in))
         Double Wqt = calcWqt(postings, formula);
         System.out.println("term: "+term + " Wqt: "+Wqt);

         // Calculate DocLengthA = Average Document Length for the whole corpus
         Double docLengthA = index.readDocLengthA();

         // Create a hashmap to store tftds of each posting
         HashMap<Integer, Integer> tftdmap = getTftdMap(postings);

         // For each document in posting list
         for(int docId : postingDocIds){
            //Initialize the accumulator value
            double score = 0;

            // Tftd = Term frequency in the document
            double tftd = tftdmap.get(docId);
            double Wdt = calcWdt(tftd, docId, docLengthA, formula);

            score += (Wdt * Wqt);

            if(accumulatorMap.containsKey(docId)){
               score += accumulatorMap.get(docId).getScore();
               // update the score of the doc in the hashmap
               accumulatorMap.get(docId).setScore(score);
            }
            else  //add the new doc to the hashmap
               accumulatorMap.put(docId, new ScoredDocument(docId,score));
         }
      }

      // Divide scores by docWeights and then add to Priority Queue
      for(Integer docId : accumulatorMap.keySet()){
         ScoredDocument doc = accumulatorMap.get(docId);
         if(doc.getScore() > 0){
            //Ld is the weight of the document
            double Ld = calcLd(docId, formula);
            // divide the doc's score by the doc weight
            doc.setScore(doc.getScore()/Ld);
         }
         docqueue.add(doc);
      }

      return docqueue;
   }

   private static double calcWqt(List<Posting> postings, String formula){
      double N = index.getFileNames().size();
      double dft = postings.size();

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

   private static double calcWdt(double tftd, int docId, double docLengthA, String formula){
      // A list that contains: docWeightD, docLengthD, ByteSizeD, ave(tftd)
      // values in that order.
      List<Double> weights = index.readWeightFromFile(docId);

      if(formula.equals("Default")){
         return 1.0 + Math.log(tftd);
      }
      else if(formula.equals("tf-idf")){
         return tftd;
      }
      else if(formula.equals("Okapi BM25")){
         double kd = 1.2 * (.25 + (.75 * (weights.get(1)/docLengthA)) );
         return (2.2 * tftd ) / (kd + tftd);
      }
      else if(formula.equals("Wacky")){
         return (1 + Math.log(tftd)) /(1+Math.log(weights.get(3)));
      }
      return 0;
   }

   private static double calcLd(int docId, String formula){
      // A size 4 list that contains: docWeightD, docLengthD, ByteSizeD, ave(tftd)
      // values in that order.
      List<Double> weights = index.readWeightFromFile(docId);

      if(formula.equals("Default") || formula.equals("tf-idf")){
         return weights.get(0);
      }
      else if(formula.equals("Okapi BM25")){
         return 1.0;
      }
      else if(formula.equals("Wacky")){
         return Math.sqrt(weights.get(2));
      }
      return 0;
   }

   // Given a list of postings, creates a map containing the docid as the key
   // and the tftd as the values
    private static HashMap<Integer, Integer> getTftdMap(List<Posting> postings){
      HashMap<Integer, Integer> tftdmap = new HashMap<>();
      for(Posting p: postings){
         tftdmap.put(p.getDocId(), p.getTftd());
      }
      return  tftdmap;
   }
}





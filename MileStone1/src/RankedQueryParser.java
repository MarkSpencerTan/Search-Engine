import java.util.*;

public class RankedQueryParser extends QueryParser {
   private static DocumentProcessing dp = new DocumentProcessing();
   private static double size;
   private static PositionalInvertedIndex index;

   public RankedQueryParser(PositionalInvertedIndex i, double corpus_size){
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
      List<Integer> results = orQuery(query);

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
      double Ld = 0;

      for(String term : terms){
         double Wqt =0;
         List<PositionArray> postings = index.getPostings(term);

         // Wqt = (ln(1 + N/(Amount of Documents the term appeared in))
         if(index.getPostings(term)!=null) {
            Wqt = Math.log(1 + (size / postings.size()));
         }
         // Tftd = Term frequency in the document
         double tftd = 0;
         double Wdt =0;
         if(postings!=null) {
            for (PositionArray pos : postings) {
               if (pos.getDocID() == docId) {
                  tftd = pos.getListofPos().size();
                  Wdt = 1 + Math.log(tftd);
               }
            }
         }
         ad += Wqt * Wdt;
      }
      //set Ld to getWeights method that will read the weight of a document from docWeights.bin file.
      Ld = 1;
      if(Ld != 0) {
         ad /= Ld;
         return ad;
      }
      return 0;
   }

   //retrieves a list of documents that have at least 1 of the terms in the query
   private static List<Integer> orQuery(String query){
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

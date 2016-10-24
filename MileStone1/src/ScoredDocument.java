/**
 * Created by Mark on 10/23/2016.
 */
public class ScoredDocument {
   private int docId;
   private float score;
   public ScoredDocument(int id, float s){
      docId = id;
      score = s;
   }

   public int getId(){
      return docId;
   }

   public float getScore(){
      return score;
   }
}

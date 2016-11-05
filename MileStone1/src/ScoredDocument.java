/**
 * Created by Mark on 10/23/2016.
 */
public class ScoredDocument {
   private int docId;
   private double score;
   public ScoredDocument(int id, double s){
      docId = id;
      score = s;
   }

   public int getId(){
      return docId;
   }

   public double getScore(){
      return score;
   }

   public void setScore(double newScore){
      score = newScore;
   }
}

import java.util.List;

/**
 * Created by Mark on 10/29/2016.
 */
public class Posting {
   private int docId;
   private int tftd;
   private List<Integer> positions;

   public Posting(int d, int t, List<Integer> pos){
      docId = d;
      tftd = t;
      positions = pos;
   }

   // Constructor for Ranked
   public Posting(int d, int t){
      docId = d;
      tftd = t;
   }

   public int getDocId(){
      return docId;
   }

   public int getTftd(){
      return tftd;
   }

   public List<Integer> getPositions(){
      return positions;
   }
}

import java.util.Arrays;

public class AveragePrecision {
   private double relevant = 0;
   private double returned = 0;
   private double acc = 0;

   public AveragePrecision(int[] returnedDoc, int[] relevantDoc){
      for(int i = 0; i < returnedDoc.length; i++){
         returned++;
         if(Arrays.binarySearch(relevantDoc, returnedDoc[i]) > 0){
            relevant++;
            acc = acc + (relevant/returned);
         }

      }
      acc = acc/relevantDoc.length;
   }
   public double getAcc(){
      return acc;
   }
}

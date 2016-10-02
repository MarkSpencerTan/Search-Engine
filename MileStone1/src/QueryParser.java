import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParser {

   PositionalInvertedIndex index;

   public QueryParser(){
      System.out.println("Parser made for Testing Only");
   }

   public QueryParser(PositionalInvertedIndex index){
      this.index = index;
   }

   //Will Return a list of integers corresponding to the document ID's that matched the query.
   public static List<Integer> parseQuery(String query, PositionalInvertedIndex index){
      DocumentProcessing dp = new DocumentProcessing();
      List<Integer> postings = new ArrayList<>(); //final merged postings list
      String[] orsplit = query.split("\\+"); //splits the query by + if there's any

      System.out.println("Parsed 1: "+ Arrays.toString(orsplit));

      //loops through the orsplit list
      for(int i=0; i< orsplit.length; i++){
         String[] andmerge = splitQuotes(orsplit[i]);
         for( int j =0; j<andmerge.length;j++){
            andmerge[j] = dp.normalizeToken(andmerge[j]);
         }

         //will contain document IDs of the current string in andMerge
         List<Integer> ormerge = getDocList(index.getPostings(andmerge[0]));
         // perform an and-merge on the doclist of each string in the list
         for(int j=1; j<andmerge.length; j++){
            ormerge = andMerge(getDocList(index.getPostings(andmerge[j])), ormerge);
         }
         postings = orMerge(ormerge, postings);
         System.out.println(postings);
      }
      return postings;
   }

   // Retrieves an Integer List from the List of Position Array containing ONLY Doc IDs
   private static List<Integer> getDocList(List<PositionArray> posarray){
      List<Integer> doclist = new ArrayList<>();
      if(posarray!=null) {
         for (PositionArray p : posarray) {
            doclist.add(p.getDocID()); //fills ormerge list with doc ids
         }
      }
      return doclist;
   }

   //Splits query phrase into a String[] by surrounding quotes and spaces.
   private static String[] splitQuotes(String s){
      List<String> list = new ArrayList<String>();
      Matcher m = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'").matcher(s);
      while (m.find()) {
         list.add(m.group(0).replace("\"","")); //separates phrases and trims surrounding quotes
      }
      return list.toArray(new String[list.size()]);
   }

   private static List<Integer> andMerge(List<Integer> list1, List<Integer> list2){
      List<Integer> merged = new ArrayList<>();

      //if one list is null, return the other.
      if(list1==null){
         merged.addAll(list2);
      }
      else if(list2==null){
         merged.addAll(list1);
      }
      else{
         int pos1 = 0 , pos2 = 0; //current positions of each list
         while( pos1 < list1.size() && pos2 < list2.size() ){
            if (list1.get(pos1) == list2.get(pos2)) {
               merged.add(list1.get(pos1));
               pos1++;
               pos2++;
            }
            //have to put the first check if pos <= list.size to avoid bounds error.
            else if(list1.get(pos1) < list2.get(pos2)){
               pos1++;
            }
            else {
               pos2++;
            }
         }
      }
      return merged;
   }

   private static List<Integer> orMerge(List<Integer> list1, List<Integer> list2){
      List<Integer> merged = new ArrayList<>();

      //if one list is null, return the other.
      if(list1==null){
         merged.addAll(list2);
      }
      else if(list2==null){
         merged.addAll(list1);
      }
      else {
         int pos1 = 0, pos2 = 0; //current positions of each list
         while (pos1 < list1.size() && pos2 < list2.size()) {
            if (list1.get(pos1) == list2.get(pos2)) {
               merged.add(list1.get(pos1));
               pos1++;
               pos2++;
            } else if (list1.get(pos1) < list2.get(pos2)) {

               merged.add(list1.get(pos1));
               pos1++;
            } else {

               merged.add(list2.get(pos2));
               pos2++;
            }
         }
         // loop ends and lists have the same size. compare positions.
         // add the rest of the one that ended on a smaller position.
         if(list1.size() == list2.size()){
            if (pos1 < pos2) {
               merged.addAll(list1.subList(pos1, list1.size()));
            } else if (pos1 > pos2) {
               merged.addAll(list2.subList(pos2, list2.size()));
            }
         }
         // loop ends and one list is bigger than the other.
         // add the remaining of the longer list.
         else if (list1.size() < list2.size()) {
            merged.addAll(list2.subList(pos2, list2.size()));
         } else if (list1.size() > list2.size()) {
            merged.addAll(list1.subList(pos1, list1.size()));
         }
      }
      return merged;
   }


}

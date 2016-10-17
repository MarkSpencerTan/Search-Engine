import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParser {

   PositionalInvertedIndex index;
   public QueryParser(){};

   public QueryParser(PositionalInvertedIndex index){
      this.index = index;
   }

   //Will Return a list of integers corresponding to the document ID's that matched the query.
   public static List<Integer> parseQuery(String query, PositionalInvertedIndex index){
      DocumentProcessing dp = new DocumentProcessing();
      List<Integer> postings = new ArrayList<>(); //final merged postings list
      String[] orsplit = query.split("\\+"); //splits the query by + if there's any

      //loops through the orsplit list
      for(int i=0; i< orsplit.length; i++){
         String[] andsplit = splitQuotes(orsplit[i]); //splits each element from orsplit on quotes and spaces
         for( int j =0; j<andsplit.length;j++){
            if (andsplit[j].split(" ").length==1)
               andsplit[j] = dp.normalizeToken(andsplit[j]); //only normalize non phrases
         }

         //will contain document IDs of the current string in andsplit
         List<Integer> ormerge;
         if (andsplit[0].split(" ").length==1){
            ormerge = getDocList(index.getPostings(andsplit[0]));
         }
         else
            ormerge =  phraseParser(andsplit[0].split(" "), index);  // phrase detected in andsplit

         // perform an and-merge on the doclist of each string in andsplit
         for(int j=1; j<andsplit.length; j++){
            if (andsplit[j].split(" ").length==1)
               ormerge = andMerge(getDocList(index.getPostings(andsplit[j])), ormerge);
            else  //parse phrase query
               ormerge =  andMerge(phraseParser(andsplit[j].split(" "), index), ormerge);
         }
         //
         postings = orMerge(ormerge, postings);
      }
      return postings;
   }

   //Parses a basic Phrase Query/ Query surrounded by Double Quotes.
   // @params : arr - the Phrase Query split into an array of Strings
   private static List<Integer> phraseParser(String[] arr, PositionalInvertedIndex index){
      DocumentProcessing dp = new DocumentProcessing();
      List<Integer> result = new ArrayList<>();

      arr[0] = dp.normalizeToken(arr[0]);
      List<Integer> commondocs = getDocList(index.getPostings(arr[0]));
      //normalize the phrases and And-Merge all the documents of each word in the phrase
      for(int j=1; j<arr.length; j++){
         arr[j] = dp.normalizeToken(arr[j]);
         commondocs = andMerge(getDocList(index.getPostings(arr[j])), commondocs);
      }
      //loop through common documents
      for(int j=0; j<commondocs.size();j++){
         boolean matched = false;
         //first word
         List<Integer> pos1 = getPositions(index.getPostings(arr[0]), commondocs.get(j));
         //loop through rest of the phrase array
         for(Integer p : pos1){
            int savednum = p+1;
            for(int i=1; i<arr.length;i++){
               List<Integer> pos2 = getPositions(index.getPostings(arr[i]), commondocs.get(j));

               if(pos2.contains(savednum)){
                  if(i==arr.length-1) { //if evaluating last word in the phrase array
                     matched = true;
                  }
                  savednum++;
               }
               else
                  break;
            }
            if(matched)
               break; //if found a match, exit loop. no need to keep going.
         }
         if(matched){
            result.add(commondocs.get(j));
         }
      }
      return result;
   }

   //returns list of integer positions with the doc id as the param
   private static List<Integer>getPositions(List<PositionArray> posarray, int docid){
      List<Integer> list = null;
      for(PositionArray p : posarray){
         if (p.getDocID() == docid)
            return p.getListofPos();
      }
      return list;
   }

   // Retrieves an Integer List from the List of Position Array containing just the Doc IDs
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
            if (list1.get(pos1).equals(list2.get(pos2))) {
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

   public static List<Integer> orMerge(List<Integer> list1, List<Integer> list2){
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
            if (list1.get(pos1).equals(list2.get(pos2))) {
               merged.add(list1.get(pos1));
               pos1++;
               pos2++;
            } else if (list1.get(pos1) < list2.get(pos2)) {
               merged.add(list1.get(pos1));
               pos1++;
            } else if (list1.get(pos1) > list2.get(pos2)){
               merged.add(list2.get(pos2));
               pos2++;
            }
         }
         // loop ends and add the rest of the list that was not fully iterated yet
         if(pos1 == list1.size()){
            merged.addAll(list2.subList(pos2, list2.size()));
         }
         else if (pos2==list2.size()){
            merged.addAll(list1.subList(pos1, list1.size()));
         }
      }
      return merged;
   }


}

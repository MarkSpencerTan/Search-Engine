
import java.util.*;

public class PositionalInvertedIndex {
   private HashMap<String, List<PositionArray>> mIndex;
   private HashMap<String, Integer> weighttable;
   private ArrayList<Double> weightlist = new ArrayList();

   public PositionalInvertedIndex() {
      mIndex = new HashMap<String, List<PositionArray>>();
      weighttable = new HashMap<>();
   }

   public void addTerm(String term, int documentID, int termPosition) {
      boolean found = false;
      // TO-DO: add the term to the index hashtable. If the table does not have
      // an entry for the term, initialize a new ArrayList<Integer>, add the
      // docID to the list, and put it into the map. Otherwise add the docID
      // to the list that already exists in the map, but ONLY IF the list does
      // not already contain the docID.

      if ( !mIndex.containsKey(term) ){
         // create an object of PositionArray
         // insert the docid and the location where the term appear in the doc
         // into the arraylist of position
         PositionArray	tempoj = new PositionArray();
         tempoj.setDocID(documentID);
         tempoj.getListofPos().add(termPosition);

         //ArrayList<Integer> mList = new ArrayList<>();
         ArrayList<PositionArray> mList = new ArrayList<>();
         // add an object that contain a docid and the position
         //mList.add(documentID);
         mList.add(tempoj);
         mIndex.put( term , mList);
      }
      // we have the term already
      else if((mIndex.get(term).get(mIndex.get(term).size()-1).getDocID() == documentID)){

         mIndex.get(term).get(mIndex.get(term).size()-1).getListofPos().add(termPosition);
      }

      // docid is not in the list of object
      else{
         PositionArray tempoj = new PositionArray();
         tempoj.setDocID(documentID);
         tempoj.getListofPos().add(termPosition);
         mIndex.get(term).add(tempoj);
      }
   }

   public List<PositionArray> getPostings(String term) {
      // TO-DO: return the postings list for the given term from the index map.
      return mIndex.get(term);
   }

   public int getTermCount() {
      // TO-DO: return the number of terms in the index.
      return mIndex.size();
   }

   public String[] getDictionary() {
      // TO-DO: fill an array of Strings with all the keys from the hashtable.
      // Sort the array and return it.
      String[] mDictionary = mIndex.keySet().toArray(new String[getTermCount()]);
      Arrays.sort(mDictionary);
      return mDictionary;
   }

   public void addWeight(String term){
      // term is not there add the term and weight of 1
      if ( !weighttable.containsKey(term) ){
         weighttable.put(term, 1);
      }
      // term is there then increase the weight by one
      else{
         weighttable.put(term, weighttable.get(term)+1);
      }
   }

   public double getDocWeight(){
      List<String> keys = new ArrayList<String>();
      keys.addAll(weighttable.keySet());
      double totalweight = 0;
      // weight of term in a doc
      double wdt = 0;
      for(int j = 0; j < keys.size(); j++){
         wdt = (1 + Math.log(weighttable.get(keys.get(j))));
         totalweight = (totalweight + Math.pow(wdt,2));
      }

      return Math.sqrt(totalweight);
   }

   public double getaveTftd(){
      List<String> keys = new ArrayList<String>();
      keys.addAll(weighttable.keySet());
      int avetftd = 0;

      for(int j = 0; j < keys.size(); j++){
         avetftd = avetftd + weighttable.get(keys.get(j));
      }
      if(weighttable.size()>0)
         return avetftd/weighttable.size();

      return avetftd;
   }

   public double getDoclengthD(){
      List<String> keys = new ArrayList<String>();
      keys.addAll(weighttable.keySet());
      int DocLength = 0;

      for(int j = 0; j < keys.size(); j++){
         DocLength = DocLength + weighttable.get(keys.get(j));
      }

      return DocLength;
   }

   public void cleanWeighttable(){
      weighttable.clear();
   }

   public void addWeightToArray(Double weight){
      weightlist.add(weight);
   }

   public ArrayList<Double> getWeightArray(){
      return weightlist;
   }
}

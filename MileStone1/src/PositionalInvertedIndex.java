
import java.util.*;

public class PositionalInvertedIndex {
   private HashMap<String, List<PositionArray>> mIndex;

   public PositionalInvertedIndex() {
      mIndex = new HashMap<String, List<PositionArray>>();
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
}

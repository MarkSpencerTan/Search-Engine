
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BiwordIndex {
	   private HashMap<String, List<Integer>> mIndex;
	   
	   public BiwordIndex() {
	      mIndex = new HashMap<String, List<Integer>>();
	   }
	   
	   public void addTerm(String term, int documentID) {
	      // TO-DO: add the term to the index hashtable. If the table does not have
	      // an entry for the term, initialize a new ArrayList<Integer>, add the 
	      // docID to the list, and put it into the map. Otherwise add the docID
	      // to the list that already exists in the map, but ONLY IF the list does
	      // not already contain the docID.
	      if ( !mIndex.containsKey(term)){
	         ArrayList<Integer> mList = new ArrayList<>();
	         mList.add(documentID);
	         mIndex.put( term , mList);
	      }
	      else if( !mIndex.get(term).contains(documentID) ){
	         mIndex.get(term).add(documentID);   //if it doesn't contain the docID, add it
	      }
	   }
	   
	   public List<Integer> getPostings(String term) {
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

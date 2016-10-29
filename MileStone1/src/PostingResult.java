import java.util.*;


public class PostingResult {
		// Maps Doc I.d.s to an array of positions of the term in the document.
	   private HashMap<Integer, int[]> result;
	   
	   public PostingResult() {
		   result = new HashMap<Integer, int[]>();
	   }
	   
	   public void addTerm(int docId, int[] posList) {
		   result.put(docId, posList);
	   }
	   
	   public HashMap<Integer, int[]> getresult() {
		   return result;
	   }
	   
	   // return the arraylist that contain
	   // all of the position for the input docid
	   public int[] getPosition(int docid) {
		   return result.get(docid);
	   }	   
	   
	   public int getDocIdsCount() {		      
		   return result.size();
	   }	  
	   
	   public int[] getDictionary() {
		   // TO-DO: fill an array of ints with all the keys from the hashtable.
		   // Sort the array and return it.
		   List<Integer> keys = new ArrayList<Integer>();
		   for ( int key : result.keySet() ) {
			   keys.add(key);
		   }
			  
		   int [] myintarr = new int[keys.size()];
		   for(int j = 0; j < myintarr.length; j++){
			   myintarr[j] = keys.get(j);
		   }
		   Arrays.sort(myintarr);
			  
		   return myintarr;
	   }

	   public int size(){
			return result.size();
		}

		// Returns a list of document ids that contains the term
		public List<Integer> getDocs(){
			Set<Integer> docset = result.keySet();
			if(!docset.isEmpty())
				return new ArrayList(result.keySet());
			return Collections.emptyList();
		}
}

import java.util.ArrayList;


public class PositionArray {
	private	int	DocId;
	private ArrayList<Integer> listofPos = new ArrayList<>();
	
	 public int getDocID() {
	      // return doc id
	      return DocId;
	 }
	 public ArrayList<Integer> getListofPos() {
	      // return list of position
	      return listofPos;
	 }
	 public void setDocID(int documentID) {
	      // set new docid
	      DocId = documentID;
	 }
	 public void setListofPos(ArrayList<Integer> listofposition) {
	      // set new list of position
	      listofPos = listofposition;
	 }
}

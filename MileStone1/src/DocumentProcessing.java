import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DocumentProcessing {
	// note all term have to be in lower case and
	// stem using porter steamer before return.
	
	// remove non alphanumeric character from begin and end of the string
	public String RemoveAllNonAlphanumeric(String token){
		token = token.replaceAll("^\\W+|\\W+$", "");
		return token;
	}
	
	// remove all apostropes
	public String RemoveAllApostropes(String token){
		token = token.replace("'", "");
		return token;
	}

	//Normalizes a Hyphenated Term and returns a list because we want to
	//separate the hypenated words and add each part to the index individually.
	public List<String> SplitHyphenWord(String token){
		List<String> retTerm = new ArrayList<>();
		retTerm.addAll( Arrays.asList(token.split("-")) );
		retTerm.add(token.replace("-","")); //adds the combined Hyphenated Word to the list.
		String temp;
		for(int i=0; i<retTerm.size(); i++){
			temp = RemoveAllApostropes(retTerm.get(i));
			temp = RemoveAllNonAlphanumeric(temp);
			temp = temp.toLowerCase();
			temp = PorterStemmer.processToken(temp);
			retTerm.set(i,temp);
			//removes empty strings added when you have --
			if(temp.equals(""))
				retTerm.remove(temp);
		}
		return retTerm;
	}

	//Normalizes a regular token for the QUERY
	public String normalizeToken(String s){
		s = RemoveAllApostropes(s);
		s = RemoveAllNonAlphanumeric(s);
		s = s.toLowerCase();
		s = PorterStemmer.processToken(s);
		s = s.replace("-", ""); //for querying userinput with hyphens
		return s;
	}
	
	//Normalizes INDEXED TOKEN
	public String normalizeIndexedToken(String s){
		s = RemoveAllApostropes(s);
		s = RemoveAllNonAlphanumeric(s);
		s = s.toLowerCase();
		return s;
	}
}

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//THIS OBJECT TAKE IN AN ARTICLE FILE NAME
// AND RETURN THE BODY OF THE ARTICLE
// AS A STRING
public class BodyOutPut {
	
	public static String converttoBodyString (String JsonfileName){
		String retString = "";
        JsonObject jsonObject = new JsonObject();
        
        String Link = "C:\\Users\\SuperAdmin\\Desktop\\fortranproject\\gsonfile\\"+JsonfileName+".json";
        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(new FileReader(Link));
            jsonObject = jsonElement.getAsJsonObject();
            
            retString = jsonObject.get("body").toString();
        
        } catch (FileNotFoundException e) {
           System.out.println("error exeception file not found");
        } 
		return retString;
	}
}

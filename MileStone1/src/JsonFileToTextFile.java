
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonFileToTextFile {
	public static JsonObject convertFileToJSON (String fileName){
		String s;
		Gson gson = new Gson();
        // Read from File to String
        JsonObject jsonObject = new JsonObject();
        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(new FileReader(fileName));
            jsonObject = jsonElement.getAsJsonObject();
            JsonArray jsonarray = jsonObject.getAsJsonArray("documents");
            
	        for(int i = 0; i < jsonarray.size(); i++){
	        	  JsonObject objects = (JsonObject) jsonarray.get(i);
	        	  String title = objects.get("title").toString();
	        	  String body = objects.get("body").toString();
	        	  String url = objects.get("url").toString();
  
	        	  try {  
	        		   //write converted json data to a file named "CountryGSON.json"  
	        		   FileWriter writer = new FileWriter("C:\\Users\\SuperAdmin\\Desktop\\fortranproject\\gsontotext\\all-nps-sites"+(i+1)+".txt");
	        		   writer.write("title "+title + "body "+body+ "url "+url);  
	        		   writer.close();  
	        		    
	        	  } catch (IOException e) {  
	        		   e.printStackTrace();  
	              }  
	        }
        } catch (FileNotFoundException e) {
           System.out.println("file not found");
        } catch (IOException ioe){
        	 System.out.println("2");
        }
        
        return jsonObject;
 }
}

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class BodyOutPut {
   //This converts a JSON file to a string containing only the body
   public static String getBodyString (String JsonfileName){
      String retString = "";
      try {
         JsonParser parser = new JsonParser();
         JsonObject jsonObject =(JsonObject) parser.parse(new FileReader(JsonfileName));
         retString = jsonObject.get("body").getAsString();
      } catch (FileNotFoundException e) {
         System.out.println("error exeception file not found");
      }
      return retString;
   }
}
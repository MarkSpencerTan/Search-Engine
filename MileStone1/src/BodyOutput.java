import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
//THIS OBJECT TAKE IN AN ARTICLE FILE NAME
// AND RETURN THE BODY OF THE ARTICLE
// AS A STRING
public class BodyOutPut {
   public static String getBodyString (String JsonfileName){
      String retString = "";
      JsonObject jsonObject = new JsonObject();

      try {
         JsonParser parser = new JsonParser();
         JsonElement jsonElement = parser.parse(new FileReader(JsonfileName));
         jsonObject = jsonElement.getAsJsonObject();
         retString = jsonObject.get("body").toString();

      } catch (FileNotFoundException e) {
         System.out.println("error exeception file not found");
      }
      return retString;
   }
}
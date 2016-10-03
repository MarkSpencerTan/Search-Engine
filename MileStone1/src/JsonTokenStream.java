import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

/**
 * Created by Mark on 10/3/2016.
 */
public class JsonTokenStream implements TokenStream{
   private Scanner mReader;

   /**
    Constructs a SimpleTokenStream to read from the specified file.
    */
   public JsonTokenStream(File fileToOpen) throws FileNotFoundException {
      mReader = new Scanner(new FileReader(fileToOpen));
   }

   /**
    Constructs a SimpleTokenStream to read from a String of text.
    */
   public JsonTokenStream(String text) {
      mReader = new Scanner(text);
   }

   /**
    Returns true if the stream has tokens remaining.
    */
   @Override
   public boolean hasNextToken() {
      return mReader.hasNext();
   }

   /**
    Returns the next token from the stream, or null if there is no token
    available.
    */
   @Override
   public String nextToken() {
      if (!hasNextToken())
         return null;

      String next = mReader.next();
      return next.length() > 0 ? next :
              hasNextToken() ? nextToken() : null;
   }
}

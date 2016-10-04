import java.util.regex.*;

public class PorterStemmer {

   // a single consonant
   private static final String c = "[^aeiou]";
   // a single vowel
   private static final String v = "[aeiouy]";

   // a sequence of consonants; the second/third/etc consonant cannot be 'y'
   private static final String C = c + "[^aeiouy]*";
   // a sequence of vowels; the second/third/etc cannot be 'y'
   private static final String V = v + "[aeiou]*";

   // this regex pattern tests if the token has measure > 0 [at least one VC].
   private static final Pattern mGr0 = Pattern.compile("^(" + C + ")?" + V + C);

   // add more Pattern variables for the following patterns:
   // m equals 1: token has measure == 1
   private static final Pattern mE1 = Pattern.compile("^(" + C + ")?" + V + C + "(" + V +")?$");
   // m greater than 1: token has measure > 1
   private static final Pattern mGr1 = Pattern.compile("^(" + C + ")?" + V + C + V + C);

   // vowel: token has a vowel after the first (optional) C
   private static final Pattern mVFirst = Pattern.compile("^(" + C + ")?"+V);
   // double consonant: token ends in two consonants that are the same, 
   //			unless they are L, S, or Z. (look up "backreferencing" to help 
   //			with this)
   private static final Pattern mDoubleC = Pattern.compile("(["+c +"&&[^lsz]])\\1$");

   // m equals 1, cvc: token is in Cvc form, where the last c is not w, x,
   //			or y.
   private static final Pattern mCvc = Pattern.compile(c + v + "([" + c + "&&[^wxy]])$");

   public static boolean testRegex(String s){
      return mE1.matcher(s).find();
   }

   private static String[][] step2pairs = {
           new String[] {"ational", "ate"},
           new String[] {"tional", "tion"},
           new String[] {"enci", "ence"},
           new String[] {"anci", "ance"},
           new String[] {"izer","ize"},
           new String[] {"abli", "able"},
           new String[] {"alli", "al"},
           new String[] {"entli", "ent"},
           new String[] {"eli", "e"},
           new String[] {"ousli", "ous"},
           new String[] {"ization", "ize"},
           new String[] {"ation", "ate"},
           new String[] {"ator", "ate"},
           new String[] {"alism", "al"},
           new String[] {"iveness", "ive"},
           new String[] {"fulness", "ful"},
           new String[] {"ousness", "ous"},
           new String[] {"aliti", "al"},
           new String[] {"iviti", "ive"},
           new String[] {"biliti", "ble"}
   };

   private static String[][] step3pairs = {
           new String[] {"icate", "ic"},
           new String[] {"ative", ""},
           new String[] {"alize", "al"},
           new String[] {"iciti", "ic"},
           new String[] {"ical", "ic"},
           new String[] {"ful", ""},
           new String[] {"ness", ""}
   };

   private static String[][] step4pairs = {
           new String[] {"al", ""},
           new String[] {"ance", ""},
           new String[] {"ence", ""},
           new String[] {"er", ""},
           new String[] {"ic", ""},
           new String[] {"able", ""},
           new String[] {"ible", ""},
           new String[] {"ant", ""},
           new String[] {"ement", ""},
           new String[] {"ment", ""},
           new String[] {"ent", ""},
           new String[] {"tion", "t"},
           new String[] {"sion", "s"},
           new String[] {"ou", ""},
           new String[] {"ism", ""},
           new String[] {"ate", ""},
           new String[] {"iti", ""},
           new String[] {"ous", ""},
           new String[] {"ive", ""},
           new String[] {"ize", ""}
   };


   // Helper method to replace suffix with the correct one depending on a pattern p.
   // This will check and loop through the suffix matrix given.
   // Returns the token with the replaced suffix.
   public static String replaceSuffix(String[][] matrix, Pattern p, String t){
      String stem = "";
      for (int i = 0 ; i < matrix.length; i++){
         if (t.endsWith(matrix[i][0])){
            stem = t.substring(0,t.length()-matrix[i][0].length());
            if(p.matcher(stem).find()){
               t = stem + matrix[i][1];
               return t; //skip other suffixes
            }
         }
      }
      return t;
   }
   
   public static String processToken(String token) {
      token = token.toLowerCase();
      if (token.length() < 3) {
         return token; // token must be at least 3 chars
      }
      // step 1a
      if (token.endsWith("sses")||token.endsWith("ies")) {
         token = token.substring(0, token.length() - 2);
      }
      else if (token.endsWith("ss")) {
         //token remains the same
      }
      else if (token.endsWith("s")) {
         token = token.substring(0, token.length() - 1);
      }
      // program the other steps in 1a. 
      // note that Step 1a.3 implies that there is only a single 's' as the 
      //	suffix; ss does not count. you may need a regex pattern here for 
      // "not s followed by s".

      // step 1b
      boolean doStep1bb = false;
      if (token.endsWith("eed")) { // 1b.1
         // token.substring(0, token.length() - 3) is the stem prior to "eed".
         // if that has m>0, then remove the "d".
         String stem = token.substring(0, token.length() - 3);
         if (mGr0.matcher(stem).find()) { // if the pattern matches the stem
            token = stem + "ee";
         }
      }
      // program the rest of 1b. set the boolean doStep1bb to true if Step 1b* 
      // should be performed.
      else if (token.endsWith("ed")) {
         String stem = token.substring(0, token.length() - 2);
         if (mVFirst.matcher(stem).find()) {
            token = stem;
            doStep1bb = true;
         }
      }
      else if (token.endsWith("ing")) {
         String stem = token.substring(0, token.length() - 3);
         if(mVFirst.matcher(stem).find()){
            token = stem;
            doStep1bb = true;
         }
      }

      // step 1b*, only if the 1b.2 or 1b.3 were performed.
      if (doStep1bb) {
         if (token.endsWith("at") || token.endsWith("bl")
          || token.endsWith("iz")) {
            token = token + "e";
         }
         else if(mDoubleC.matcher(token).find()){
            token = token.substring(0, token.length()-1);
         }
         else if(mE1.matcher(token).find() && mCvc.matcher(token).find() ){
            token = token + "e";
         }
      }

      // step 1c
      // program this step. test the suffix of 'y' first, then test the 
      // condition *v* on the stem.
      if (token.endsWith("y")){
         String stem = token.substring(0, token.length()-1);
         if (mVFirst.matcher(stem).find()){
            token = stem + "i";
         }
      }


      // step 2
      // for each suffix in the step2pairs matrix,
      // see if the token ends in the suffix.
      //    * if it does, extract the stem, and do NOT test any other suffix.
      //    * take the stem and make sure it has m > 0.
      //        * if it does, complete the step and do not test any others.
      //          if it does not, attempt the next suffix.
      token = replaceSuffix(step2pairs, mGr0, token);

      // step 3
      // program this step. the rules are identical to step 2 and you can use
      // the same helper method. you may also want a matrix here.
      
      token = replaceSuffix(step3pairs, mGr0, token);

      // step 4
      // program this step similar to step 2/3, except now the stem must have
      // measure > 1.
      // note that ION should only be removed if the suffix is SION or TION, 
      // which would leave the S or T.
      // as before, if one suffix matches, do not try any others even if the 
      // stem does not have measure > 1.
      token = replaceSuffix(step4pairs, mGr1, token);

      // step 5
      // program this step. you have a regex for m=1 and for "Cvc", which
      // you can use to see if m=1 and NOT Cvc.

      // Step 5a
      if(token.endsWith("e")){
         String stem = token.substring(0, token.length()-1);
         if(mGr1.matcher(stem).find() || (mE1.matcher(stem).find() && !mCvc.matcher(stem).find()) ){
            token = stem;
         }
      }
      // Step 5b - remove an l if a token has -ll and m>1
      if(token.endsWith("ll")){
         if(mGr1.matcher(token).find()){
            token = token.substring(0, token.length()-1);
         }
      }
      return token;
   }
}

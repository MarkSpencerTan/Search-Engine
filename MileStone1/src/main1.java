
public class main1 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BodyOutPut run = new BodyOutPut();
		String output;
		output = run.converttoBodyString("article1");
		System.out.println(output);
		
		JsonFileToTextFile converter = new JsonFileToTextFile();
		converter.convertFileToJSON("C:/Users/SuperAdmin/Desktop/fortranproject/429HW2/all-nps-sites.json");
	}

}

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OxfordDictionaryFormater {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Scanner in = new Scanner(new File("oxforddictionary.txt"));
		PrintStream out = new PrintStream(new File("dict.dat"));
		String line = null;
		String word = null;
		String part = null;
		Pattern pat = Pattern.compile("(^[A-Za-z- ]+)[ \t]+([A-Za-z-]+)\\..*");
		Set<String> parts = new HashSet<String>();
		while(in.hasNextLine()) {
			line = in.nextLine();
			Scanner l = new Scanner(line);
			if (l.hasNext()) {
				word = l.next();
			}
			Matcher m = pat.matcher(line);
			if (m.matches()) {
				String mone = m.group(1);
				String mtwo = m.group(2);
				if (!mone.startsWith("Usage")) {
					out.println(mone + "\t" + mtwo);
				}
			}
			l.close();
		}
		in.close();
		out.close();
		System.out.println();
		System.out.println("Convertion Complete.");
	}

}

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * A quick test program for running markov chains using file inputs
 */
public class BookerDewitt {
	
	public static Markov mk = null;
	private static boolean SAVE = true;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			mk = new Markov("null.dat");
		} catch (IOException e) {
			System.out.println("Failed to create markov object.");
		}
		intro();
		Scanner c = new Scanner(System.in);
		boolean cont = true;
		while(cont) {
			System.out.println("analyze, write, or quit");
			String[] n = c.nextLine().toLowerCase().trim().split(" ");
			if (n.length > 0) {
				if (n[0].equals("quit")) {
					cont = false;
				} else if (n[0].equals("write")) {
					if (n.length == 3) {
						write(n[1], Integer.parseInt(n[2]));
					} else {
						System.out.println("write FILENAME SENTENCES");
					}
				} else if (n[0].equals("analyze")) {
					if (n.length == 2) {
						analyze(n[1]);
					} else {
						System.out.println("analyze FILENAME");
					}
				} else {
					System.out.println("I'm sorry, I don't recognize that command.");
				}
			}
		}
		c.close();
		if (SAVE) {
			mk.writeFile();
		}
	}
	
	public static void intro() {
		System.out.println("Hello and welcome!");
		System.out.println("Tell me to \"analyze\" a file, or \"write\" a file"
				+ " with however many sentences and I'll do it!");
	}
	
	public static void write(String file, int num) {
		try {
			File f = new File(file);
			if (!f.exists()) {
				f.createNewFile();
			}
			PrintStream s = new PrintStream(f);
			for(int i = 0; i < num; i++) {
				s.println(mk.randomSentence());
			}
			s.close();
		} catch (IOException e) {
			System.out.println("IO Failure.");
		}
	}
	
	public static void analyze(String file) {
		try {
			Scanner f = new Scanner(new File(file));
			mk.AnalyzeText(f);
			f.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to find file.");
		}
	}

}

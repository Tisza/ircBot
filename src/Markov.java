import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class Markov implements Comparable<Markov> {
	private File fd;
	private Map<String, Set<Pair<String, Integer>>> chain;
	private static String DELIMIT = ":::";
	private static boolean DEBUG = false;
	
	public Markov(String fileName) throws IOException {
		fd = new File(fileName);
		chain = new HashMap<String, Set<Pair<String, Integer>>>();
		if (fd.exists() && fd.isFile()) {
			if (DEBUG) System.out.println("Reading Markov File...");
			try {
				Scanner s = new Scanner(fd);
				while(s.hasNextLine()) {
					String l = s.nextLine().trim();
					String[] parts = l.split("\t");
					if (parts.length > 1) {
						if (!chain.containsKey(parts[0]))
							chain.put(parts[0], new HashSet<Pair<String, Integer>>());
						if (DEBUG) System.out.println("adding " + parts[0]);
						Set<Pair<String, Integer>> h = chain.get(parts[0]);
						for(int i = 1; i < parts.length; i++) {
							String[] args = parts[i].split(DELIMIT);
							if (args.length == 2) {
								Pair<String, Integer> p = new Pair<String, Integer>
								(args[0], Integer.parseInt(args[1]));
								h.add(p);
								if (DEBUG) System.out.println("\t -> " + args[0]);
							}
						}
					}
				}
				s.close();
				if (DEBUG) System.out.println("Markov chain built.");
			} catch (FileNotFoundException e) {
				System.out.println("No File Found.");
			}
		}
		if (!fd.exists()) {
			fd.createNewFile();
		}
		if (!fd.canRead() || !fd.canWrite() || !fd.isFile()) {
			throw new IOException("Cannot use markov file provided.");
		}
	}
	
	/**
	 * Adds to the markov chain
	 * @param from the first word in the association
	 * @param to the second word in the association
	 */
	public void countWordAssoc(String from, String to) {
		if (chain.containsKey(from)) {
			Set<Pair<String, Integer>> s = chain.get(from);
			boolean found = false;
			for(Pair<String, Integer> p : s) {
				if (p.k.equals(to)) {
					found = true;
					p.v = p.v + 1;
				}
			}
			if (!found) {
				Pair<String, Integer> p = new Pair<String, Integer>(to, 1);
				s.add(p);
			}
		} else {
			Set<Pair<String, Integer>> s = new HashSet<Pair<String, Integer>>();
			s.add(new Pair<String, Integer>(to, 1));
			chain.put(from, s);
		}
	}
	/**
	 * creates a sentence.
	 * @param start the first word to start at
	 * @return the sentence
	 */
	public String createSentence(String start) {
		if (start.equals(". ")) {
			return "";
		} else if (chain.containsKey(start)) {
			Set<Pair<String, Integer>> s = chain.get(start);
			if (start.equals("^ ")) {
				start = "";
			}
			int total = 0;
			for(Pair<String, Integer> p : s) {
				total += p.v;
			}
			int rand = (int) Math.round(Math.random() * total);
			Pair<String, Integer> last = null;
			for(Pair<String, Integer> p : s) {
				rand -= p.v;
				if (rand < 0) {
					return start + " " + createSentence(p.k);
				}
				last = p;
			}
			return start + " " + createSentence(last.k);
		} else {
			if (!start.equals("^ "))
				return start;
			return "";
		}
	}
	
	/**
	 * Constructs a random sentence...
	 * @return a random sentence
	 */
	public String randomSentence() {
		return createSentence("^ ");
	}
	
	/**
	 * analyzes the input in and constructs a markov chain based off it.
	 * @param in the scanner input to use.
	 */
	public void AnalyzeText(Scanner in) {
		while(in.hasNextLine()) {
			Scanner l = new Scanner(in.nextLine());
			if (l.hasNext()) {
				String last = "^ ";
				String next;
				while(l.hasNext()) {
					next = l.next();
					countWordAssoc(last, next);
					last = next;
				}
				countWordAssoc(last, ". ");
			}
			l.close();
		}
	}
	
	/**
	 * writes the chain to file.
	 * @return true on success, false on failure.
	 */
	public boolean writeFile() {
		try {
			PrintStream s = new PrintStream(fd);
			for(String k : chain.keySet()) {
				s.print(k + "\t");
				for(Pair<String, Integer> p : chain.get(k)) {
					s.print(p.k + DELIMIT + p.v.toString() + "\t");
				}
				s.println();
			}
			s.flush();
			s.close();
		} catch (FileNotFoundException e) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Markov o) {
		return chain.size() - o.chain.size();
	}
}

/*
* consider making a markov chain that reverse looks up words to part of speech (noun, etc)
* and have it study word patterns, then construct by selecting from part of speech...
*/
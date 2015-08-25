import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class ImprovedMarkov implements Comparable<ImprovedMarkov> {
	private Map<String, String> wordToPart;
	private Map<String, Set<String>> partToWord;
	private Set<String> s;
	private File fd;
	
	/**
	 * reads and loads in the dictionary file, opens or creates the save file 
	 * @param saveFile the save file for the markov chain for sentence recall
	 * @param dictionary the dictionary file full of all words and parts of speech
	 * @throws IOException if the dictionary file does not exist or it cannot create
	 * 	the markov save file.
	 */
	public ImprovedMarkov(String saveFile, String dictionary) throws IOException {
		fd = new File(saveFile);
		if (!fd.exists()) {
			fd.createNewFile();
		}
		Scanner in = new Scanner(new File(dictionary));
		wordToPart = new HashMap<String, String>();
		partToWord = new HashMap<String, Set<String>>();
		String l;
		while(in.hasNextLine()) {
			l = in.nextLine().toLowerCase();
			String[] part = l.split("\t");
			if (part.length > 1 && !wordToPart.containsKey(part[0])) {
				wordToPart.put(part[0], part[1]);
				if (!partToWord.containsKey(part[1])) 
					partToWord.put(part[1], new HashSet<String>());
				Set<String> st = partToWord.get(part[1]);
				st.add(part[0]);
			}
		}
		in.close();
	}
	
	public String generate() {
		String form = getRandom(s);
		String rtn = "";
		Scanner l = new Scanner(form);
		while(l.hasNext()) {
			rtn += getRandom(partToWord.get(l.next())) + (l.hasNext() ? " " : ".");
		}
		l.close();
		return rtn;
	}
	
	/**
	 * Analysis in for sentence structures and saves them
	 * @param in the input to use for analysis
	 * @param seemless if set to true, will continue a sentence after newlines
	 * 	otherwise, a newline ends the sentence
	 */
	public void analyse(Scanner in, boolean seemless) {
		String sent = "";
		String i;
		String p;
		Scanner l;
		Scanner input = new Scanner(System.in);
		while(in.hasNextLine()) {
			l = new Scanner(in.nextLine().toLowerCase());
			while (l.hasNext()) {
				i = l.next();
				p = wordToPart.get(i);
				if (p == null) {
					System.out.println("Unknown Part of Speech: " + i);
					for(String a : partToWord.keySet()) {
						System.out.print(a + " ");
					}
					System.out.println();
					System.out.print("Enter part of speech: ");
					String n = input.next();
					if (!partToWord.containsKey(n)) {
						partToWord.put(n, new HashSet<String>());
					}
					wordToPart.put(i, n);
					Set<String> st = partToWord.get(n);
					st.add(i);
					sent += n + " ";
				} else {
					sent += p + " ";
				}
				if (i.endsWith(".")) {
					s.add(sent);
					System.out.println(sent);
					sent = "";
				}
			}
			if (!seemless && !sent.equals("")) {
				s.add(sent);
				System.out.println(sent);
				sent = "";
			}
			l.close();
		}
		input.close();
	}
	
	private String getRandom(Set<String> s) {
		int r = (int) Math.round(s.size() * Math.random());
		int i = 0;
		for(String l : s) {
			if (i == r) {
				return l;
			}
			i++;
		}
		return "";
	}

	@Override
	public int compareTo(ImprovedMarkov o) {
		return wordToPart.size() - o.wordToPart.size();
	}

}

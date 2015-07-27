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
	private Map<String, Set<Pair<String, Integer>>> m;
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
			l = in.next();
			String[] part = l.split("\t");
			if (part.length > 0 && wordToPart.containsKey(part[0])) {
				wordToPart.put(part[0], part[1]);
				if (!partToWord.containsKey(part[1])) 
					partToWord.put(part[1], new HashSet<String>());
				Set<String> s = partToWord.get(part[1]);
				s.add(part[0]);
			}
		}
		in.close();
	}

	@Override
	public int compareTo(ImprovedMarkov o) {
		return wordToPart.size() - o.wordToPart.size();
	}

}

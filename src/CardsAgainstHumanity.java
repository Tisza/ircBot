import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.json.*;

import org.jibble.pircbot.PircBot;

public class CardsAgainstHumanity {
	private Set<Card> whitediscard;
	private Set<Card> blackdiscard;
	private PriorityQueue<Card> whitedraw;
	private PriorityQueue<Card> blackdraw;
	private Map<String, Card[]> hands;
	private Card black;
	private Map<String, List<Card>> table;
	private Map<String, Integer> score;
	private int decksize;
	private List<String> order;
	private String czar;
	private List<String> publicorder;
	private PircBot bot;
	private String cahChannel;
	private Queue<String> playerQueue;
	private Set<Card> blanks;
	private Map<String, Integer> usingABlank;
	
	/**
	 * creates a new empty game of cards against humanity from the json file at in
	 * @param in json file to read from
	 */
	public CardsAgainstHumanity(InputStream in, PircBot bot) {
		whitediscard = new HashSet<Card>();
		blackdiscard = new HashSet<Card>();
		whitedraw = new PriorityQueue<Card>();
		blackdraw = new PriorityQueue<Card>();
		hands = new HashMap<String, Card[]>();
		table = new HashMap<String, List<Card>>();
		score = new HashMap<String, Integer>();
		order = new ArrayList<String>();
		playerQueue = new LinkedList<String>();
		this.bot = bot;
		blanks = new HashSet<Card>();
		usingABlank = new HashMap<String, Integer>();
		JsonReader rdr = Json.createReader(in);
		JsonObject j = rdr.readObject();
		rdr.close();
		if (j.containsKey("masterCards")) {
			JsonValue v = j.get("masterCards");
			if (v.getValueType() == JsonValue.ValueType.ARRAY) {
				decksize = ((JsonArray) v).size();
				for(JsonValue i : (JsonArray) v) {
					if (i.getValueType() == JsonValue.ValueType.OBJECT) {
						String text = ((JsonString) ((JsonObject) i).get("text")).getString();
						int id = ((JsonNumber) ((JsonObject) i).get("id")).intValue();
						boolean black = (((JsonString) ((JsonObject) i).get("cardType")).getString().equals("A")? false: true);
						int numAnswer = ((JsonNumber) ((JsonObject) i).get("numAnswers")).intValue();
						String expansion = ((JsonString) ((JsonObject) i).get("expansion")).getString();
						Card c = new Card(id, decksize, black, text, numAnswer, expansion);
						if (black) {
							blackdraw.add(c);
						} else {
							whitedraw.add(c);
						}
					} else {
						throw new IllegalArgumentException("\"masterCards\" array must contain card objects.");
					}
				}
			} else {
				throw new IllegalArgumentException("\"masterCards\" must be an array type.");
			}
		} else {
			throw new IllegalArgumentException("No \"masterCards\" parameter");
		}
	}
	
	/**
	 * adds a player to the current game of CAH
	 * @param player the players name (must be unique)
	 * @throws IllegalArgumentException if the player is already in game, or if there are not enough cards.
	 */
	public void addPlayer(String player) {
		if (hands.containsKey(player)) {
			throw new IllegalArgumentException("player already in game.");
		}
		if (!allIn() || hands.size() == 0) {
			hands.put(player, new Card[10]);
			
			addCards(player);
			
			order.add(player);
			score.put(player, 0);
			table.put(player, new ArrayList<Card>());
			messageHand(player);
			System.out.println("\t \t added player."); // DEBUG
		} else {
			System.out.println("\t \t added to player queue."); // DEBUG
			playerQueue.add(player);
		}
	}
	
	/**
	 * discards the player's cards and removes them from the match
	 * @param player the player to remove
	 * @throws IllegalArgumentException if player is not in game
	 */
	public void removePlayer(String player) {
		if (!hands.containsKey(player)) {
			throw new IllegalArgumentException("player not in game.");
		}
		Card[] a = hands.get(player);
		for(int i = 0; i < 10; i++) {
			whitediscard.add(a[i]);
		}
		whitediscard.addAll(table.get(player));
		
		hands.remove(player);
		order.remove(player);
		score.remove(player);
		table.remove(player);
		System.out.println("\t \t removed player."); // DEBUG
		
		if (hands.size() < 3 && black != null) {
			System.out.println("\t \t \t not enough players"); // DEBUG
			messageAll("Cards Against Humanity Game ended (not enough players)");
		} else if (czar.equals(player)) {
			System.out.println("\t \t \t redo round"); // DEBUG
			messageAll("Czar left. Redoing round...");
			returnCards();
			drawBlack();
		} else if (allIn()) {
			System.out.println("\t \t \t everyone is now in"); // DEBUG
			displayCards();
		}
	}
	
	/**
	 * Attempts to draw a random black card, will re-use the deck if necessary
	 */
	private void drawBlack() {
		if (black != null) {
			blackdiscard.add(black);
			black = null;
		}
		if (blackdraw.size() == 0) {
			for(Card c : blackdiscard) {
				c.shuffle(decksize);
				blackdraw.add(c);
			}
			blackdiscard.clear();
		}
		if (blackdraw.size() == 0) {
			throw new IllegalArgumentException("No black cards avaliable.");
		}
		String p = order.remove(0);
		order.add(p);
		czar = p;
		System.out.println("\t \t " + p + " is the czar."); // DEBUG
		
		black = blackdraw.remove();
		
		while(!playerQueue.isEmpty()) {
			System.out.println("\t \t getting player from queue..."); // DEBUG
			addPlayer(playerQueue.remove());
		}
		
		dealCards();
		announceRound();
	}
	
	/** 
	 * returns a given player's hand of white cards
	 * @param player the player to inquire abouts
	 * @return the id of their cards, and the text on the cards
	 */
	public Map<Integer, String> hand(String player) {
		if (!hands.containsKey(player)) {
			throw new IllegalArgumentException("player not found!");
		}
		Map<Integer, String> m = new HashMap<Integer, String>();
		for(Card c : hands.get(player)) {
			m.put(c.id, c.text);
		}
		return m;
	}
	
	/**
	 * attempts to play a card for a player
	 * @param player the player to play for
	 * @param id the card's id
	 * @return true if successful, false otherwise
	 */
	private boolean playCard(String player, int id) {
		System.out.println("\t \t " + player + " is placing a card"); // DEBUG
		if (!hands.containsKey(player)) {
			throw new IllegalArgumentException("player not found!");
		}
		if (table.get(player).size() == black.numAnswers) {
			System.out.println("\t \t \t all cards played"); // DEBUG
			return false;
		}
		Card[] a = hands.get(player);
		if (id < 10) {
			Card c = a[id];
			a[id] = null;
			List<Card> l = table.get(player);
			l.add(c);
			System.out.println("\t \t \t played card"); // DEBUG
			return true;
		}
		System.out.println("\t \t \t card not found"); // DEBUG
		return false;
	}
	
	/**
	 * returns all the cards on the table back to their respective players
	 */
	public void returnCards() {
		System.out.println("\t \t returning cards"); // DEBUG
		for(String s : table.keySet()) {
			System.out.println("\t \t \t returning to " + s); // DEBUG
			Iterator<Card> i = table.get(s).iterator();
			while(i.hasNext()) {
				Card c = i.next();
				i.remove();
				Card[] a = hands.get(s);
				boolean placed = false;
				for(int j = 0; j < 10; j++) {
					if (a[j] == null) {
						a[j] = c;
						placed = true;
						break;
					}
				}
				if (!placed) {
					System.out.println("\t \t \t \t !!!! ERROR. COULD NOT RE-PLACE CARD.");
					whitediscard.add(c);
				}
			}
		}
	}
	
	/**
	 * checks if every player has all their cards in
	 * @return true is all are in, false otherwise
	 */
	public boolean allIn() {
		System.out.println("\t \t all in?"); // DEBUG
		boolean allIn = true;
		for(String p : table.keySet()) {
			if (p != czar) {
				if (black == null || table.get(p).size() < black.numAnswers) {
					allIn = false;
				}	
			}
		}
		System.out.println("\t \t \t " + (allIn ? "yes" : "no")); // DEBUG
		return allIn;
	}
	
	/**
	 * creates the random public order to view the table
	 */
	private void table() {
		String[] order = new String[table.size() - 1];
		for(int i = 0; i < order.length; i++) {
			order[i] = "";
		}
		for(String p : table.keySet()) {
			if (!p.equals(czar)) {
				int r;
				do {
					r = (int) Math.floor(Math.random() * order.length);
				} while(!order[r].equals(""));
				order[r] = p;
				System.out.println("\t \t \t ordering " + p); // DEBUG
			}
		}
		System.out.println("\t \t display order:"); // DEBUG
		List<String> listorder = new ArrayList<String>(order.length);
		for(String p : order) {
			System.out.println("\t \t \t " + p); // DEBUG
			listorder.add(p);
		}
		
		publicorder = listorder;
	}
	
	/**
	 * gives player a winning point for the round
	 * @param id the 0 index'd selection from the Order in table()
	 * @return the winner's name
	 */
	private String winner(int id) {
		System.out.println("\t \t choosing winner..."); // DEBUG
		int index = id;
		String winner = "";
		for(String ply : publicorder) {
			if (index == 0) {
				winner = ply;
				break;
			}
			index--;
		}
		System.out.println("\t \t \t identified as " + winner); // DEBUG
		if (!hands.containsKey(winner)) {
			throw new IllegalArgumentException("player not found!");
		}
		score.put(winner, score.get(winner) + 1);
		blackdiscard.add(black);
		black = null;
		for(List<Card> l : table.values()) {
			System.out.println("\t \t \t discarding a player's cards..."); // DEBUG
			whitediscard.addAll(l);
			l.clear();
		}
		
		return winner;
	}
	
	/**
	 * adds cards to all players until they have 10 cards
	 */
	public void dealCards() {
		System.out.println("\t \t dealing cards..."); // DEBUG
		for(String p : hands.keySet()) {
			addCards(p);
		}
	}
	
	/**
	 * adds a card to the player's hand
	 * @param player the player to add to it
	 */
	private void addCards(String player) {
		if (whitedraw.size() == 0) {
			System.out.println("\t \t \t reshuffling discards"); // DEBUG
			for(Card c : whitediscard) {
				c.shuffle(decksize);
				whitedraw.add(c);
			}
			Set<Card> s = new HashSet<Card>(); // makes new blank cards
			for(Card c : blanks) {
				Card d = new Card(c.id, decksize, c.black, "___", 0, "Base");
				d.shuffle(decksize);
				s.add(d);
				whitedraw.add(d);
			}
			whitedraw.removeAll(blanks);
			blanks = s;
			whitediscard.clear();
		}
		if (whitedraw.size() == 0) {
			throw new IllegalArgumentException("Not enough cards.");
		}
		Card[] a = hands.get(player);
		for(int i = 0; i < 10; i++) {
			if (a[i] == null) {
				a[i] = whitedraw.remove();
				System.out.println("\t \t \t gave " + player + " a card"); // DEBUG
			}
		}
	}
	
	/**
	 * returns the czar this round
	 * @return the czar's name
	 */
	public String czar() {
		return czar;
	}
	
	/**
	 * returns the current list of players.
	 * @return
	 */
	public Set<String> playerlist() {
		return hands.keySet();
	}
	
	public Map<String, Integer> scores() {
		return score;
	}
	
	public boolean onMessage(String channel, String sender, String message) {
		return interact(channel, sender, message);
	}
	
	public boolean onPrivateMessage(String sender, String message) {
		if (usingABlank.keySet().contains(sender)) { // allow for text on blanks
			Card c = hands.get(sender)[usingABlank.get(sender)];
			Card d = new Card(c.id, decksize, false, message, 0, "Base");
			blanks.remove(c);
			blanks.add(d); // track them to replace next round
			hands.get(sender)[usingABlank.get(sender)] = d;
			if (playCard(sender, usingABlank.get(sender))) {
				bot.sendMessage(sender, "played blank card.");
			} else {
				blanks.remove(d);
				blanks.add(c);
				hands.get(sender)[usingABlank.get(sender)] = c;
				bot.sendMessage(sender, "You cannot play the card.");
			}
			usingABlank.remove(sender);
			return true;
		} else {
			return interact(sender, sender, message);
		}
	}
	
	private boolean interact(String channel, String sender, String message) {
		if (!command(channel, sender, message)) {
			String[] m = message.toLowerCase().split(" ");
			
			if (m[0].equals("select") && hands.containsKey(sender)) {
				if (!sender.equals(czar)) {
					try {
						if (blanks.contains(hands.get(sender)[Integer.parseInt(m[1])])) {
							usingABlank.put(sender, Integer.parseInt(m[1]));
							bot.sendMessage(sender, "Enter text for your blank card:");
						} else if (playCard(sender, Integer.parseInt(m[1]))) {
							bot.sendMessage(sender, "played " + m[1]);
							if (allIn()) {
								displayCards();
							}
						} else {
							bot.sendMessage(sender, "You cannot play the card.");
						}
					} catch (IllegalArgumentException e) {
						System.out.println(sender + " tried to play a card but is " +
								"not playing");
					}
					return true;
				} else { // czar is choosing...
					try {
						String w = winner(Integer.parseInt(m[1]));
						messageAll(w + " has won the round.");
						scoreAll();
						drawBlack(); // new round
					} catch (IllegalArgumentException e) {
						bot.sendMessage(czar, "Please choose a valid id.");
					}
				}
			} else {
				return false;
			}
		}
		return true;
	}
	
	private boolean command(String channel, String sender, String message) {
		String[] m = message.toLowerCase().split(" ");
		if (m[0].startsWith("!")) {
			m[0] = m[0].substring(1);
			if (m[0].equals("pass") && hands.size() > 2) {
				messageAll("Round passed.");
				returnCards();
				drawBlack();
			} else if (m[0].equals("join")) {
				addPlayer(sender);
				messageAll(sender + " has joined the game.");
			} else if (m[0].equals("leave")) {
				removePlayer(sender);
				messageAll(sender + " has left the game.");
			} else if (m[0].equals("cahchannel")) {
				if (m.length > 1) {
					cahChannel = m[1];
				} else {
					cahChannel = channel;
				}
				bot.sendMessage(channel, "Cards against Humanity will now output to " + cahChannel);
			} else if (m[0].equals("start")) {
				if (hands.size() < 3) {
					bot.sendMessage(channel, "Not enough players.");
				} else {
					drawBlack();
				}
			} else if (m[0].equals("hand")) {
				messageHand(sender);
			} else if (m[0].equals("numblanks") && m.length > 1) {
				if (allIn()) {
					bot.sendMessage(channel, "Round currently in progress. Please wait.");
				} else {
					int numBlanks = Math.max(Integer.parseInt(m[1]), 0);
					resetBlanks(numBlanks);
					messageAll(numBlanks + " blank cards have been set.");
				}
			}
			return true;
		}
		return false;
	}
	
	private void announceRound() {
		System.out.println("\t \t announcing round"); // DEBUG
		String prompt = "0,1" + black.text + "";
		if (cahChannel != null) {
			bot.sendMessage(cahChannel, prompt);
			bot.sendMessage(cahChannel, czar + " is the czar.");
		}
		for(String p : hands.keySet()) {
			bot.sendMessage(p, prompt);
			bot.sendMessage(p, czar + " is the czar.");
		}
		for(String p : hands.keySet()) {
			if (!p.equals(czar) && black.numAnswers > 1) {
				bot.sendMessage(p, "Choose cards by id number. Cards" +
						" with multiple answers are filled in order of blanks.");
			}
			messageHand(p);
			if (!p.equals(czar)) {
				bot.sendMessage(p, "To play a card, message \"select #\" where " +
						"# is the id of the card");
			}
		}
	}
	
	private void messageHand(String p) {
		if (p.equals(czar)) {
			bot.sendMessage(p, "You are the czar. Wait for player's" +
				" to choose their cards.");
		} else {
			Card[] a = hands.get(p);
			for(int i = 0; i < 10; i++) {
				bot.sendMessage(p, "["+ i + "]1,0 " + a[i].text + "");
			}
		}
	}
	
	private void displayCards() {
		System.out.println("\t \t Displaying table..."); // DEBUG
		String prompt = "0,1" + black.text + "";
		table();
		
		if (cahChannel != null) {
			bot.sendMessage(cahChannel, prompt);
		}
		for(String ply : hands.keySet()) {
			bot.sendMessage(ply, prompt);
		}
		for(int i = 0; i < publicorder.size(); i++) {
			String p = publicorder.remove(0);
			publicorder.add(p);
			String msg = "[" + i + "] ";
			List<Card> l = table.get(p);
			for (int j = 0; j < l.size(); j++) {
				Card c = l.remove(0);
				l.add(c);
				msg += "1,0" + c.text + ", ";
			}
			msg = msg.substring(0, msg.length() - 2);
			if (cahChannel != null) {
				bot.sendMessage(cahChannel, msg);
			}
			for(String ply : hands.keySet()) {
				bot.sendMessage(ply, msg);
			}
		}
		bot.sendMessage(czar, "Please choose the winning player by messaging " +
				"\"select #\" where # is the id");
	}
	
	private void messageAll(String prompt) {
		if (cahChannel != null) 
			bot.sendMessage(cahChannel, prompt);
		for(String p : hands.keySet()) {
			bot.sendMessage(p, prompt);
		}
	}
	
	private void scoreAll() {
		String msg = "Current Score: ";
		for(String p : hands.keySet()) {
			msg += p + " " + score.get(p) + ", ";
		}
		msg = msg.substring(0, msg.length() - 2);
		messageAll(msg);
	}
	
	private void resetBlanks(int numBlanks) {
		if (!allIn()) {
			whitedraw.removeAll(blanks);
			whitediscard.removeAll(blanks);
			for(String p : hands.keySet()) {
				Card[] a = hands.get(p);
				for(int i = 0; i < 10; i++) {
					if (blanks.contains(a[i])) {
						a[i] = null;
					}
				}
			}
			for(List<Card> l : table.values()) {
				l.removeAll(blanks);
			}
			blanks.clear();
			for(int i = 0; i < numBlanks; i++) {
				Card c = new Card(-(i + 1), decksize, false, "___", 0, "Base");
				blanks.add(c);
				c.shuffle(decksize);
				whitedraw.add(c);
			}
			dealCards();
		}
	}
}
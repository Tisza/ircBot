import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.json.*;

public class CardsAgainstHumanity {
	private Set<Card> whitediscard;
	private Set<Card> blackdiscard;
	private PriorityQueue<Card> whitedraw;
	private PriorityQueue<Card> blackdraw;
	private Map<String, Set<Card>> hands;
	private Card black;
	private Map<String, List<Card>> table;
	private Map<String, Integer> score;
	private int decksize;
	private List<String> order;
	private String czar;
	
	/**
	 * creates a new empty game of cards against humanity from the json file at in
	 * @param in json file to read from
	 */
	public CardsAgainstHumanity(InputStream in ) {
		whitedraw = new PriorityQueue<Card>();
		blackdraw = new PriorityQueue<Card>();
		order = new ArrayList<String>();
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
		Set<Card> h = new HashSet<Card>();
		if (whitedraw.size() < 10) {
			for(Card c : whitediscard) {
				c.shuffle(decksize);
				whitedraw.add(c);
			}
			whitediscard.clear();
		}
		if (whitedraw.size() < 10) {
			throw new IllegalArgumentException("Not enough cards.");
		}
		for(int i = 0; i < 10; i++) {
			h.add(whitedraw.remove());
		}
		hands.put(player, h);
		order.add(player);
		score.put(player, 0);
		table.put(player, new ArrayList<Card>());
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
		whitediscard.addAll(hands.get(player));
		whitediscard.addAll(table.get(player));
		
		hands.remove(player);
		order.remove(player);
		score.remove(player);
		table.remove(player);
		
		if (czar.equals(player)) {
			returnCards();
			drawBlack();
		}
	}
	
	/**
	 * Attempts to draw a random black card, will re-use the deck if necessary
	 * @return the player who is the czar
	 */
	public String drawBlack() {
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
		
		black = blackdraw.remove();
		return czar;
	}
	
	/**
	 * returns the current black card, or null if there is no black card
	 * @return the current black card
	 */
	public Card black() {
		if (black != null)
			return new Card(black.id, 0, true, black.text, black.numAnswers, black.expansion);
		return null;
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
	public boolean playCard(String player, int id) {
		if (!hands.containsKey(player)) {
			throw new IllegalArgumentException("player not found!");
		}
		Set<Card> s = hands.get(player);
		Iterator<Card> i = s.iterator();
		while(i.hasNext()) {
			Card c = i.next();
			if (c.id == id) {
				i.remove();
				List<Card> l = table.get(player);
				l.add(c);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * returns all the cards on the table back to their respective players
	 */
	public void returnCards() {
		for(String s : table.keySet()) {
			Iterator<Card> i = table.get(s).iterator();
			while(i.hasNext()) {
				Card c = i.next();
				i.remove();
				hands.get(s).add(c);
			}
		}
	}
}

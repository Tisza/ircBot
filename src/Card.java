
public class Card implements Comparable<Card>{
	public final int id;
	private int index;
	public final String text;
	public final boolean black;
	public final int numAnswers;
	public final String expansion;
	
	/**
	 * creates a new card object
	 * @param id a helpful id number per card!
	 * @param maxDeck the deck size for shuffling (used in priority queues)
	 * @param black is it a black card?
	 * @param text the text found on this card
	 * @param numAnswers the number of answer cards required
	 * @param expansion which expansion is this card from
	 */
	public Card(int id, int maxDeck, boolean black, String text, int numAnswers, String expansion) {
		this.id = id;
		this.black = black;
		this.text = text;
		this.numAnswers = numAnswers;
		this.expansion = expansion;
		shuffle(maxDeck);
	}

	@Override
	public int compareTo(Card o) {
		if (id == o.id) {
			return 0;
		}
		int c = index - o.index;
		if (c == 0) {
			return id - o.id;
		}
		return c;
	}
	
	@Override 
	public int hashCode() {
		return id * 37;
	}
	
	/**
	 * shuffles the card's value for use in priority queues
	 * @param maxDeck the deck size for shuffling (used in priority queues)
	 */
	public void shuffle(int maxDeck) {
		this.index = (int) Math.round(Math.random() * maxDeck);
	}
	
}

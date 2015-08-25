import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class CardShark {

	public static void main(String[] args) throws FileNotFoundException {
		new CardsAgainstHumanity(new FileInputStream("cards.json"));
	}
	
}

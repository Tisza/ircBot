import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.jibble.pircbot.*;


public class Runner {
	
	public static final boolean DEBUG = true;
	public static final String SERVER = "irc.freenode.net";
	public static final String CHANNEL = "#testingabotlol";
	public static final String NAME = "botinprogress";
	
	/**
	 * @param args
	 * @throws IrcException 
	 * @throws IOException 
	 * @throws NickAlreadyInUseException 
	 */
	public static void main(String[] args) {
		PircBot bot = null;
		try {
			bot = new AlphaBot(NAME, "ops.dat", "xi wector");
		} catch (FileNotFoundException e) {
			System.out.println("Failed to find operators file.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("File system failure.");
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		if(DEBUG)
			bot.setVerbose(true);
		try {
		bot.connect(SERVER);
		} catch (NickAlreadyInUseException e) {
			System.out.println("Nick already in use.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Unable to connect.");
			System.out.println(e.getMessage());
			System.exit(1);
		} catch (IrcException e) {
			System.out.println("IRC error: ");
			System.out.println(e.getMessage());
			System.exit(1);
		}
		bot.joinChannel(CHANNEL);
		
		Scanner c = new Scanner(System.in);
		boolean cont = true;
		while(cont) {
			String[] n = c.nextLine().toLowerCase().trim().split(" ");
			commandWrapper(bot, n, "");
			if (n[0].equals("exit")) {
					cont = false;
			}
		}
		System.out.println("Markov save: " + (((AlphaBot) bot).write() ? "Succeeded" : "Failed"));
		bot.disconnect();
		bot.dispose();
		c.close();
		System.exit(0);
	}
	
	/**
	 * Joins an array of strings s into a single string starting at index n. 
	 * @param s The array of strings
	 * @param n the starting index
	 * @return A string of the rest of the array.
	 */
	public static String stringCat(String[] s, int n) {
		String r = "";
		for(int i = n; i < s.length; i++) {
			r += s[i];
			if (i + 1 < s.length) {
				r += " ";
			}
		}
		return r;
	}
	
	/**
	 * interactive control of the bot while it runs.
	 * @param bot the bot to modify
	 * @param n A string array of commands and arguments.
	 * @param irc the target to respond to, or blank for console
	 */
	public static void commandWrapper(PircBot bot, String[] n, String irc) {
		if (n[0].equals("connect")) {	// connect
			try {
			if (n.length == 1) {
				bot.reconnect();
			} else if (n.length == 2) {
				bot.connect(n[1]);
			} else if (n.length == 3) {
				bot.connect(n[1], Integer.parseInt(n[2]));
			} else if (n.length == 4) {
				bot.connect(n[1], Integer.parseInt(n[2]), n[3]);
			}
			} catch (IOException e) {
				if (!irc.isEmpty()) {
					bot.sendMessage(irc, "Cannot connect.");
				} else {
					System.out.println("Cannot connect.");
				}
			} catch (NickAlreadyInUseException e) {
				if (!irc.isEmpty()) {
					bot.sendMessage(irc,  "Nick already used.");
				} else {
					System.out.println("Nick already used.");
				}
			} catch (IrcException e) {
				if (!irc.isEmpty()) {
					bot.sendMessage(irc, "IRC cannot connect.");
				} else {
					System.out.println("IRC cannot connect.");
				}
			}
		} else if (n[0].equals("?connect")) {	// connect?
			if (!irc.isEmpty()) {
				bot.sendMessage(irc, (bot.isConnected() ? "Yes" : "No"));
			} else {
				System.out.println((bot.isConnected() ? "Yes" : "No"));
			}
		} else if (n[0].equals("join")) {	// join
			if (n.length == 2) {
				bot.joinChannel(n[1]);
			} else if (n.length == 3) {
				bot.joinChannel(n[1], n[2]);
			}
		} else if (n[0].equals("part")) {	// part
			if (n.length == 2) {
				bot.partChannel(n[1]);
			} else if (n.length == 3) {
				bot.partChannel(n[1], stringCat(n, 2));
			}
		} else if (n[0].equals("quit")) {	// quit
			if (n.length == 1)
				bot.quitServer();
			else if (n.length == 2) 
				bot.quitServer(n[1]);
		} else if (n[0].equals("msg")) {	// msg
			if (n.length == 3)
				bot.sendMessage(n[1], stringCat(n, 2));
		} else if (n[0].equals("set")) {	// set...
			if (n.length > 1) {
				if (n[1].equals("nick")) {	// nick
					if (n.length > 2)
						bot.changeNick(n[2]);
				} else {					// unknown command.
					if (!irc.isEmpty()) {
						bot.sendMessage(irc, "Unknown property.");
					} else {
						System.out.println("Unknown property.");
					}
				}
			}
		}
	}

}

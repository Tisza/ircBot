import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.jibble.pircbot.*;

public class AlphaBot extends PircBot {
	
	private Set<String> ops;
	private String password;
	private String passwordCheck;
	private String passwordCheckCommand;
	private Map<String, Markov> mk;
	
	
	public AlphaBot(String name, String opsfile, String password)
			throws IOException {
		this.setName(name);
		ops = new HashSet<String>();
		
		Scanner f = new Scanner(new File(opsfile));
		while(f.hasNextLine()) {
			ops.add(f.nextLine().toLowerCase());
		}
		f.close();
		
		this.password = password;
		passwordCheck = "";
		passwordCheckCommand = "";
		
		mk = new HashMap<String, Markov>();
	}
	
	public AlphaBot(String ops, String password) throws IOException {
		this("MyBot", ops, password);
	}
	
	public AlphaBot(String password) throws IOException {
		this("MyBot", "ops.dat", password);
	}
	
	/** 
	 * scans for input and adds to the markov chain.
	 */
	@Override
	protected void onMessage(String channel, String sender, String login,
			String hostname, String message) {
		inputQuery(channel, sender, message);
		markov(sender, message);
	}
	
	/** 
	 * scans for input.
	 */
	@Override
	protected void onPrivateMessage(String sender, String login, 
			String hostname, String message) {
		if (passwordCheck.equals(sender)) {
			if (message.equals(password)) {
				ops.add(sender.toLowerCase());
				command(sender, passwordCheckCommand);
			} else {
				sendMessage(sender, "Permission Denied.");
			}
			passwordCheck = "";
			passwordCheckCommand = "";
		} else {
			inputQuery(sender, sender, message);
			command(sender, message);
			markov(sender, message);
		}
	}
	
	/**
	 * command dispatch
	 * @param channel the channel it was given from
	 * @param sender the sender of the command
	 * @param message the command and arguments
	 */
	protected void inputQuery(String channel, String sender, String message) {
		if (message.toLowerCase().startsWith("!")) {
			String[] m = message.substring(1).toLowerCase().trim().split(" ");
			if (m.length > 0) {
				if (m[0].equals("roll")) {
					roll(m, channel);
				} else if (m[0].equals("help")) {
					help(sender);
				} else if (m[0].equals("channellist")) {
					String[] ch = getChannels();
					String r = "";
					for(String a : ch) {
						r += a + ", ";
					}
					sendMessage(sender, r.substring(0, r.length() - 2));
				} else if (m[0].equals("imitate")) {
					if (m.length > 1) {
						imitate(m[1], channel);
					} else {
						imitate(sender, channel);
					}
				} else {
					sendMessage(channel, "Unknown command: " + m[0]);
				}
			}
		}
		System.out.println("[" + channel + "] " + sender + ": " + message);
	}
	
	/**
	 * rolls a dice and responds with the result.
	 * @param m the query string
	 * @param channel the source of the query
	 */
	protected void roll(String[] m, String channel) {
		if (m.length > 1) {
			int n = Integer.parseInt(m[1]);
			sendMessage(channel, "" + 
					Math.round(Math.random() * n + 1));
		} else {
			sendMessage(channel, "" + 
					Math.round(Math.random() * 5 + 1));
		}
	}
	
	/**
	 * responds with a list of commands, their arguments, and a description.
	 * @param sender who to send the help to!
	 */
	protected void help(String sender) {
		sendMessage(sender, "!command arguments [optional arguments]" +
			": description");
		sendMessage(sender, "!roll [rank = 6]: rolls a rank " + 
			"number dice.");
		sendMessage(sender, "!channellist: lists the channels this bot is in");
		sendMessage(sender, "!imitate [target = sender]: uses markov chains in"
				+ " an attempt to imitate the target");
		
		sendMessage(sender, "*** Those with bot operator privileges may run"
				+ "irc commands for the bot by pm using \"sudo\" ***");
	}
	
	/**
	 * allows interactive control of the bot from within irc.
	 * @param sender who is attempting to command the bot
	 * @param message the command message being sent.
	 */
	protected void command(String sender, String message) {
		if (message.toLowerCase().startsWith("sudo")) {
			if (ops.contains(sender.toLowerCase())) {
				String[] m = message.substring(4).toLowerCase().trim().split(" ");
				Runner.commandWrapper(this, m, sender);
			} else {
				sendMessage(sender, "Password?");
				passwordCheckCommand = message;
				passwordCheck = sender;
			}
		}
	}
	
	/** 
	 * writes the Markov's to a file...
	 * @return true on success, false on failure.
	 */
	public boolean write() {
		boolean success = true;
		for(Markov m : mk.values()) {
			success = success && m.writeFile();
		}
		return success;
	}
	
	/**
	 * processes a user's message for markov statistics.
	 * @param sender the target responder
	 * @param message the user's message
	 */
	protected void markov(String sender, String message) {
		if (!message.startsWith("!") && !message.startsWith("sudo")) {
			if (!mk.containsKey(sender)) {
				try {
					mk.put(sender, new Markov(sender + "-Markov.dat"));
				} catch (IOException e) {
					System.out.println("ERROR: Failed to create Markov for " + sender);
					return;
				}
			}
			Scanner in = new Scanner(message);
			mk.get(sender).AnalyzeText(in);
			in.close();
		}
	}
	
	/**
	 * runs a markov for the target chain and sends it to back
	 * @param target the markov chain to use
	 * @param back where to send it
	 */
	protected void imitate(String target, String back) {
		if (mk.containsKey(target)) {
			sendMessage(back, mk.get(target).randomSentence());
		} else {
			sendMessage(back, "I don't know " + target + " well enough yet.");
		}
	}
}

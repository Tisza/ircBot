import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.jibble.pircbot.*;

public class AlphaBot extends PircBot {
	
	private Set<String> ops;
	private Set<String> opt;
	private String password;
	private String passwordCheck;
	private String passwordCheckCommand;
	private Map<String, Markov> mk;
	private CardsAgainstHumanity cah;
	private Map<String, String> lastSentence;
	private Map<String, String> lastSaid;
	private Map<String, Boolean> echo;
	
	/**
	 * creates a new bot given the bot's name, the nicks which choose to opt out of markov chains, and a password
	 * @param name the name of the bot
	 * @param optfile a file with names of those who opt out of markov chains
	 * @param password a password bot control use
	 * @throws IOException
	 */
	public AlphaBot(String name, String optfile, String password)
			throws IOException {
		this.setName(name);
		ops = new HashSet<String>();
		opt = new HashSet<String>();
		setMessageDelay(250);
		
		Scanner f = new Scanner(new File(optfile));
		while(f.hasNextLine()) {
			opt.add(f.nextLine().toLowerCase());
		}
		f.close();
		
		this.password = password;
		passwordCheck = "";
		passwordCheckCommand = "";
		
		mk = new HashMap<String, Markov>();
		InputStream in = new FileInputStream("cards.json");
		cah = new CardsAgainstHumanity(in, this);
		in.close();
		
		lastSentence = new HashMap<String, String>();
		lastSaid = new HashMap<String, String>();
		echo = new HashMap<String, Boolean>();
	}
	
	public AlphaBot(String ops, String password) throws IOException {
		this("MyBot", ops, password);
	}
	
	public AlphaBot(String password) throws IOException {
		this("MyBot", "optout.dat", password);
	}
	
	/** 
	 * scans for input and adds to the markov chain.
	 */
	@Override
	protected void onMessage(String channel, String sender, String login,
			String hostname, String message) {
		if (!inputQuery(channel, sender, message) && !cah.onMessage(channel, sender, message)) {
			markov(sender.toLowerCase(), message);
			
			if (!lastSentence.containsKey(channel)) {
				lastSentence.put(channel, "");
				lastSaid.put(channel, "");
				echo.put(channel, false);
			}
			if (!echo.get(channel) && message.equals(lastSentence.get(channel))
					&& !lastSaid.get(channel).equals(sender)) {
				sendMessage(channel, message);
				echo.put(channel, true);
			}
			if (!message.equals(lastSentence.get(channel))) {
				echo.put(channel, false);
			}
			lastSentence.put(channel, message);
			lastSaid.put(channel, sender);
			
		}
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
			if (!inputQuery(sender, sender, message) && !command(sender, message) && !cah.onPrivateMessage(sender, message)) {
				markov(sender.toLowerCase(), message);
			}
		}
	}
	
	/**
	 * command dispatch
	 * @param channel the channel it was given from
	 * @param sender the sender of the command
	 * @param message the command and arguments
	 */
	protected boolean inputQuery(String channel, String sender, String message) {
		System.out.println("[" + channel + "] " + sender + ": " + message);
		if (message.toLowerCase().startsWith("!")) {
			String[] m = message.substring(1).toLowerCase().trim().split(" ");
			if (m.length > 0) {
				if (m[0].equals("roll")) {
					roll(m, channel);
					return true;
				} else if (m[0].equals("help")) {
					help(sender);
					return true;
				} else if (m[0].equals("channellist")) {
					String[] ch = getChannels();
					String r = "";
					for(String a : ch) {
						r += a + ", ";
					}
					sendMessage(sender, r.substring(0, r.length() - 2));
					return true;
				} else if (m[0].equals("imitate")) {
					if (m.length > 1) {
						imitate(m[1], channel);
					} else {
						imitate(sender.toLowerCase(), channel);
					}
					return true;
				} else if (m[0].equals("optout") && m.length > 1) {
					optout(sender.toLowerCase(), m[1]);
					return true;
				}
			}
		}
		return false;
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
		sendMessage(sender, "!optout yes/no/status: changes opt-out status for markov chain generation and storage");
	}
	
	/**
	 * allows interactive control of the bot from within irc.
	 * @param sender who is attempting to command the bot
	 * @param message the command message being sent.
	 */
	protected boolean command(String sender, String message) {
		if (message.toLowerCase().startsWith("sudo")) {
			if (ops.contains(sender.toLowerCase())) {
				String[] m = message.substring(4).toLowerCase().trim().split(" ");
				Runner.commandWrapper(this, m, sender);
				if (m[0].equals("cah")) {
					if (m[1].equals("remove")) {
						cah.removePlayer(m[3]);
						sendMessage(sender, "player removed");
					}
				}
			} else {
				sendMessage(sender, "Password?");
				passwordCheckCommand = message;
				passwordCheck = sender;
			}
			return true;
		} else {
			return false;
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
		if (!opt.contains(sender)) {
			if (!mk.containsKey(sender)) {
				try {
					mk.put(sender, new Markov(sender + "-Markov.dat"));
				} catch (IOException e) {
					System.out.println("ERROR: Failed to create Markov for " + sender);
				}
			}
			if (mk.containsKey(sender)) {
				Scanner in = new Scanner(message);
				mk.get(sender).AnalyzeText(in);
				in.close();
			}
		}
	}
	
	/**
	 * runs a markov for the target chain and sends it to back
	 * @param target the markov chain to use
	 * @param back where to send it
	 */
	protected void imitate(String target, String back) {
		if (opt.contains(target)) {
			sendMessage(back, target + " has opted-out markov chains.");
		} else if (!mk.containsKey(target)) {
			try {
				mk.put(target, new Markov(target + "-Markov.dat"));
			} catch (IOException e) {
				System.out.println("ERROR: Failed to create Markov for " + target);
			}
		}
		if (mk.containsKey(target)) 
			sendMessage(back, mk.get(target).randomSentence());
		else 
			System.out.println("Failed to imitate for some reason...");
	}
	
	/**
	 * changes the opt-in/out'ness of target
	 * @param target the person to include or exclude from markov chains
	 * @param cmd the command for opt-in/out'ness.
	 */
	protected void optout(String target, String cmd) {
		if (cmd.startsWith("y")) {
			opt.add(target);
			try {
				Files.delete(Paths.get(target + "-Markov.dat"));
			} catch (IOException e) {
				System.out.println("Failed to remove " + target + "-Markov.dat file");
			}
			mk.remove(target);
			sendMessage(target, "You are no longer monitored for markov chain generation. Your markov chain file has been deleted.");
		} else if (cmd.startsWith("n")) {
			opt.remove(target);
			sendMessage(target, "You will now have a markov chain generated and saved in a file.");
		} else if (cmd.startsWith("s")) {
			sendMessage(target, (opt.contains(target) ? "Opted-out of markov chains." : "Markov chains are on."));
		} else {
			sendMessage(target, "!optout yes/no/status");
		}
		try {
			PrintStream o = new PrintStream("optout.dat");
			for(String s : opt) {
				o.println(s);
			}
			o.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to re-write the optout file.");
			e.printStackTrace();
		}
	}
}

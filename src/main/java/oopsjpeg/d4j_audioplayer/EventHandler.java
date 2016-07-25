package oopsjpeg.d4j_audioplayer;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.UnsupportedAudioFileException;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.audio.AudioPlayer;

public class EventHandler  {
	
	// This code does not include login methods - it implies you've already logged in and are ready.
	
	private String botprefix = "="; // Understand commands using "="
	
	// Queue audio from specified URL stream for guild
	private static void playAudioFromUrl(String s_url, IGuild guild) throws IOException, UnsupportedAudioFileException {
		URL url = new URL(s_url); // Get URL
		AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild); // Get AudioPlayer for guild
		player.queue(url); // Queue URL stream
	}
	
	// Queue audio from specified file for guild
	private static void playAudioFromFile(String s_file, IGuild guild) throws IOException, UnsupportedAudioFileException {
		File file = new File(s_file); // Get file
		AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild); // Get AudioPlayer for guild
		player.queue(file); // Queue file
	}
	
	// Change AudioPlayer volume for guild
	private static void setVolume(float vol, IGuild guild){
		AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild);
		player.setVolume(vol);
	}
	
	@EventSubscriber
	// Receive messages
	public void OnMesageEvent(MessageReceivedEvent event) throws IOException, UnsupportedAudioFileException, RateLimitException, MissingPermissionsException, DiscordException {
		IMessage message = event.getMessage(); // Get message from event
		
		if(message.getContent().startsWith(botprefix)){
			String command = message.getContent().replaceFirst(botprefix, ""); // Remove prefix
			String[] args = command.split(" "); // Split command into arguments
			
			// Check for "summon" as command
			if(args[0].equalsIgnoreCase("summon")){
				// Get the user's voice channel
				IVoiceChannel voicechannel = message.getAuthor().getConnectedVoiceChannels().get(0);
				// Join the channel
				voicechannel.join();
				// Send message
				message.getChannel().sendMessage("Joined `" + voicechannel.getName() + "`.");
			}
			
			// Check for "playfile" as command
			if(args[0].equalsIgnoreCase("playfile")){
				// Queue up test file from local path
				playAudioFromFile("SoundHelix-Song-1.mp3", message.getGuild());
				// Send message
				message.getChannel().sendMessage("Queued test file.");
				
			// Check for "playurl" as command
			} else if(args[0].equalsIgnoreCase("playurl")){
				// Queue up test file from URL
				playAudioFromUrl("http://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", message.getGuild());
				// Send message
				message.getChannel().sendMessage("Queued test URL.");
			
			// Check for "setvol" as command
			} else if(args[0].equalsIgnoreCase("setvol")){
				// Read first argument as float value
				float vol = Float.parseFloat(args[1]);
				// Set volume for guild
				setVolume(vol, message.getGuild());
				// Send message
				message.getChannel().sendMessage("Set volume to " + vol + ".");
			}
		}
	}
	
}

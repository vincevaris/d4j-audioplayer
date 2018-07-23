package com.oopsjpeg.d4j_audioplayer;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.audio.AudioPlayer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

/**
 * D4J AudioPlayer Example
 * http://github.com/oopsjpeg/d4j-audioplayer/
 *
 * @author oopsjpeg
 */
public class Example {
	// The token that the bot will use.
	private static final String TOKEN = "Insert your bot's token here";
	// The prefix that the bot will use.
	private static final String PREFIX = "!";

	private static IDiscordClient client;

	public static void main(String[] args) throws DiscordException, RateLimitException {
		System.out.println("Logging bot in...");
		client = new ClientBuilder().withToken(TOKEN).build();
		client.getDispatcher().registerListener(new Example());
		client.login();
	}

	@EventSubscriber
	public void onMessage(MessageReceivedEvent event) throws RateLimitException, DiscordException,
			MissingPermissionsException, IOException, UnsupportedAudioFileException {
		IMessage message = event.getMessage();
		IChannel channel = message.getChannel();
		IUser user = message.getAuthor();
		IGuild guild = message.getGuild();
		String content = message.getContent();

		// Make sure the message starts with the prefix
		if (content.startsWith(PREFIX)) {
			String[] split = content.split(" ");
			String alias = split[0].replaceFirst(PREFIX, "");
			String[] args = Arrays.copyOfRange(split, 1, split.length);

			// `join` - Join the user's current voice channel
			if (alias.equalsIgnoreCase("join")) {
				join(channel, user);
				// `queueurl <url>` - Queue a specified URL
			} else if (alias.equalsIgnoreCase("queueurl")) {
				queueUrl(channel, new URL(String.join(" ", args)));
				// `queuefile <filename>` - Queue a specified local file by name
			} else if (alias.equalsIgnoreCase("queuefile")) {
				queueFile(channel, new File(String.join(" ", args)));
				// `resume` - Resume the current audio
			} else if (alias.equalsIgnoreCase("play") || alias.equalsIgnoreCase("unpause")) {
				playing(channel, false);
				// `pause` - Pause/unpause the current audio
			} else if (alias.equalsIgnoreCase("pause")) {
				playing(channel, !getPlayer(guild).isPaused());
				// `skip` - Skip the current audio
			} else if (alias.equalsIgnoreCase("skip")) {
				skip(channel);
				// `vol <volume>` - Sets the volume to a percentage
			} else if (alias.equalsIgnoreCase("vol") || alias.equalsIgnoreCase("volume")) {
				try {
					volume(channel, Integer.parseInt(args[0]));
				} catch (NumberFormatException e) {
					channel.sendMessage("Invalid volume percentage.");
				}
			}
		}
	}

	private void join(IChannel channel, IUser user) throws RateLimitException, DiscordException,
			MissingPermissionsException {
		if (user.getVoiceStates().size() < 1)
			channel.sendMessage("You aren't in a voice channel!");
		else {
			IVoiceChannel voice = user.getVoiceStates().get(0).getChannel();
			voice.join();
			channel.sendMessage("Connected to **" + voice.getName() + "**.");
		}
	}

	private void queueUrl(IChannel channel, URL url) throws RateLimitException, DiscordException,
			MissingPermissionsException, IOException, UnsupportedAudioFileException {
		getPlayer(channel.getGuild()).queue(url);
	}

	private void queueFile(IChannel channel, File file) throws RateLimitException, DiscordException,
			MissingPermissionsException, IOException, UnsupportedAudioFileException {
		getPlayer(channel.getGuild()).queue(file);
	}

	private void playing(IChannel channel, boolean pause) {
		getPlayer(channel.getGuild()).setPaused(pause);
	}

	private void skip(IChannel channel) {
		getPlayer(channel.getGuild()).skip();
	}

	private void volume(IChannel channel, int percent) throws RateLimitException, DiscordException,
			MissingPermissionsException {
		volume(channel, (float) (percent) / 100);
	}

	private void volume(IChannel channel, float vol) throws RateLimitException, DiscordException,
			MissingPermissionsException {
		vol = Math.max(0, Math.min(1.5f, vol));
		getPlayer(channel.getGuild()).setVolume(vol);
		channel.sendMessage("Set volume to **" + (int) (vol * 100) + "%**.");
	}

	private AudioPlayer getPlayer(IGuild guild) {
		return AudioPlayer.getAudioPlayerForGuild(guild);
	}

}

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
import sx.blah.discord.util.audio.events.TrackFinishEvent;
import sx.blah.discord.util.audio.events.TrackQueueEvent;
import sx.blah.discord.util.audio.events.TrackStartEvent;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

	// Stores the last channel that the join command was sent from.
	// We do this to send audio-related info to the most relevant channel
	// ex. audio ends, queue finishes
	private final Map<IGuild, IChannel> lastChannel = new HashMap<>();

	public static void main(String[] args) throws DiscordException, RateLimitException {
		System.out.println("Logging bot in...");
		client = new ClientBuilder().withToken(TOKEN).build();
		client.getDispatcher().registerListener(new Example());
		client.login();
	}

	@EventSubscriber
	public void onMessage(MessageReceivedEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
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

			// Update the last audio channel
			lastChannel.put(guild, channel);

			// `join` - Join the user's current voice channel
			if (alias.equalsIgnoreCase("join")) {
				join(channel, user);
			// `queueurl <url>` - Queue a specified URL
			} else if (alias.equalsIgnoreCase("queueurl")) {
				queueUrl(channel, String.join(" ", args));
			// `queuefile <filename>` - Queue a specified local file by name
			} else if (alias.equalsIgnoreCase("queuefile")) {
				queueFile(channel, String.join(" ", args));
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

	/*
	Track events
	 */

	@EventSubscriber
	public void onTrackQueue(TrackQueueEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
		lastChannel.get(event.getPlayer().getGuild())
				.sendMessage("Added **" + getTrackTitle(event.getTrack()) + "** to the playlist.");
	}

	@EventSubscriber
	public void onTrackStart(TrackStartEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
		lastChannel.get(event.getPlayer().getGuild())
				.sendMessage("Now playing **" + getTrackTitle(event.getTrack()) + "**.");
	}

	@EventSubscriber
	public void onTrackFinish(TrackFinishEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
		IGuild guild = event.getPlayer().getGuild();
		lastChannel.get(guild).sendMessage("Finished playing **" + getTrackTitle(event.getOldTrack()) + "**.");

		if (event.getNewTrack() == null)
			lastChannel.get(guild).sendMessage("The playlist is now empty.");
	}

	/*
	Audio player methods
	 */

	private void join(IChannel channel, IUser user) throws RateLimitException, DiscordException, MissingPermissionsException {
		if (user.getVoiceStates().size() < 1)
			channel.sendMessage("You aren't in a voice channel!");
		else {
			IVoiceChannel voice = user.getVoiceStates().get(0).getChannel();
			if (!voice.getModifiedPermissions(client.getOurUser()).contains(Permissions.VOICE_CONNECT))
				channel.sendMessage("I can't join that voice channel!");
			else if (voice.getUserLimit() != 0 && voice.getConnectedUsers().size() >= voice.getUserLimit())
				channel.sendMessage("That room is full!");
			else {
				voice.join();
				channel.sendMessage("Connected to **" + voice.getName() + "**.");
			}
		}
	}

	private void queueUrl(IChannel channel, String url) throws RateLimitException, DiscordException, MissingPermissionsException {
		try {
			URL u = new URL(url);
			setTrackTitle(getPlayer(channel.getGuild()).queue(u), u.getFile());
		} catch (MalformedURLException e) {
			channel.sendMessage("That URL is invalid!");
		} catch (IOException e) {
			channel.sendMessage("An IO exception occured: " + e.getMessage());
		} catch (UnsupportedAudioFileException e) {
			channel.sendMessage("That type of file is not supported!");
		}
	}

	private void queueFile(IChannel channel, String file) throws RateLimitException, DiscordException, MissingPermissionsException {
		File f = new File(file);
		if (!f.exists())
			channel.sendMessage("That file doesn't exist!");
		else if (!f.canRead())
			channel.sendMessage("I don't have access to that file!");
		else {
			try {
				setTrackTitle(getPlayer(channel.getGuild()).queue(f), f.toString());
			} catch (IOException e) {
				channel.sendMessage("An IO exception occured: " + e.getMessage());
			} catch (UnsupportedAudioFileException e) {
				channel.sendMessage("That type of file is not supported!");
			}
		}
	}

	private void playing(IChannel channel, boolean pause) {
		getPlayer(channel.getGuild()).setPaused(pause);
	}

	private void skip(IChannel channel) {
		getPlayer(channel.getGuild()).skip();
	}

	private void volume(IChannel channel, int percent) throws RateLimitException, DiscordException, MissingPermissionsException {
		volume(channel, (float) (percent) / 100);
	}

	private void volume(IChannel channel, Float vol) throws RateLimitException, DiscordException, MissingPermissionsException {
		if (vol > 1.5) vol = 1.5f;
		if (vol < 0) vol = 0f;
		getPlayer(channel.getGuild()).setVolume(vol);
		channel.sendMessage("Set volume to **" + (int) (vol * 100) + "%**.");
	}

	/*
	Utility methods
	 */

	private AudioPlayer getPlayer(IGuild guild) {
		return AudioPlayer.getAudioPlayerForGuild(guild);
	}

	private String getTrackTitle(AudioPlayer.Track track) {
		return track.getMetadata().containsKey("title") ? String.valueOf(track.getMetadata().get("title")) : "Unknown Track";
	}

	private void setTrackTitle(AudioPlayer.Track track, String title) {
		track.getMetadata().put("title", title);
	}

}

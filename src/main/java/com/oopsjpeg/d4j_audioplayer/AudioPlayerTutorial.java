package com.oopsjpeg.d4j_audioplayer;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
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
 * D4J AudioPlayer Tutorial
 * http://github.com/oopsjpeg/d4j-audioplayer/
 *
 * @author oopsjpeg
 */
public class AudioPlayerTutorial {
	private static final String TOKEN = "Insert your bot's token here";
	private static final String PREFIX = "!";
	private static IDiscordClient client;

	// Stores the channel that
	private final Map<IGuild, IChannel> lastChannel = new HashMap<>();

	public static void main(String[] args) throws DiscordException, RateLimitException {
		System.out.println("Logging bot in...");
		client = new ClientBuilder().withToken(TOKEN).build();
		client.getDispatcher().registerListener(new AudioPlayerTutorial());
		client.login();
	}

	/*
	General events
	 */

	@EventSubscriber
	public void onReady(ReadyEvent event) {
		System.out.println("Bot is now ready!");
	}

	@EventSubscriber
	public void onMessage(MessageReceivedEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
		IMessage message = event.getMessage();
		IUser user = message.getAuthor();
		if (user.isBot()) return;

		IChannel channel = message.getChannel();
		IGuild guild = message.getGuild();
		String[] split = message.getContent().split(" ");

		if (split.length >= 1 && split[0].startsWith(PREFIX)) {
			String command = split[0].replaceFirst(PREFIX, "");
			String[] args = split.length >= 2 ? Arrays.copyOfRange(split, 1, split.length) : new String[0];

			if (command.equalsIgnoreCase("join")) {
				lastChannel.put(guild, channel);
				join(channel, user);
			} else if (command.equalsIgnoreCase("queueurl")) {
				queueUrl(channel, String.join(" ", args));
			} else if (command.equalsIgnoreCase("queuefile")) {
				queueFile(channel, String.join(" ", args));
			} else if (command.equalsIgnoreCase("play") || command.equalsIgnoreCase("unpause")) {
				pause(channel, false);
			} else if (command.equalsIgnoreCase("pause")) {
				if (getPlayer(guild).isPaused()) pause(channel, false);
				else pause(channel, true);
			} else if (command.equalsIgnoreCase("skip")) {
				skip(channel);
			}
		}
	}

	/*
	Track events
	 */

	@EventSubscriber
	public void onTrackQueue(TrackQueueEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
		IGuild guild = event.getPlayer().getGuild();
		lastChannel.get(guild).sendMessage("Added **" + getTrackTitle(event.getTrack()) + "** to the playlist.");
	}

	@EventSubscriber
	public void onTrackStart(TrackStartEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
		IGuild guild = event.getPlayer().getGuild();
		lastChannel.get(guild).sendMessage("Now playing **" + getTrackTitle(event.getTrack()) + "**.");
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
		if (user.getConnectedVoiceChannels().size() < 1)
			channel.sendMessage("You aren't in a voice channel!");
		else {
			IVoiceChannel voice = user.getConnectedVoiceChannels().get(0);
			if (!voice.getModifiedPermissions(client.getOurUser()).contains(Permissions.VOICE_CONNECT))
				channel.sendMessage("I can't join that voice channel!");
			else if (voice.getConnectedUsers().size() >= voice.getUserLimit())
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

	private void pause(IChannel channel, boolean pause) {
		getPlayer(channel.getGuild()).setPaused(pause);
	}

	private void skip(IChannel channel) {
		getPlayer(channel.getGuild()).skip();
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

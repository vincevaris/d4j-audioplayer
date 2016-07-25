# Discord4J AudioPlayer Tutorial
Requires **version 2.5.0+**

Discord4J: http://austinv11.github.io/Discord4J/

_This tutorial assumes you have a basic understanding of D4J and know how to log into your bot and create listeners._

As of version 2.5.0 for Discord4J, the [AudioChannel](https://jitpack.io/com/github/austinv11/Discord4j/2.5.0/javadoc/sx/blah/discord/handle/AudioChannel.html) method has been deprecated and replaced with the somewhat confusing (yet easy to learn) [AudioPlayer](https://jitpack.io/com/github/austinv11/Discord4j/2.5.0/javadoc/sx/blah/discord/util/audio/AudioPlayer.html). In this tutorial, I will explain the basics of AudioPlayer, such as **Joining voice channels, playing audio from URL and file, and setting volume.** There's more methods you can check out on the [javadocs](https://jitpack.io/com/github/austinv11/Discord4j/2.5.0/javadoc/sx/blah/discord/util/audio/AudioPlayer.html).

## Joining channels

First, we need to grab a user's IVoiceChannel before we can join it.

This is a basic command setup to accept commands starting with "=".

```java
public class EventHandler {
	private String botprefix = "="; // Understand commands using "="
	
	@EventSubscriber
	// Receive messages
	public void OnMesageEvent(MessageReceivedEvent event) throws IOException, UnsupportedAudioFileException, RateLimitException, MissingPermissionsException, DiscordException {
		IMessage message = event.getMessage(); // Get message from event
		
		if(message.getContent().startsWith(botprefix)){
			String command = message.getContent().replaceFirst(botprefix, ""); // Remove prefix
			String[] args = command.split(" "); // Split command into arguments
		}
	}
}
```

Here, we're able to parse messages that begin with "=" as a command. We now need to make a command that summons the bot into an audio channel with a command.

```java
String command = message.getContent().replaceFirst(botprefix, "");
String[] args = command.split(" ");

// Check for "summon" as command
if(args[0].equalsIgnoreCase("summon")){
	// Get the user's voice channel
	IVoiceChannel voicechannel = message.getAuthor().getConnectedVoiceChannels().get(0);
	// Join the channel
	voicechannel.join();
	// Send message
	message.getChannel().sendMessage("Joined `" + voicechannel.getName() + "`.");
}
```

With `equalsIgnoreCase("summon")`, the bot will now recognize the command "=summon", and work upon it.

`IVoiceChannel voicechannel` indicates a Discord voice channel object, and `message.getAuthor().getConnectedVoiceChannels().get(0)` gets the message author's connected voice channels and uses the first one.

## Playing audio

Let's make two methods that play / queue audio - `playAudioFromURL(String s_url, IGuild guild)` and `playAudioFromFile(String s_file, IGuild guild)`.

```java
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
```

Both events use `AudioPlayer.getAudioPlayerForGuild(guild);` to get / create an AudioPlayer for a certain guild. The AudioPlayer holds the tracks to be queued, volume of the player, etc.

Now, let's put these methods to use. I've stored an mp3 file named _SoundHelix-Song-1_ in the project directory, and have a [URL from SoundHelix](http://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3) with a different mp3 file. We need two new commands.

```java
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
}
```

These statements will now play audio from a certain source, and send a message in chat.

## Changing volume

Now that the bot can join channels and play audio, the audio might be too loud. Volume is set by 0 - 1 (or beyond that if you're crazy). The default value is 1, which translates to 100%. Let's add a method to change that.

```java
// Change AudioPlayer volume for guild
private static void setVolume(float vol, IGuild guild){
	AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild);
	player.setVolume(vol);
}
```

This method grabs the AudioPlayer from a guild and sets the volume to a specified float. You should also keep in mind that floats can go pretty damn high, so you may want to limit the float's value.

Finally, let's create the command to let a user set volume.

```java
// Check for "setvol" as command
} else if(args[0].equalsIgnoreCase("setvol")){
	// Read first argument as float value
	float vol = Float.parseFloat(args[1]);
	// Set volume for guild
	setVolume(vol, message.getGuild());
	// Send message
	message.getChannel().sendMessage("Set volume to " + vol + ".");
}
```

Done! This command now sets the volume to a specified float using "=setvol <float>".

This should now explain the basics of using AudioPlayer to play music in voice channels with others in guilds. Being set up correctly, you can use AudioPlayer to stream audio from sites such as SoundCloud and create playlists to listen to while you smash your head on the keyboard!

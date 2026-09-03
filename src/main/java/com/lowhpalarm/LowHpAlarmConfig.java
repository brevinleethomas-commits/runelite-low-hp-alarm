package com.lowhpalarm;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("lowHpAlarm")
public interface LowHpAlarmConfig extends Config
{
	@Range(min = 1, max = 50)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "thresholdPercent",
		name = "HP threshold",
		description = "Start looping the alarm when current hitpoints are at or below this percent of max.",
		position = 1
	)
	default int thresholdPercent()
	{
		return 10;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "Playback volume for the alarm.",
		position = 2
	)
	default int volume()
	{
		return 80;
	}

	@ConfigItem(
		keyName = "soundFile",
		name = "Custom WAV path",
		description = "Full path to a .wav file. Leave blank to use alarm.wav from .runelite/low-hp-alarm/, or the built-in siren.",
		position = 3
	)
	default String soundFile()
	{
		return "";
	}

	@ConfigItem(
		keyName = "chatMessage",
		name = "Chat warning",
		description = "Write a game-chat line when the alarm starts.",
		position = 4
	)
	default boolean chatMessage()
	{
		return true;
	}
}

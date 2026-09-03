package com.lowhpalarm;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.StatChanged;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Low HP Alarm",
	description = "Loops a sound when hitpoints fall to the configured percent",
	tags = {"combat", "pvm", "alert", "hp", "sound"}
)
public class LowHpAlarmPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private LowHpAlarmConfig config;

	@Inject
	private AudioPlayer audioPlayer;

	private AlarmPlayer player;
	private boolean alarming;

	@Override
	protected void startUp()
	{
		player = new AlarmPlayer(audioPlayer);
		evaluate();
	}

	@Override
	protected void shutDown()
	{
		alarming = false;
		if (player != null)
		{
			player.close();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.HITPOINTS)
		{
			evaluate();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			evaluate();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		evaluate();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			stopAlarm();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("lowHpAlarm".equals(event.getGroup()))
		{
			if (player != null)
			{
				player.close();
			}
			evaluate();
		}
	}

	private void evaluate()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			stopAlarm();
			return;
		}

		int current = client.getBoostedSkillLevel(Skill.HITPOINTS);
		int max = Math.max(1, client.getRealSkillLevel(Skill.HITPOINTS));
		boolean low = current * 100 <= config.thresholdPercent() * max;

		if (low)
		{
			startAlarm();
		}
		else
		{
			stopAlarm();
		}
	}

	private void startAlarm()
	{
		boolean first = !alarming;
		alarming = true;
		if (player != null)
		{
			player.tick(config);
		}
		if (first && config.chatMessage())
		{
			client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"Low HP alarm — eat before the loop stops.",
				null
			);
		}
	}

	private void stopAlarm()
	{
		alarming = false;
		if (player != null)
		{
			player.stop();
		}
	}

	@Provides
	LowHpAlarmConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LowHpAlarmConfig.class);
	}
}

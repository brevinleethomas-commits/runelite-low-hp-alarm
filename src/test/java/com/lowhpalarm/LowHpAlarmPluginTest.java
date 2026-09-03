package com.lowhpalarm;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LowHpAlarmPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LowHpAlarmPlugin.class);
		RuneLite.main(args);
	}
}

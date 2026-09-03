package com.lowhpalarm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.audio.AudioPlayer;

@Slf4j
final class AlarmPlayer
{
	private static final long REPEAT_MS = 550;

	private final AudioPlayer audioPlayer;
	private long lastPlayMs;

	AlarmPlayer(AudioPlayer audioPlayer)
	{
		this.audioPlayer = audioPlayer;
	}

	void tick(LowHpAlarmConfig config)
	{
		long now = System.currentTimeMillis();
		if (now - lastPlayMs < REPEAT_MS)
		{
			return;
		}
		lastPlayMs = now;

		float gain = toGain(config.volume());
		try
		{
			File custom = resolveFile(config.soundFile());
			if (custom != null)
			{
				audioPlayer.play(custom, gain);
			}
			else
			{
				audioPlayer.play(new ByteArrayInputStream(builtInWav()), gain);
			}
		}
		catch (Exception ex)
		{
			log.warn("Alarm playback failed", ex);
		}
	}

	void stop()
	{
		lastPlayMs = 0;
	}

	void close()
	{
		stop();
	}

	private static float toGain(int percent)
	{
		int p = Math.max(0, Math.min(100, percent));
		if (p <= 0)
		{
			return -80f;
		}
		return (float) (20.0 * Math.log10(p / 100.0));
	}

	private static File resolveFile(String configured)
	{
		if (configured != null && !configured.isBlank())
		{
			File direct = new File(configured.trim());
			if (direct.isFile())
			{
				return direct;
			}
		}
		File dropped = new File(new File(RuneLite.RUNELITE_DIR, "low-hp-alarm"), "alarm.wav");
		if (dropped.isFile())
		{
			return dropped;
		}
		return null;
	}

	static byte[] builtInWav()
	{
		final int sampleRate = 8000;
		final int durationMs = 520;
		final int n = sampleRate * durationMs / 1000;
		byte[] pcm = new byte[n];
		for (int i = 0; i < n; i++)
		{
			double t = i / (double) sampleRate;
			double freq = (i < n / 2) ? 880 : 620;
			double env = (i % (sampleRate / 8) < sampleRate / 16) ? 1 : 0;
			pcm[i] = (byte) (Math.sin(2 * Math.PI * freq * t) * 80 * env);
		}
		int dataSize = pcm.length;
		byte[] wav = new byte[44 + dataSize];
		writeAscii(wav, 0, "RIFF");
		writeInt(wav, 4, 36 + dataSize);
		writeAscii(wav, 8, "WAVE");
		writeAscii(wav, 12, "fmt ");
		writeInt(wav, 16, 16);
		writeShort(wav, 20, 1);
		writeShort(wav, 22, 1);
		writeInt(wav, 24, sampleRate);
		writeInt(wav, 28, sampleRate);
		writeShort(wav, 32, 1);
		writeShort(wav, 34, 8);
		writeAscii(wav, 36, "data");
		writeInt(wav, 40, dataSize);
		System.arraycopy(pcm, 0, wav, 44, dataSize);
		return wav;
	}

	private static void writeAscii(byte[] out, int off, String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			out[off + i] = (byte) s.charAt(i);
		}
	}

	private static void writeInt(byte[] out, int off, int v)
	{
		out[off] = (byte) v;
		out[off + 1] = (byte) (v >> 8);
		out[off + 2] = (byte) (v >> 16);
		out[off + 3] = (byte) (v >> 24);
	}

	private static void writeShort(byte[] out, int off, int v)
	{
		out[off] = (byte) v;
		out[off + 1] = (byte) (v >> 8);
	}
}

package com.lowhpalarm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

@Slf4j
final class AlarmPlayer
{
	private Clip clip;
	private String loadedPath = "";
	private int loadedVolume = -1;

	void start(LowHpAlarmConfig config)
	{
		ensureClip(config);
		if (clip == null)
		{
			return;
		}
		applyVolume(config.volume());
		if (clip.isActive())
		{
			return;
		}
		clip.setFramePosition(0);
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}

	void stop()
	{
		if (clip != null && clip.isOpen())
		{
			clip.stop();
			clip.setFramePosition(0);
		}
	}

	void close()
	{
		stop();
		if (clip != null)
		{
			clip.close();
			clip = null;
		}
		loadedPath = "";
		loadedVolume = -1;
	}

	private void ensureClip(LowHpAlarmConfig config)
	{
		String path = resolvePath(config.soundFile());
		if (clip != null && path.equals(loadedPath))
		{
			return;
		}
		close();
		try
		{
			clip = AudioSystem.getClip();
			if (!path.isEmpty())
			{
				try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path)))
				{
					clip.open(stream);
				}
			}
			else
			{
				try (AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(builtInWav())))
				{
					clip.open(stream);
				}
			}
			loadedPath = path;
		}
		catch (UnsupportedAudioFileException | IOException | LineUnavailableException | IllegalArgumentException ex)
		{
			log.warn("Could not open alarm sound, using built-in tone", ex);
			try
			{
				if (clip != null && clip.isOpen())
				{
					clip.close();
				}
				clip = AudioSystem.getClip();
				try (AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(builtInWav())))
				{
					clip.open(stream);
				}
				loadedPath = "";
			}
			catch (Exception inner)
			{
				log.warn("Built-in alarm failed", inner);
				clip = null;
			}
		}
	}

	private void applyVolume(int percent)
	{
		if (clip == null || percent == loadedVolume)
		{
			return;
		}
		loadedVolume = percent;
		if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
		{
			return;
		}
		FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float min = gain.getMinimum();
		float max = Math.min(gain.getMaximum(), 0f);
		float t = Math.max(0, Math.min(100, percent)) / 100f;
		gain.setValue(min + (max - min) * t);
	}

	private static String resolvePath(String configured)
	{
		if (configured != null && !configured.isBlank())
		{
			File direct = new File(configured.trim());
			if (direct.isFile())
			{
				return direct.getAbsolutePath();
			}
		}
		File dropped = new File(new File(RuneLite.RUNELITE_DIR, "low-hp-alarm"), "alarm.wav");
		if (dropped.isFile())
		{
			return dropped.getAbsolutePath();
		}
		return "";
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

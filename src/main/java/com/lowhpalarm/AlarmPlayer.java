package com.lowhpalarm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.audio.AudioPlayer;

@Slf4j
final class AlarmPlayer
{
	private static final long BUILTIN_MS = 520;

	private final AudioPlayer audioPlayer;
	private long nextPlayMs;

	AlarmPlayer(AudioPlayer audioPlayer)
	{
		this.audioPlayer = audioPlayer;
	}

	void tick(LowHpAlarmConfig config)
	{
		long now = System.currentTimeMillis();
		if (now < nextPlayMs)
		{
			return;
		}

		File custom = resolveFile(config.soundFile());
		long durationMs = custom != null ? wavDurationMs(custom) : BUILTIN_MS;
		nextPlayMs = now + durationMs + 40;

		float gain = toGain(config.volume());
		try
		{
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
			nextPlayMs = now + 1000;
		}
	}

	void stop()
	{
		nextPlayMs = 0;
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

	private static long wavDurationMs(File file)
	{
		try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
		{
			if (raf.length() < 44 || !"RIFF".equals(readAscii(raf, 4)))
			{
				return BUILTIN_MS;
			}
			raf.skipBytes(4);
			if (!"WAVE".equals(readAscii(raf, 4)))
			{
				return BUILTIN_MS;
			}

			int byteRate = 0;
			int dataSize = 0;
			while (raf.getFilePointer() + 8 <= raf.length())
			{
				String id = readAscii(raf, 4);
				int size = readIntLE(raf);
				if (size < 0)
				{
					break;
				}
				long dataStart = raf.getFilePointer();
				long next = Math.min(raf.length(), dataStart + size + (size & 1));

				if ("fmt ".equals(id) && size >= 16)
				{
					raf.skipBytes(8);
					byteRate = readIntLE(raf);
				}
				else if ("data".equals(id))
				{
					dataSize = size;
					break;
				}
				raf.seek(next);
			}

			if (byteRate > 0 && dataSize > 0)
			{
				return Math.max(200L, dataSize * 1000L / byteRate);
			}
		}
		catch (IOException ignored)
		{
		}
		return BUILTIN_MS;
	}

	private static String readAscii(RandomAccessFile raf, int n) throws IOException
	{
		byte[] b = new byte[n];
		raf.readFully(b);
		return new String(b, 0, n);
	}

	private static int readIntLE(RandomAccessFile raf) throws IOException
	{
		int b0 = raf.readUnsignedByte();
		int b1 = raf.readUnsignedByte();
		int b2 = raf.readUnsignedByte();
		int b3 = raf.readUnsignedByte();
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
	}

	static byte[] builtInWav()
	{
		final int sampleRate = 8000;
		final int durationMs = (int) BUILTIN_MS;
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

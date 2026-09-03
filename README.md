# Low HP Alarm (RuneLite)

Loops a sound whenever your hitpoints drop to **10%** (configurable) or below.
The loop stops as soon as you eat back above the threshold.

This is a real external RuneLite plugin. Official RuneLite will not load a random
jar from the internet — you run it through the Plugin Hub example-plugin
workflow (developer client).

## Load it in RuneLite

1. Install [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Community is fine) and a JDK 11+.
2. **File → New → Project from Existing Sources** and open this folder.
3. Trust the Gradle project when IntelliJ asks.
4. Open `build.gradle` and run the **`run`** task (green triangle).
5. Log into the RuneLite window that appears (Jagex accounts: see
   [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)).
6. Sidebar → configuration → enable **Low HP Alarm**.

## Custom sound

Use a **`.wav`** file (PCM). Either:

- Drop it at `~/.runelite/low-hp-alarm/alarm.wav` (Windows: `%USERPROFILE%\.runelite\low-hp-alarm\alarm.wav`)
- Or paste the full path into the plugin setting **Custom WAV path**

MP3 is not supported by Java’s built-in clip player.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| HP threshold | 10% | Alarm starts at or below this percent of max HP |
| Volume | 80% | Clip gain |
| Custom WAV path | empty | Overrides the built-in siren |
| Chat warning | on | Game-chat line when the loop starts |

## Plugin Hub

To publish for one-click install inside normal RuneLite, fork this repo and
follow [Creating Plugin Hub Plugins](https://github.com/runelite/plugin-hub/blob/master/README.md).
Until it is accepted, only the developer `run` client loads it.

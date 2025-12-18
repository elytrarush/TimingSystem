[![](https://www.jitpack.io/v/FrostHexABG/TimingSystem.svg)](https://www.jitpack.io/#FrostHexABG/TimingSystem)

# TimingSystem

An Elytra time trial and event management plugin.  
Based on [Makkuusen](https://github.com/Makkuusen/TimingSystem) and [FrostHexABG Team](https://github.com/FrostHexABG/TimingSystem).

## What does it do?

TimingSystem is a plugin that aims to do a few things.

- Creation of Elytra time-trials.
- Displaying leaderboards. In-game holograms and/or command-based ones.
- Get rockets at checkpoints.
- [Optional] Get wind charges at checkpoints.

## Installation

First of all you need a Minecraft server running [Paper](https://papermc.io). Then you can get started by following these easy steps:

1. Download the TimingSystem version that is built for your MC server version. If you are running MC 1.21.4 or newer use the [latest release](https://github.com/FrostHexABG/TimingSystem/releases/latest/). If you are running MC 1.21.1 or lower use version [2.3.x](https://github.com/FrostHexABG/TimingSystem/releases/tag/2.3).
2. Add World Edit or [FastAsyncWorldEdit](https://www.spigotmc.org/resources/fastasyncworldedit.13932/) to your /plugins folder.
3. [Optional] Add [DecentHolograms](https://www.spigotmc.org/resources/decentholograms-1-8-1-20-1-papi-support-no-dependencies.96927/) to enable the use of hologram leaderboards.

## Plugin Add-ons

- [TimingSystemNoBoatCollisions](https://github.com/FrostHexABG/TimingSystemNoBoatCollisions) - Removes server side boat collisions. Client side collisions will still occur. This helps players with different pings race together.
- [TrackExchange](https://github.com/Pigalala/TrackExchange) - Makes it possible to copy and paste tracks on the same server or between different servers.
- [TimingSystemRESTApi](https://github.com/JustBru00/TimingSystemRESTApi) - Adds a basic JSON REST API to the TimingSystem plugin.
- [TimingSystemBlueMap](https://github.com/JustBru00/TimingSystemBlueMap) - Adds TimingSystem track locations to [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap).

## TAB below-name: Global rank

TimingSystem exposes PlaceholderAPI placeholders you can use in TAB's below-name feature:

- `%timingsystem_global_rank%` (1 = best, `-` = unranked)
- `%timingsystem_global_points%`

Example `TAB/config.yml`:

```yml
belowname:
	enabled: true
	value: "&6Rank &f#%timingsystem_global_rank%"
```

## For Developers

Export the plugin directly into your local developement server:

```bash
export PAPER_SERVER_PATH="<your path>"
mvn clean package
```

If you want to develop plugins that add on to TimingSystem, you should start by adding TimingSystem as a maven dependency.  
Step 1. Add the JitPack repository.

```xml
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://www.jitpack.io</url>
	</repository>
</repositories>
```

Step 2. Add the TimingSystem dependancy

```xml
<dependency>
    <groupId>com.github.FrostHexABG</groupId>
    <artifactId>TimingSystem</artifactId>
    <version>3.0.5</version>
</dependency>
```

Timing System was originally forked from [EpicIceTrack](https://github.com/JustBru00/NetherCubeParkour).

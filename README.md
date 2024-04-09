[![](https://www.jitpack.io/v/FrostHexABG/TimingSystem.svg)](https://www.jitpack.io/#FrostHexABG/TimingSystem)
# TimingSystem
An ice boat time trial and event management plugin.   
Created by [Makkuusen](https://github.com/Makkuusen/TimingSystem). This fork maintained by [JustBru00](https://github.com/JustBru00) for FrostHex.com

## What does it do?
TimingSystem is a plugin that aims to do a few things.
* Creation and time-trials of 3 various track types. [Boats, Elytra and Parkour]
* Displaying leaderboards. In-game holograms and/or command-based ones.
* Creating and managing events to host races. (Currently only supports Boats).

## Installation
First of all you need a Minecraft server running [Paper](https://papermc.io). Then you can get started by following these easy steps:

1. Download the [latest release](https://github.com/Makkuusen/TimingSystem/releases) of TimingSystem and put the .jar in /plugins folder. 
1. Add World Edit or [FastAsyncWorldEdit](https://www.spigotmc.org/resources/fastasyncworldedit.13932/) to your /plugins folder.
1. _[Optional] Add [DecentHolograms](https://www.spigotmc.org/resources/decentholograms-1-8-1-20-1-papi-support-no-dependencies.96927/) to enable the use of hologram leaderboards._


## Plugin Add-ons
* [TrackExchange](https://github.com/Pigalala/TrackExchange) - Makes it possible to copy and paste tracks on servers.
* [TimingSystemRESTApi](https://github.com/JustBru00/TimingSystemRESTApi) - Adds a basic JSON REST API to the TimingSystem plugin.
* [TimingSystemBlueMap](https://github.com/JustBru00/TimingSystemBlueMap) - Adds TimingSystem track locations to [BlueMap](https://github.com/BlueMap-Minecraft/BlueMap).

## Recommended client mods
* [OpenBoatUtils](https://modrinth.com/mod/openboatutils/versions) - Enables additional boat behaviours supported by TimingSystem.
* [OinkScoreboard](https://github.com/Pigalala/OinkScoreboard) - Makes it possible to display more than 15 rows for big races.

## For Developers
If you want to develop plugins that add on to TimingSystem, you should start by adding TimingSystem as a maven dependancy.    
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
    <version>2.2</version>
</dependency>
```

Timing System was originally forked from [EpicIceTrack](https://github.com/JustBru00/NetherCubeParkour).

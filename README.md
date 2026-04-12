[![Release](https://jitpack.io/v/umjammer/vavi-sound-flac.svg)](https://jitpack.io/#umjammer/vavi-sound-flac)
[![Java CI](https://github.com/umjammer/vavi-sound-flac/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-flac/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-flac/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-sound-flac/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-25-b07219)
[![Parent](https://img.shields.io/badge/Parent-vavi--sound--sandbox-pink)](https://github.com/umjammer/vavi-sound-sandbox)

# vavi-sound-flac

<img src="https://github.com/umjammer/vavi-image-avif/assets/493908/b3c1389e-e50e-402b-921c-1264f8adb117" width="200" alt="FLAC logo"/><sub><a href="https://wiki.xiph.org/XiphWiki:Copyrights">CC BY 3.0 and revised BSD</a></sub>

Pure Java FLAC decoder (Java Sound SPI) powered by [JustFlac](https://jitpack.io/#drogatkin/JustFLAC)

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-sound-flac)

## References

 * https://sourceforge.net/projects/javaflacencoder/

## Usage

```java
    AudioInputStream ais = AudioSystem.getAudioInputStream(Paths.get(flac).toFile());
    Clip clip = AudioSystem.getClip();
    clip.open(AudioSystem.getAudioInputStream(new AudioFormat(44100, 16, 2, true, false), ais));
    clip.loop(Clip.LOOP_CONTINUOUSLY);
```

## TODO

 * ~~rename project into vavi-sound-flac~~

---

[Original](https://jitpack.io/#drogatkin/JustFLAC)

Welcome to JustFLAC
===================

What the heck?
--------------

It isn't a fish, it's just a fork of the popular jFLAC decoder. 

Install
-------

https://jitpack.io/#drogatkin/JustFLAC


Why fork jFLAC?
---------------

jFLAC hasn't been updated in a long time and does not support certain formats like 24-bit/192kHz.
The decoder also has a few minor bugs. So this project adds support for a new format extension
and fixes some bugs.

JustFLAC is already used in MediaChest and Music-Barrel (Java programs)
giving life to audiophile quality formats previously supported only by Foobar and DeaDBeeF (C programs).

Where to find the FLAC standard?
--------------------------------

Follow the [link](https://www.xiph.org/flac/format.html).

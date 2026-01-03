# 1/10000 chance for Withered Foxy Jumpscare every second
### JAVA EDITION

Inspiration: https://steamcommunity.com/sharedfiles/filedetails/?id=3481943642

i didn't sleep today since my mood was terrible so i made this

happy new year!! here comes 2026!!

> [!NOTE]
> Java 25 is required to build and run this thing...

> [!WARNING]
> oh yeah i should NOT be responsible for whatever happens when you use this app (application), that's up to the end user (they can use this app for whatever they want at their own risk 💔).

## How to build?
1. `gradlew jar`

## Preferences
Preferences are stored wherever 
[`Preferences`](https://www.google.com/search?q=java+preferences+location) stores them 🤷‍♂️

on Windows it could be somewhere on
`Computer\HKEY_CURRENT_USER\Software\JavaSoft\Prefs\io\github\brickwall2900\jumpscare`

on Linux it might be somewhere on
`~/.java/.userPrefs`

on Mac i have no idea...
*maybe it could be stored here? `~/Library/Preferences/com.apple.java.util.prefs.plist`*

right now, the keys and their default values are:
* `Chance` = `10000`; the chance the jumpscare appears every interval
* `Interval` = `1`; time in seconds to check if the jumpscare should appear
* `PrepareSeconds` = `5`; time in seconds before the jumpscare appears
* `DelaySeconds` = `10`; time in seconds to delay after the jumpscare appears

### How do I have a different jumpscare?
Adding a jumpscare is easy... i think.

Jumpscare definitions are defined in a `jumpscare.properties` file. The `jumpscare.properties` file looks like this:
```properties
# Jumpscare definitions are declared using a unique <identifier>.
# Each property for a jumpscare is prefixed with that identifier.

# The delay in seconds between each frame.
jumpscare1.frameDelay = 0.05

# The directory (folder) containing the jumpscare frames.
# The frame order will be determined by file name.
jumpscare1.frameFolder = path/to/frames

# Image format of each jumpscare frame (e.g. png, jpg).
jumpscare1.frameType = png

# The higher the number, the higher the chance of this jumpscare appearing.
# This property is optional, it defaults to 1.
jumpscare1.weight = 6.7

# The path to the jumpscare sound. It MUST be a .wav file.
jumpscare.sound = path/to/sound.wav


# Here, I can define multiple jumpscares in the same file!
freddy.frameDelay = 6.7
freddy.frameFolder = path/to/freddy
freddy.frameType = png
freddy.sound = path/to/freddy.wav

foxy.frameDelay = 6.9
foxy.frameFolder = C:\\homework\\foxy
foxy.frameType = png
# Make sure to use double backslashes when referencing Windows paths.
foxy.sound = C:\\homework\\foxy\\sound.wav
foxy.weight = 2
```

The precedence for loading a jumpscare definition is as follows:
1. Provided as a command line argument: `java -jar Jumpscare-x.x.x.jar jumpscare.properties`.
2. Provided as a system property: `java -Djumpscare.definitions=jumpscare.properties -jar Jumpscare-x.x.x.jar`.
3. A `jumpscare.properties` file exists in the current directory the JAR file runs on.
4. The key `DefaultDefinition` exists in the [preferences](#preferences) with a valid value.
5. Foxy Jumpscare (the default)

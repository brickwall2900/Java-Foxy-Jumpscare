# 1/10000 chance for Withered Foxy Jumpscare every second
### JAVA EDITION

Inspiration: https://steamcommunity.com/sharedfiles/filedetails/?id=3481943642

i didn't sleep today since my mood was terrible so i made this

happy new year!! here comes 2026!!

## How to build?
1. `gradlew jar`

## Preferences
Preferences should be stored wherever 
[`Preferences`](https://www.google.com/search?q=java+preferences+location) stores them 🤷‍♂️

on Windows it could be somewhere on
`Computer\HKEY_CURRENT_USER\Software\JavaSoft\Prefs\io\github\brickwall2900\jumpscare`

on Linux it might be somewhere on
`~/.java/.userPrefs`

on Mac i have no idea
*<small>maybe it could be stored here? `~/Library/Preferences/com.apple.java.util.prefs.plist`</small>*

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
jumpscare1.frameDelay  = 0.05            # The delay in seconds between each frame.
jumpscare1.frameFolder = path/to/frames  # The directory (folder) containing the jumpscare frames.
jumpscare1.frameType   = png             # Image format of each jumpscare frame (e.g. png, jpg).

# Here, I can define multiple jumpscares in the same file!
freddy.frameDelay = 6.7
freddy.frameFolder = path/to/folder
freddy.frameType = png

foxy.frameDelay = 6.9
foxy.frameFolder = C:\\homework\\foxy
foxy.frameType = png
```

The precedence for loading a jumpscare definition is as follows:
1. Provided as a command line argument: `java -jar Jumpscare-x.x.x.jar jumpscare.properties`.
2. A `jumpscare.properties` file exists in the current directory the JAR file runs on.
3. The key `DefaultDefinition` exists in the [preferences](#preferences) with a valid value.
4. Foxy Jumpscare (the default)
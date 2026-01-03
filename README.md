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
Preferences should be stored wherever 
<a href="https://www.google.com/search?q=java+preferences+location">`Preferences`</a> stores them 🤷‍♂️

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
See TODO below.

## Not Implemented
- [ ] changeable jumpscares

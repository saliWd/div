# Prompt
Lists the prompts for different AIs. This is the starting point for the app

## Claude
chat id: TBD  
model: Sonnet 4.6  

### initial description:
Need an android app for API level 36 and newer for the newest Android Studio (2025.3, Panda 4), minSdk is 36, no legacy versions need to be supported.  
It shall use the latest kotlin and AGP 9.2.0 (or newer) version and use as few dependencies as possible.  
It shall store personal data in a sqlite db which is encrypted and unlocks with fingerprint.  
It needs functions to add an entry with date, a rating field (integer number between 1 and 10, use a slider for input) and text field. Entries can also be deleted.  
Display also a calender (above the existing entries) for the last 14 days.  
The days where an entry exists in the db (use the date field) need to have a different color than the days where no entry exists.  
Clicking on a date on the calender opens either the existing entry for editing or creates a new entry for this day.  
The whole database can be exported onto local file system, export file needs to be encrypted with a password.  
This password itself shall be stored in secure location.  
Also an import function for such a file is required.  
Leave 10% at the bottom of the screen empty on all views.  
Use the newest versions where possible (dependencies and JVM and so forth).  
Name of the app is "GueteTag", namespace is ch.widmedia.guetetag  
The texts of the app shall be in German.  
Make it visually appealing, use a font like Raleway-Regular for the titles and a non-serif font for all other texts.  

### Result
Does not compile out of the box, needs afterwork in Android Studio, Gemini AI is able to fix the errors.  
Functionality however is good, all stuff is there (in previous versions)

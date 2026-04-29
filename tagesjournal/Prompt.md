# Prompt
Lists the prompts for different AIs. This is the starting point for the app

## Claude
model: Sonnet 4.6  

### initial description:
Need an android app for API level 36 and newer for the newest Android Studio (2025.3, Panda 4), minSdk is 36, no legacy versions need to be supported.  
It shall use the latest kotlin and AGP 9.2.0 (or newer) version and use as few dependencies as possible.  
It shall store personal data in a database which is encrypted and unlocks with fingerprint.  
The whole database can be exported onto local file system, export file needs to be encrypted with a password.  
This password itself shall be stored in secure location.  
Also an import function for such a file is required.  
It needs functions to add an entry with date, a rating field (integer number between 1 and 10, use a slider for input) and text field. Entries can also be deleted.  
On the main view: display a calender for at least the last 7 days. The calender display starts with Monday but only the last 7 days are clickable and lead to the view where I can edit or create a database entry.  
The days where an entry exists in the db (use the date field) need to have a different color than the days where no entry exists.  
Below this calender (calender display takes about 40% of the available height) show a list of existing entries.  
Clicking on a date on the calender opens either the existing entry for editing or creates a new entry for this day if none exists yet.  
Leave 10% at the bottom of the screen empty on all views.  
Use the newest versions where possible (dependencies and JVM and so forth).  
Name of the app is "GueteTag", namespace is ch.widmedia.guetetag  
Make it visually appealing, use a built-in serif-font for the titles and a non-serif font for all other texts. Generous spacing, use dividers between different sections. Colors should have a noble touch, hinting toward a noble diary.  
The texts of the app shall be in German.  

### Refinement
- change app symbol
- weekday (long version e.g. "Mittwoch") shall be displayed before the date on the Eintrag page

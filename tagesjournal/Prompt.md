# Prompt

## Refinement
This list is updated as soon as the stuff works, only the next few points (TODOs) are listed 
1. check whether min-API level 35 (android 15) has an impact. Should not.  
1. define the layout settings of the cards in one place and derive from there (instead of having the same code on every page, e.g. cardElevation is defined at multiple places, always with the same value).  
1. try: have a different background, use the same background on all screens, use the one from the lock screen.  
1. write a python script to automatically derive screen shots from all screens. Use the scrcpy tool to get them. Make use of the versionName (e.g. "2026.06.09") to derive the file name.  
1. rework the settings page, make it more clearly arranged. Maybe: change name and symbol to import/export. Maybe have the link to the import/export at the page where all entries are visible and thus no special symbol anymore on the main page?
1. move the "Tutorial-restart" to the bottom of the main page  
1. clean up the code. Check for newer stable versions of dependencies and in general reduce the number of imports and dependencies.  
1. try out some different UIs / colors. TODO: maybe with another tool (figma)
1. have the lock symbol more prominent (TODO: decide how)


## App store
1. have tablet screenshots (tablet with fingerprint?)  

<br /><br /><br />

---
## Won't do
- Does not work, probably Android/Samsung specific: 
  on the Sperrscreen: always have the 3 standard android buttons visible (back/home/overview)
- Does not work due to sqlcipher usage:  
  I get this warning when publishing it: 
  "This App Bundle contains native code, and you've not uploaded debug symbols. We recommend that you upload a symbol file to make your crashes and ANRs easier to analyse and debug." and when I analyze my build, I don't have the BUNDLE-METADATA/com.android.tools.build.debugsymbols folder. NDK version 27.3.13750724 is installed.
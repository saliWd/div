# Prompt

## Refinement
This list is updated as soon as the stuff works, only the next few points (TODOs) are listed 
1. Tutorial: delete the newly created entry at the end of the tutorial. Do this also when the skip-tutorial button is pressed (catch the case where there is no entry to be deleted)
1. Tutorial: add a 1/6 counter to the title.  
1. Entry page: there is a green button on green background. Add a card around it.  
1. re-think the picture-in-picture mode. Not really a use case, app should close or go into the background.  
1. write a python script to automatically derive screen shots from all screens. Use the scrcpy tool to get them. Make use of the versionName (e.g. "2026.06.09") to derive the file name.  
1. rework the settings page, make it more clearly arranged. Maybe: change name and symbol to import/export. Maybe have the link to the import/export at the page where all entries are visible and thus no special symbol anymore on the main page?
1. clean up the code. Check for newer stable versions of dependencies and in general reduce the number of imports and dependencies.  
1. try out some different UIs / colors. TODO: maybe with another tool (figma)
1. have the lock symbol more prominent (TODO: decide how)


## Webpage
1. Refine it. Use the overview screenshot instead of the store-picture and use the Raleway font for the title. Change the texts.  


## GIT re-org
1. move the start folder


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
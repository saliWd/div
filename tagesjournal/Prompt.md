# Prompt

## Refinement
This list is updated as soon as the stuff works, only the next few points (TODOs) are listed
- should be fixed: -- fix this warning from publishing the app:
     1. "Edge-to-edge may not display for all users  
From Android 15, apps targeting SDK 35 will display edge-to-edge by default. Apps targeting SDK 35 should handle insets to make sure that their app displays correctly on Android 15 and later. Investigate this issue and allow time to test edge-to-edge and make the required updates. Alternatively, call enableEdgeToEdge() for Kotlin or EdgeToEdge.enable() for Java for backward compatibility."
     1. "Your app uses deprecated APIs or parameters for edge-to-edge  
     One or more of the APIs that you use or parameters that you set for edge-to-edge and window display have been deprecated in Android 15. Your app uses the following deprecated APIs or parameters:

    android.view.Window.setStatusBarColor
    android.view.Window.setNavigationBarColor
    LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

These start in the following places:

    r40.a
    o40.b
To fix this, migrate away from these APIs or parameters."


     1. Implement picture-in-picture to improve your app quality and user experience
- rework the settings page, change name and symbol to import/export and make it more clearly arranged.     
- add an introduction to the app at first startup which explains the functionality. This also needs a 'skip intro' button.
- try out some different UIs / colors. TODO: maybe with another tool (figma)
- have the lock symbol more prominent (TODO: decide how)


---
- Does not work, probably Android/Samsung specific: 
  on the Sperrscreen: always have the 3 standard android buttons visible (back/home/overview)

"""
=============================================================================
 Pico Display Pack 2.8" + Power Bank — Parametric Case
 Autodesk Fusion 360 Script  (Python API)
=============================================================================
 HOW TO RUN
 ----------
 1. Open Autodesk Fusion 360.
 2. Go to  UTILITIES → Scripts and Add-Ins  (or press Shift+S).
 3. Click the  "+"  next to "My Scripts", browse to this file and add it.
 4. Select "PicoDisplayCase" and click  RUN.
 5. Two bodies — "CaseBody" and "Lid" — will appear in the browser.
    Separate them and export each as STL for 3-D printing.

 DESIGN OVERVIEW
 ---------------
 The case is split into two printable parts:

   ┌─────────────────────────────────────┐  ← Lid  (3 mm thick, screen window)
   │  ┌─────────────────────────────┐    │
   │  │  Pico Display 2.8" facing   │    │    Upper pocket  22 mm deep
   │  │  up — Pico hangs below it   │    │    (display + Pico assembly)
   │  └─────────────────────────────┘    │
   ├──────────── shelf  ─────────────────┤  ← 2 mm divider  +  USB slot
   │                                     │
   │   Power bank  90 × 62 × 13 mm       │    Lower pocket  16 mm deep
   │                                     │
   └─────────────────────────────────────┘  ← 2 mm base

 Features
 --------
   • Upper pocket   : Pico Display assembly (centred, 76 × 50 × 22 mm)
   • Lower pocket   : Power bank (93 × 65 × 16 mm)
   • Shelf slot     : 20 × 12 mm cut-through so the USB-A → micro/USB-C cable
                      from the power bank routes cleanly up to the Pico
   • Charging slot  : 12 × 6 mm notch in the right-hand wall (lower section)
                      lets you charge the power bank in-situ
   • Lid window     : 59 × 45 mm opening aligned with the active screen area
   • Locating lip   : 4-sided frame on the underside of the lid drops into
                      the body and keeps everything aligned
   • No button access required (all buttons deliberately enclosed)

 DIMENSIONS (all in mm unless noted)
 ------------------------------------
   Outer case body : 97 × 69 × 42 mm
   Lid             : 97 × 69 ×  3 mm
   Wall thickness  : 2 mm
   Shelf thickness : 2 mm
   Tolerance       : 1.5 mm per side

 ADJUSTING PARAMETERS
 --------------------
   All key dimensions are constants at the top of run().
   Change them and re-run the script; the model rebuilds automatically.
=============================================================================
"""

import adsk.core
import adsk.fusion
import traceback


def run(context):
    ui = None
    try:
        app  = adsk.core.Application.get()
        ui   = app.userInterface
        des  = adsk.fusion.Design.cast(app.activeProduct)
        root = des.rootComponent

        sks  = root.sketches
        exts = root.features.extrudeFeatures
        cpls = root.constructionPlanes

        # Reference construction planes
        xy = root.xYConstructionPlane   # normal = +Z
        yz = root.yZConstructionPlane   # normal = +X

        # ================================================================
        #  PARAMETERS  (all values in centimetres — Fusion 360 internal)
        # ================================================================
        W   = 0.20   # wall / base / lid-lip wall thickness     =  2.0 mm

        # Power bank (user-supplied)
        PB_X = 9.0   # 90 mm  — runs along X axis
        PB_Y = 6.2   # 62 mm  — runs along Y axis
        PB_Z = 1.3   # 13 mm  — height

        # Pico Display assembly
        #   Board alone : 73 × 47 × 9.5 mm (includes socket headers)
        #   + Pico PCB  : ~3.5 mm extra below socket headers
        #   Total used  : 73 × 47 × 18 mm  (generous clearance)
        DP_X = 7.3   # 73 mm board length
        DP_Y = 4.7   # 47 mm board width
        DP_Z = 1.8   # 18 mm total assembly height

        # Active screen area window in lid
        SCR_X = 5.9  # 59 mm
        SCR_Y = 4.5  # 45 mm

        T    = 0.15  # tolerance / clearance per side             =  1.5 mm
        DIV  = 0.20  # internal shelf / divider thickness          =  2.0 mm
        LID  = 0.30  # lid panel thickness                         =  3.0 mm
        LIP  = 0.12  # locating lip drop height on lid underside   =  1.2 mm
        LIP_CLR = 0.04  # clearance on lip so it slides in easily  =  0.4 mm

        # USB cable routing slot through shelf (centre)
        SL_X = 2.0   # 20 mm  — wide enough for a flat cable or small plug
        SL_Y = 1.2   # 12 mm

        # Power-bank charging port slot (right wall, lower section)
        CH_W = 1.2   # 12 mm  — width of slot
        CH_H = 0.6   #  6 mm  — height of slot

        # ── Derived dimensions ────────────────────────────────
        IN_X   = PB_X + 2*T          # inner cavity width  (power bank drives)
        IN_Y   = PB_Y + 2*T          # inner cavity depth  (power bank drives)
        PBZ_IN = PB_Z + 2*T          # lower pocket height (with clearance)
        DPZ_IN = DP_Z + 4*T          # upper pocket height (extra headroom)

        OUT_X  = IN_X + 2*W          # outer case width
        OUT_Y  = IN_Y + 2*W          # outer case depth
        # Body height = base + lower pocket + shelf + upper pocket (open top)
        OUT_Z  = W + PBZ_IN + DIV + DPZ_IN

        # Z coordinate of the shelf's bottom face
        SHELF_Z = W + PBZ_IN

        # Display pocket: centred in the (larger) power-bank footprint
        DP_PKT_X = DP_X + 2*T         # display pocket width
        DP_PKT_Y = DP_Y + 2*T         # display pocket depth
        DP_X0 = W + (IN_X - DP_PKT_X) / 2   # left edge of display pocket
        DP_Y0 = W + (IN_Y - DP_PKT_Y) / 2   # front edge of display pocket

        # ================================================================
        #  HELPER FUNCTIONS
        # ================================================================

        def V(v):
            return adsk.core.ValueInput.createByReal(v)

        def P(x, y, z=0):
            return adsk.core.Point3D.create(x, y, z)

        def offset_plane(base, dist):
            """Return a new construction plane offset from *base* by *dist* cm."""
            inp = cpls.createInput()
            inp.setByOffset(base, V(dist))
            return cpls.add(inp)

        def rect_profile(plane, x0, y0, x1, y1):
            """Sketch a single rectangle on *plane* and return its profile."""
            s = sks.add(plane)
            s.sketchCurves.sketchLines.addTwoPointRectangle(P(x0, y0), P(x1, y1))
            return s.profiles.item(0)

        def ext_new(profile, dist):
            """Extrude *profile* by *dist* cm (positive = +normal) → new body."""
            inp = exts.createInput(
                profile,
                adsk.fusion.FeatureOperations.NewBodyFeatureOperation)
            inp.setOneSideExtent(
                adsk.fusion.DistanceExtentDefinition.create(V(abs(dist))),
                adsk.fusion.ExtentDirections.PositiveExtentDirection
                if dist >= 0
                else adsk.fusion.ExtentDirections.NegativeExtentDirection)
            return exts.add(inp)

        def ext_cut(profile, dist, body):
            """Cut *body* with *profile* extruded by *dist* cm."""
            inp = exts.createInput(
                profile,
                adsk.fusion.FeatureOperations.CutFeatureOperation)
            inp.setOneSideExtent(
                adsk.fusion.DistanceExtentDefinition.create(V(abs(dist))),
                adsk.fusion.ExtentDirections.PositiveExtentDirection
                if dist >= 0
                else adsk.fusion.ExtentDirections.NegativeExtentDirection)
            inp.participantBodies = [body]
            return exts.add(inp)

        def ext_join(profile, dist, body):
            """Join an extrusion of *profile* by *dist* cm into *body*."""
            inp = exts.createInput(
                profile,
                adsk.fusion.FeatureOperations.JoinFeatureOperation)
            inp.setOneSideExtent(
                adsk.fusion.DistanceExtentDefinition.create(V(abs(dist))),
                adsk.fusion.ExtentDirections.PositiveExtentDirection
                if dist >= 0
                else adsk.fusion.ExtentDirections.NegativeExtentDirection)
            inp.participantBodies = [body]
            return exts.add(inp)

        # ================================================================
        #  BODY
        # ================================================================

        # ── 1. Outer solid ──────────────────────────────────
        body_feat = ext_new(rect_profile(xy, 0, 0, OUT_X, OUT_Y), OUT_Z)
        B = body_feat.bodies.item(0)
        B.name = "CaseBody"

        # ── 2. Lower cavity — power bank pocket ─────────────
        #   Starts at z = W (above base wall) and extends to z = W + PBZ_IN
        ext_cut(rect_profile(xy, W, W, W + IN_X, W + IN_Y),
                W + PBZ_IN, B)

        # ── 3. Upper cavity — Pico Display assembly pocket ──
        #   Centred over the lower cavity; starts above the shelf
        upper_pl = offset_plane(xy, SHELF_Z + DIV)
        ext_cut(rect_profile(upper_pl, DP_X0, DP_Y0,
                              DP_X0 + DP_PKT_X, DP_Y0 + DP_PKT_Y),
                DPZ_IN, B)

        # ── 4. USB cable routing slot through shelf ──────────
        #   Rectangular slot centred in X/Y, passes through the full DIV
        shelf_pl = offset_plane(xy, SHELF_Z)
        ext_cut(
            rect_profile(shelf_pl,
                         OUT_X / 2 - SL_X / 2, OUT_Y / 2 - SL_Y / 2,
                         OUT_X / 2 + SL_X / 2, OUT_Y / 2 + SL_Y / 2),
            DIV, B)

        # ── 5. Power-bank charging port slot (right / +X wall) ──
        #   Centre of the right wall, vertically centred in the lower pocket
        right_pl = offset_plane(yz, OUT_X)   # plane at X = OUT_X, normal = +X
        cz = W + PBZ_IN / 2                  # Z centre of lower pocket
        ext_cut(
            rect_profile(right_pl,
                         OUT_Y / 2 - CH_W / 2, cz - CH_H / 2,
                         OUT_Y / 2 + CH_W / 2, cz + CH_H / 2),
            -W, B)   # cut inward (−X direction, magnitude = wall thickness)

        # ================================================================
        #  LID
        # ================================================================

        lid_pl = offset_plane(xy, OUT_Z)   # sits on top of the body

        # ── 6. Lid solid ─────────────────────────────────────
        lid_feat = ext_new(rect_profile(lid_pl, 0, 0, OUT_X, OUT_Y), LID)
        L = lid_feat.bodies.item(0)
        L.name = "Lid"

        # ── 7. Screen window opening ─────────────────────────
        #   Centred in the lid plate; aligned with the active screen area
        ext_cut(
            rect_profile(lid_pl,
                         OUT_X / 2 - SCR_X / 2, OUT_Y / 2 - SCR_Y / 2,
                         OUT_X / 2 + SCR_X / 2, OUT_Y / 2 + SCR_Y / 2),
            LID, L)

        # ── 8. Locating lip (underside of lid) ──────────────
        #   Four thin rectangular strips form a frame that drops into the
        #   open top of the body and locates the lid precisely.
        #   The lip's outer edge is set back from the body's inner wall by
        #   LIP_CLR so it slides in without force.
        lip_x0 = W + LIP_CLR
        lip_x1 = OUT_X - W - LIP_CLR
        lip_y0 = W + LIP_CLR
        lip_y1 = OUT_Y - W - LIP_CLR
        lip_w  = W * 0.8   # strip width  ≈ 1.6 mm

        # Front strip  (−Y side)
        ext_join(rect_profile(lid_pl, lip_x0, lip_y0, lip_x1, lip_y0 + lip_w),
                 -LIP, L)
        # Back strip   (+Y side)
        ext_join(rect_profile(lid_pl, lip_x0, lip_y1 - lip_w, lip_x1, lip_y1),
                 -LIP, L)
        # Left strip   (−X side)
        ext_join(rect_profile(lid_pl, lip_x0, lip_y0 + lip_w,
                               lip_x0 + lip_w, lip_y1 - lip_w),
                 -LIP, L)
        # Right strip  (+X side)
        ext_join(rect_profile(lid_pl, lip_x1 - lip_w, lip_y0 + lip_w,
                               lip_x1, lip_y1 - lip_w),
                 -LIP, L)

        # ================================================================
        #  SUMMARY
        # ================================================================
        summary = (
            "✓  Pico Display Case — generated!\n"
            "═══════════════════════════════════════\n\n"

            "  CaseBody\n"
            f"  Outer  {OUT_X*10:.1f} × {OUT_Y*10:.1f} × {OUT_Z*10:.1f} mm\n"
            f"  Lower pocket   {IN_X*10:.1f} × {IN_Y*10:.1f} × {PBZ_IN*10:.1f} mm"
            "  ← power bank\n"
            f"  Upper pocket   {DP_PKT_X*10:.1f} × {DP_PKT_Y*10:.1f} × {DPZ_IN*10:.1f} mm"
            "  ← Pico Display\n"
            f"  USB cable slot {SL_X*10:.0f} × {SL_Y*10:.0f} mm  (through shelf)\n"
            f"  Charging slot  {CH_W*10:.0f} × {CH_H*10:.0f} mm  (right wall)\n\n"

            "  Lid\n"
            f"  Outer  {OUT_X*10:.1f} × {OUT_Y*10:.1f} × {LID*10:.1f} mm\n"
            f"  Screen window  {SCR_X*10:.1f} × {SCR_Y*10:.1f} mm  (centred)\n"
            f"  Locating lip   {LIP*10:.1f} mm drop  (underside frame)\n\n"

            "NOTES\n"
            "• All tolerances = 1.5 mm per side.\n"
            "• Modify the constants at the top of run() and re-run\n"
            "  to regenerate with different dimensions.\n"
            "• Export each body as STL for 3-D printing.\n"
            "• Print CaseBody with the base on the build plate.\n"
            "• Print Lid face-down (window side down) for best surface finish.\n"
            "• A dab of friction-fit or an M2 screw through each corner\n"
            "  will secure the lid permanently if needed."
        )
        ui.messageBox(summary, "Pico Display Case — Done")

    except Exception:  # noqa: BLE001
        if ui:
            ui.messageBox("Script failed:\n\n" + traceback.format_exc(),
                          "Error", adsk.core.MessageBoxButtonTypes.OKButtonType,
                          adsk.core.MessageBoxIconTypes.CriticalIconType)

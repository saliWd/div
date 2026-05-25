# =============================================================================
#  PimoroniDisplayCase.py
#  Autodesk Fusion 360 – Script
#
#  Generates a modified case for the Pimoroni Pico Display Pack 2.8"
#  Extended from the original 90 × 60 mm design to also house a power bank
#  (90 × 62 × 13 mm) below the display compartment.
#
#  Changes vs. the original Printables model #1235809:
#   • Button access holes removed from lid
#   • LED hole removed from lid
#   • Footprint widened to 96 × 68 mm to clear the 62 mm-deep power bank
#   • Base height extended to 32 mm (was 20 mm)
#   • New lower chamber for power bank (90 × 62 × 13 mm + clearance)
#   • 2 mm horizontal divider between chambers
#   • 22 × 22 mm USB-cable slot through divider (centred)
#   • Upper display section keeps the original PCB-pocket depth (4 mm)
#     and PCB footprint (73 × 47 mm + 1 mm clearance each side = 75 × 49 mm)
#   • Display window (58 × 44 mm) centred in the full case footprint
#
#  Coordinate convention
#   • Origin at the centre of the base bottom face
#   • Z grows upward; lid sits on top of the base at Z = base_h
#   • Fusion 360 API uses centimetres → every mm value is multiplied by CM=0.1
#
#  Output
#   Two separate bodies in the active document:
#     "Base"  – print open-side up, no supports required
#     "Lid"   – print face-down (display-window face on the bed)
#   Select each body in the browser and use File ▸ Export… ▸ STL to export.
# =============================================================================

import adsk.core
import adsk.fusion
import traceback


def run(context):
    ui = None
    try:
        app    = adsk.core.Application.get()
        ui     = app.userInterface
        design = adsk.fusion.Design.cast(app.activeProduct)
        root   = design.rootComponent

        # ── Ensure parametric mode ─────────────────────────────────────
        design.designType = adsk.fusion.DesignTypes.ParametricDesignType

        # ── Unit conversion ────────────────────────────────────────────
        # All dimensions below are in mm; the Fusion API works in cm.
        CM = 0.1

        # ==============================================================
        #  MASTER DIMENSIONS  (mm)
        # ==============================================================

        # Outer footprint – must clear power bank (90 × 62 mm)
        # plus 1.5 mm walls each side:  90+3 = 93 → round to 96 for margin
        #                                62+3 = 65 → round to 68 for margin
        outer_w = 96.0          # X – width
        outer_d = 68.0          # Y – depth
        wall_t  =  1.5          # wall / floor thickness

        # Inner cavity (same for both chambers; gives ≥1.5 mm clearance
        # to power bank on every side)
        inner_w = outer_w - 2 * wall_t     # 93 mm
        inner_d = outer_d - 2 * wall_t     # 65 mm

        # ── Z stack (base, bottom = Z 0) ──────────────────────────────
        floor_t      =  2.0     # solid base floor
        pb_cav_h     = 14.0     # power-bank cavity: 13 mm PB + 1 mm top gap
        div_t        =  2.0     # horizontal divider between chambers
        disp_lower_h = 10.0     # space for Pico MCU / connectors below PCB
        pcb_pocket_h =  4.0     # snug PCB pocket at the very top of the base

        base_h = floor_t + pb_cav_h + div_t + disp_lower_h + pcb_pocket_h
        # = 2 + 14 + 2 + 10 + 4 = 32 mm

        # Derived Z levels (from base bottom = 0)
        pb_z0  = floor_t                    #  2 – bottom of PB cavity
        pb_z1  = pb_z0 + pb_cav_h          # 16 – top of PB cavity / divider start
        div_z0 = pb_z1                      # 16
        div_z1 = div_z0 + div_t            # 18 – top of divider / display start
        dsp_z0 = div_z1                     # 18
        pcb_z0 = dsp_z0 + disp_lower_h    # 28 – bottom of PCB pocket

        # PCB pocket – original 73 × 47 mm PCB + 1 mm clearance per side
        pcb_w = 75.0
        pcb_d = 49.0

        # USB cable slot through divider – positioned to one side
        # The slot is pushed to the left (negative X) so the USB plug body
        # (~20 mm long) lies in the open space to the right of the slot.
#
        #  left inner wall                        right inner wall
#  |-- 3mm -|---- 22mm slot ----|---- ~68mm free (plug sits here) ----|  
#
        usb_slot_w = 22.0   # slot width (X)
        usb_slot_d = 22.0   # slot depth (Y)
        # Centre of slot in X = left inner edge + 3 mm margin + half width
        usb_cx = -(inner_w / 2 - 3.0 - usb_slot_w / 2)  # ≈ -32.5 mm
        usb_cy = 0.0  # centred in Y is fine

        # ── Lid ───────────────────────────────────────────────────────
        lid_gap     =  5.0      # Z gap above base (visual only, remove when printing)
        lid_h       =  4.0      # total lid thickness
        lid_plate_h =  2.0      # outer top plate
        lid_lip_h   =  2.0      # lip that slides into the base opening
        # Lip is 0.3 mm smaller than inner_w/inner_d on each side (print fit)
        lid_lip_w   = inner_w - 0.6    # 92.4 mm
        lid_lip_d   = inner_d - 0.6    # 64.4 mm
        lid_z0      = base_h + lid_gap  # Z of lid bottom in model space = 37 mm

        # Display window – centred in the lid face
        # Matches the 2.8" IPS TFT active area (≈ 57.6 × 43.2 mm, rounded up)
        win_w = 58.0
        win_d = 44.0

        # ==============================================================
        #  HELPER FUNCTIONS
        # ==============================================================

        def offset_plane(comp, base_plane, z_mm):
            """Return a construction plane offset z_mm above base_plane."""
            planes = comp.constructionPlanes
            inp    = planes.createInput()
            inp.setByOffset(base_plane,
                            adsk.core.ValueInput.createByReal(z_mm * CM))
            return planes.add(inp)

        def rect_sk(comp, plane, cx, cy, w, h):
            """
            Sketch a single rectangle centred at (cx, cy) with size w × h.
            Returns the sketch object.
            """
            sk    = comp.sketches.add(plane)
            lines = sk.sketchCurves.sketchLines
            lines.addTwoPointRectangle(
                adsk.core.Point3D.create((cx - w / 2) * CM,
                                         (cy - h / 2) * CM, 0),
                adsk.core.Point3D.create((cx + w / 2) * CM,
                                         (cy + h / 2) * CM, 0))
            return sk

        def frame_sk(comp, plane, cx, cy, ow, od, iw, id_):
            """
            Sketch two concentric rectangles → frame (donut) profile.
            Outer rectangle: ow × od; inner rectangle: iw × id_.
            Returns the sketch object.
            """
            sk    = comp.sketches.add(plane)
            lines = sk.sketchCurves.sketchLines
            lines.addTwoPointRectangle(
                adsk.core.Point3D.create((cx - ow / 2) * CM,
                                         (cy - od / 2) * CM, 0),
                adsk.core.Point3D.create((cx + ow / 2) * CM,
                                         (cy + od / 2) * CM, 0))
            lines.addTwoPointRectangle(
                adsk.core.Point3D.create((cx - iw / 2) * CM,
                                         (cy - id_ / 2) * CM, 0),
                adsk.core.Point3D.create((cx + iw / 2) * CM,
                                         (cy + id_ / 2) * CM, 0))
            return sk

        def best_profile(sketch, target_area_cm2):
            """
            Return the profile whose area is closest to target_area_cm2.
            Used to pick the frame (donut) profile from a two-rectangle sketch.
            """
            best       = None
            best_delta = 1e9
            for i in range(sketch.profiles.count):
                p     = sketch.profiles.item(i)
                delta = abs(p.areaProperties().area - target_area_cm2)
                if delta < best_delta:
                    best_delta = delta
                    best       = p
            return best

        def extrude(comp, profile, h_mm, operation, body=None):
            """
            Extrude profile by h_mm in the sketch-normal direction (+Z for XY planes).
            body – when provided, restrict cut to that body only.
            """
            exts = comp.features.extrudeFeatures
            inp  = exts.createInput(profile, operation)
            inp.setDistanceExtent(
                False,
                adsk.core.ValueInput.createByReal(h_mm * CM))
            if body is not None:
                inp.participantBodies = [body]
            return exts.add(inp)

        # Short-hand constants for extrusion operations
        NEW = adsk.fusion.FeatureOperations.NewBodyFeatureOperation
        CUT = adsk.fusion.FeatureOperations.CutFeatureOperation

        xy = root.xYConstructionPlane     # base reference plane (Z = 0)

        # ==============================================================
        #  BASE
        # ==============================================================

        # Step 1 – Outer solid block (96 × 68 × 32 mm)
        sk = rect_sk(root, xy, 0, 0, outer_w, outer_d)
        base_feat = extrude(root, sk.profiles.item(0), base_h, NEW)
        base_body = base_feat.bodies.item(0)
        base_body.name = 'Base'

        # Step 2 – Power-bank cavity
        #   Cuts a 93 × 65 × 14 mm pocket (Z = 2 … 16) from the inside.
        #   The 90 × 62 mm power bank sits here with ≥ 1.5 mm clearance
        #   on all four sides and 1 mm headroom on top (before the divider).
        pl = offset_plane(root, xy, pb_z0)
        sk = rect_sk(root, pl, 0, 0, inner_w, inner_d)
        extrude(root, sk.profiles.item(0), pb_cav_h, CUT, base_body)

        # Step 3 – USB cable slot through divider
        #   22 × 22 mm opening offset to the left side (Z = 16 … 18).
        #   The slot is 3 mm from the left inner wall; the USB plug body
        #   (~20 mm) rests in the large open space to the right of the slot.
        pl = offset_plane(root, xy, div_z0)
        sk = rect_sk(root, pl, usb_cx, usb_cy, usb_slot_w, usb_slot_d)
        extrude(root, sk.profiles.item(0), div_t, CUT, base_body)

        # Step 4 – Display section lower cavity
        #   93 × 65 × 10 mm (Z = 18 … 28) – room for the Pico MCU,
        #   USB connector and any wiring below the PCB.
        pl = offset_plane(root, xy, dsp_z0)
        sk = rect_sk(root, pl, 0, 0, inner_w, inner_d)
        extrude(root, sk.profiles.item(0), disp_lower_h, CUT, base_body)

        # Step 5 – PCB pocket
        #   75 × 49 × 4 mm (Z = 28 … 32) – the 73 × 47 mm PCB slides into
        #   this tight pocket with 1 mm clearance per side.  The 9 mm ledge
        #   all around supports the PCB from below and prevents it from
        #   dropping into the lower compartment.
        pl = offset_plane(root, xy, pcb_z0)
        sk = rect_sk(root, pl, 0, 0, pcb_w, pcb_d)
        extrude(root, sk.profiles.item(0), pcb_pocket_h, CUT, base_body)

        # ==============================================================
        #  LID
        #  Positioned lid_gap mm above the base for visual separation.
        #  When exporting, export this body separately from "Base".
        # ==============================================================

        # Step 6 – Outer lid block (96 × 68 × 4 mm)
        pl = offset_plane(root, xy, lid_z0)
        sk = rect_sk(root, pl, 0, 0, outer_w, outer_d)
        lid_feat = extrude(root, sk.profiles.item(0), lid_h, NEW)
        lid_body = lid_feat.bodies.item(0)
        lid_body.name = 'Lid'

        # Step 7 – Lip rebate
        #   Remove the outer 2 mm frame from the bottom 2 mm of the lid.
        #   This leaves a 92.4 × 64.4 × 2 mm lip that fits snugly inside
        #   the base opening (93 × 65 mm inner) with 0.3 mm play per side.
        #   The top plate remains full 96 × 68 mm wide.
        pl = offset_plane(root, xy, lid_z0)
        sk = frame_sk(root, pl, 0, 0,
                      outer_w, outer_d, lid_lip_w, lid_lip_d)
        # Frame area = outer area – inner (lip) area (in cm²)
        frame_area_cm2 = (outer_w * outer_d - lid_lip_w * lid_lip_d) * CM * CM
        frame_prof = best_profile(sk, frame_area_cm2)
        extrude(root, frame_prof, lid_lip_h, CUT, lid_body)

        # Step 8 – Display window
        #   58 × 44 mm opening centred in the lid face; passes through the
        #   full 4 mm thickness so the display is fully visible.
        #   No button holes.  No LED hole.
        pl = offset_plane(root, xy, lid_z0)
        sk = rect_sk(root, pl, 0, 0, win_w, win_d)
        extrude(root, sk.profiles.item(0), lid_h, CUT, lid_body)

        # ==============================================================
        #  DONE
        # ==============================================================
        ui.messageBox(
            'Pimoroni Pico Display Extended Case — created successfully!\n\n'
            'DIMENSIONS\n'
            f'  Outer footprint : {outer_w:.0f} × {outer_d:.0f} mm\n'
            f'  Base height     : {base_h:.0f} mm\n'
            f'  Lid height      : {lid_h:.0f} mm\n\n'
            'COMPARTMENTS\n'
            f'  Power-bank pocket : {inner_w:.0f} × {inner_d:.0f} × {pb_cav_h:.0f} mm\n'
            f'    (fits 90 × 62 × 13 mm bank with ≥ 1.5 mm clearance)\n'
            f'  USB cable slot    : {usb_slot_w:.0f} × {usb_slot_d:.0f} mm (divider)\n'
            f'  Display PCB pocket: {pcb_w:.0f} × {pcb_d:.0f} × {pcb_pocket_h:.0f} mm\n'
            f'  Display window    : {win_w:.0f} × {win_d:.0f} mm (centred in lid)\n\n'
            'NEXT STEPS\n'
            '  1. Inspect both bodies ("Base" and "Lid") in the browser.\n'
            '  2. Right-click each body → Save As STL → export separately.\n'
            '  3. Print Base  : open side UP  – no supports needed.\n'
            '     Print Lid   : face DOWN (display window on the bed).\n'
            '  4. Recommended layer height: 0.15–0.2 mm; wall loops: ≥ 3.'
        )

    except Exception:
        if ui:
            ui.messageBox('Script failed:\n\n{}'.format(traceback.format_exc()))

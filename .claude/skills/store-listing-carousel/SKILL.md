---
name: store-listing-carousel
description: Maintain the Apk Analyzer Play Store screenshot carousel templates (docs/store/graphics/carousel_template.html and carousel_template_dark.html) and export their frames as upload-ready PNGs (docs/store/graphics/phone-screenshots/day and night). Covers two independent workflows — editing (swap in new screenshots after a UI change, refresh light/dark, add/remove/reorder a frame) and exporting (render the current frames to individual PNGs for Play Store upload) — either can run without the other. Triggered by phrases like "update the carousel", "refresh the store screenshots", "new Play Store screenshots", "carousel template", "export store screenshots", "generate store images", "render the carousel to PNGs" — even if the user doesn't name this skill explicitly.
---

# Store Listing Carousel

Maintains the two Apk Analyzer Play Store screenshot carousels —
`docs/store/graphics/carousel_template.html` (light) and `carousel_template_dark.html` (dark) — each
a single self-contained HTML file with 8 phone-frame "slides." Every frame shows a real app
screenshot behind a styled device frame, with a themed background, a headline, and a shared
"skyline" motif (rounded-arch + antenna dots, derived from the app icon) that ties all frames into
one visual system.

This skill covers two independent workflows that share the same templates but don't depend on each
other — run either one without the other:

- **[Editing](#workflow-a-editing--swap-in-a-new-batch-of-screenshots)**: swap new device screenshots
  into the templates (crop → verify color → embed → QA). It never touches
  `phone-screenshots/`.
- **[Exporting](#workflow-b-exporting--render-frames-to-upload-ready-pngs)**: render whatever the
  templates currently show into individual PNGs at `docs/store/graphics/phone-screenshots/{day,night}/`,
  ready for Play Store upload. It's read-only against the templates — it never edits them, and doesn't
  require having just run the edit workflow first.

Both files render at CSS scale (`.frame` is `360×640`) — that's a review resolution, not the upload
resolution produced by the export workflow.

## Before touching anything

Read the template(s) being updated and confirm the current frame count and order:

```bash
grep -n '<!-- FRAME' docs/store/graphics/carousel_template.html
```

The `<!-- FRAME N — Name -->` HTML comments are the only order marker in the file (there are no
on-canvas debug labels — those were stripped from the shipped version and shouldn't be
reintroduced unless the user explicitly asks for them). Keep the comments sequential and accurate
whenever you add, remove, or reorder a frame — a previous edit left them out of order (`1,2,3,4,4,
6,4,5`), which silently broke the invariant this skill relies on; that's now fixed, so don't
reintroduce the drift.

Do not restructure the shared CSS system (`.frame`, `.device`, `.screen`, `.skyline`, `.headtext`,
theme classes) unless asked. The job is almost always: swap screenshots in, keep everything else.

## The 8-frame map

Current locked structure for both templates. Preserve this order and theme unless the user asks to
change it — Play Store allows up to 8 screenshots, and this set is already at that cap.

| # | Feature | Theme class | Headline | Eyebrow | `alt` text |
|---|---------|-------------|----------|---------|------------|
| 1 | App Report (detail overview) | `theme-teal` | "See exactly / what's inside." | full inspection | `App report overview` |
| 2 | Search & Filter | `theme-teal` | "Find any app / in seconds." | search & filter | `Search and filter apps` |
| 3 | Permissions | `theme-tealLight` | "Every permission, / plainly explained." | permission audit | `Permissions audit` |
| 4 | Browse by Attribute | `theme-browse` | "Flip the question / around." | browse by attribute | `Browse by attribute` |
| 5 | Certificates | `theme-cream` | "Verify who / really signed it." | signing & trust | `Certificate details and fingerprints` |
| 6 | Manifest | `theme-manifest` | "The full manifest. / Decoded, exportable." | raw manifest | `Raw AndroidManifest.xml` |
| 7 | AI Summary | `theme-violet` | "Plain-language / summaries. On-device." | on-device AI | `On-device AI summary` |
| 8 | Export & Share | `theme-clay` | "Save, export, / no root needed." | export & share | `Export and share actions` |

Each frame's screenshot is embedded via:

```html
<img class="real-shot" src="data:image/png;base64,<...>" alt="<descriptive alt text>">
```

Target a specific frame for replacement by matching on `alt`, not on position in the file — edits
can reorder frames, and `alt` is the stable identifier.

## Workflow A: Editing — swap in a new batch of screenshots

Do the light and dark templates as two separate passes. They take different source screenshots
(light-mode vs. dark-mode device captures) and a different `.screen` background (`#f7f7f7` vs.
`#000000`) — don't reuse a light-mode crop in the dark file or vice versa.

This environment has no Python/PIL and no ImageMagick (`convert` on `PATH` resolves to the Windows
disk-conversion utility, not ImageMagick) — every step below uses PowerShell's built-in
`System.Drawing`, which needs no install.

### 1. Collect and sanity-check the screenshots

Confirm you have one screenshot per frame you're updating (up to 8), and check resolution
consistency:

```powershell
Add-Type -AssemblyName System.Drawing
Get-ChildItem new_*.png | ForEach-Object {
    $img = [System.Drawing.Image]::FromFile($_.FullName)
    "$($_.Name): $($img.Width)x$($img.Height)"
    $img.Dispose()
}
```

If resolutions differ from the previous batch, the crop height (next step) must be re-verified —
don't reuse a prior crop value blindly.

### 2. Determine the status-bar crop height

Status bar height varies by device skin, font size, and OS version — never assume a fixed pixel
value from a prior session. Read one raw screenshot directly with the Read tool to eyeball it, or
cut a top strip first if you need a closer look:

```powershell
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile("new_frame1.png")
$strip = New-Object System.Drawing.Bitmap($src.Width, 220)
$g = [System.Drawing.Graphics]::FromImage($strip)
$rect = New-Object System.Drawing.Rectangle(0, 0, $src.Width, 220)
$g.DrawImage($src, $rect, $rect, [System.Drawing.GraphicsUnit]::Pixel)
$strip.Save("_status_bar_check.png")
$g.Dispose(); $strip.Dispose(); $src.Dispose()
```

Then Read `_status_bar_check.png` and read off where the status bar icons end and real content
(back arrow, title, etc.) begins. Re-verify per batch — don't assume a value from a previous
session still holds for a different device skin or OS version.

### 3. Crop every screenshot and sample the background color

```powershell
Add-Type -AssemblyName System.Drawing
$cropTop = 140  # <- set from step 2

Get-ChildItem new_*.png | ForEach-Object {
    $src = [System.Drawing.Image]::FromFile($_.FullName)
    $h = $src.Height - $cropTop
    $bmp = New-Object System.Drawing.Bitmap($src.Width, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $srcRect = New-Object System.Drawing.Rectangle(0, $cropTop, $src.Width, $h)
    $dstRect = New-Object System.Drawing.Rectangle(0, 0, $src.Width, $h)
    $g.DrawImage($src, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    $bmp.Save("cropped_$($_.Name)")
    $halfWidth = $src.Width / 2
    $px = $bmp.GetPixel([int]$halfWidth, 5)
    "{0} -> {1}x{2}  #{3:X2}{4:X2}{5:X2}" -f $_.Name, $src.Width, $h, $px.R, $px.G, $px.B
    $g.Dispose(); $bmp.Dispose(); $src.Dispose()
}
```

**Check that every sampled color matches** (they should all be identical — either the light
`#f7f7f7` background token or the dark `#000000` token, per `ApkAnalyzerColorPalette` in
[`Color.kt`](../../../core/ui-library/src/main/kotlin/sk/styk/martin/apkanalyzer/core/uilibrary/theme/Color.kt)).
If one screenshot samples differently, that screen likely has a non-standard top background (e.g. a
colored app bar) — flag it to the user rather than silently forcing a mismatched gap color.

### 4. Update the notch-gap and screen background to match

`.screen` sets the base background and `.real-shot` is absolutely positioned with a fixed `top:30px`
gap so the device notch never overlaps real content:

```css
.screen{ background:#f7f7f7; /* or #000000 for dark */ ... }
.real-shot{
  position:absolute; top:30px; left:0;
  width:100%; height:calc(100% - 30px);
  object-fit:cover; object-position:top center;
}
```

If switching between light/dark or onto a new palette, update `.screen`'s `background` to the
sampled color from step 3 — do not leave it mismatched, even by a shade, or the notch gap will show
a visible seam.

### 5. Base64-encode and embed by alt-text match

Keep referencing images by relative file path out of the templates — a self-contained HTML file
stays reviewable in a browser or an Artifact without carrying sibling image files alongside it in
the repo, and it's how the two templates already ship. Always inline as base64:

```powershell
$templatePath = "docs/store/graphics/carousel_template.html"  # or carousel_template_dark.html
$html = Get-Content -Raw $templatePath

$altToFile = @{
    'App report overview'                  = 'cropped_new_frame1.png'
    'Search and filter apps'               = 'cropped_new_frame2.png'
    'Permissions audit'                    = 'cropped_new_frame3.png'
    'Browse by attribute'                  = 'cropped_new_frame4.png'
    'Certificate details and fingerprints' = 'cropped_new_frame5.png'
    'Raw AndroidManifest.xml'              = 'cropped_new_frame6.png'
    'On-device AI summary'                 = 'cropped_new_frame7.png'
    'Export and share actions'             = 'cropped_new_frame8.png'
}

$pattern = '(?s)(<img class="real-shot" src="data:image/png;base64,)([^"]+)(" alt="([^"]+)">)'
$evaluator = {
    param($m)
    $alt = $m.Groups[4].Value
    if ($altToFile.ContainsKey($alt)) {
        $bytes = [IO.File]::ReadAllBytes($altToFile[$alt])
        $b64 = [Convert]::ToBase64String($bytes)
        return $m.Groups[1].Value + $b64 + $m.Groups[3].Value
    }
    return $m.Value
}
$html = [regex]::Replace($html, $pattern, $evaluator)

[IO.File]::WriteAllText($templatePath, $html, (New-Object System.Text.UTF8Encoding($false)))
```

Only frames present in `$altToFile` get touched — everything else in the template (headlines,
themes, untouched frames) is preserved exactly. Write with a BOM-less `UTF8Encoding` explicitly —
`Set-Content`/`Out-File` in Windows PowerShell 5.1 don't reliably round-trip the file's emoji and
unicode punctuation otherwise.

### 6. Reconcile theme colors against the new screenshots (if content changed)

If a screenshot's own accent color changed (e.g. a redesigned AI-summary card, a new brand color),
update the surrounding frame theme to match it rather than leave it clashing. Re-read
[`Color.kt`](../../../core/ui-library/src/main/kotlin/sk/styk/martin/apkanalyzer/core/uilibrary/theme/Color.kt)
for the current tokens rather than trusting a cached value — this table was correct as of the last
verification, but the source file is canonical:

**Light**: `primary #006766` · `primaryContainer #B2EBEA` · `secondary #607C7B` ·
`tertiary #894C2D` / `#FFDBC9` · `aiAccent #7C4DFF` / `#EDE7FE`

**Dark**: `primary #86D4D2` · `primaryContainer #1A3F3E` · `secondary #607C7B` ·
`aiAccent #B39DFF` / `#2E2251`

The AI Summary frame (`theme-violet`) in particular must use the dark `aiAccent` tokens when showing
a dark-mode screenshot — using the light-mode violet next to a dark-mode purple card reads as two
different, clashing purples.

### 7. QA before calling it done

Open the updated HTML in a browser and check, per frame:

- [ ] No visible seam between the notch gap and the screenshot's real background
- [ ] Headline text (max 2 lines) doesn't overlap the device frame
- [ ] Cropped screenshot didn't lose the specific content the frame is meant to prove (e.g. don't
      let a top-anchored crop cut off the one stat that sells the frame — check `object-position`
      isn't hiding it)
- [ ] Exactly 8 frames total (or fewer, never more — Play Store's hard cap)
- [ ] Status bar clocks/times are gone from every frame (cropped in step 3)
- [ ] `<!-- FRAME N -->` comments are still sequential 1–8 after any reorder

The finished files are already in their real location (`docs/store/graphics/`) — there's no separate
delivery step. Tell the user which frames changed and in which of the two templates.

## Workflow B: Exporting — render frames to upload-ready PNGs

Purely reads the templates and writes PNGs — never edits `carousel_template.html` or
`carousel_template_dark.html`. Output goes to
`docs/store/graphics/phone-screenshots/day/` (from the light template) and
`.../phone-screenshots/night/` (from the dark template), one file per frame, named from the
[8-frame map](#the-8-frame-map)'s slug: `01-app-report.png`, `02-search-filter.png`,
`03-permissions.png`, `04-browse-attribute.png`, `05-certificates.png`, `06-manifest.png`,
`07-ai-summary.png`, `08-export-share.png`.

**Target format** (Play Store phone screenshot requirements): PNG, ≤8 MB, 9:16 or 16:9 aspect ratio,
each side between 320 px and 3,840 px. The frames are natively 9:16 (`360×640` at CSS scale), so this
falls out automatically as long as you scale both dimensions together — never crop to force the
ratio.

This environment has no headless HTML-to-PNG renderer (no Puppeteer/Playwright, no ImageMagick —
`convert` on `PATH` resolves to the Windows disk-conversion utility, not ImageMagick) and no
Python/PIL. Rendering goes through the `claude-in-chrome` browser tools plus PowerShell's built-in
`System.Drawing` for the pixel work — load the chrome tools once via
`ToolSearch("select:mcp__claude-in-chrome__tabs_context_mcp,mcp__claude-in-chrome__navigate,mcp__claude-in-chrome__computer,mcp__claude-in-chrome__javascript_tool,mcp__claude-in-chrome__browser_batch,mcp__claude-in-chrome__tabs_create_mcp,mcp__claude-in-chrome__tabs_close_mcp")`
before starting.

### Why this isn't a single screenshot per frame

Two constraints, discovered by testing this pipeline directly rather than assumed:

1. **`file://` is blocked.** The `claude-in-chrome` extension refuses to navigate to `file://` URLs
   ("Can't interact with browser-internal or unparseable URLs") — it won't read arbitrary local
   files. Serve the templates' directory over local HTTP first.
2. **The browser window can't exceed the physical screen.** `resize_window` silently clamps to
   whatever the monitor allows — asking for `1120×2040` on a 1920×1080 display came back as
   `1920×957` (screen height minus window chrome). A frame rendered at a scale large enough to look
   like a real phone screenshot (e.g. 3× CSS zoom → `1080×1920`) is therefore *taller than the
   viewport can ever show at once*, on any normal monitor. The fix is to capture each frame in two
   vertical slices and stitch them — not to hunt for a bigger window.

### 1. Serve the templates directory over local HTTP

```bash
node -e "
const http = require('http');
const fs = require('fs');
const path = require('path');
http.createServer((req, res) => {
  const filePath = path.join(__dirname, decodeURIComponent(req.url.split('?')[0]));
  fs.readFile(filePath, (err, data) => {
    if (err) { res.writeHead(404); res.end('not found'); return; }
    res.writeHead(200, {'Content-Type': 'text/html'});
    res.end(data);
  });
}).listen(8743, '127.0.0.1', () => console.log('listening'));
" &
```

Run it from `docs/store/graphics/` (or pass that as `__dirname` some other way) and confirm it's up
before touching the browser:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8743/carousel_template.html
```

Kill the `node` process (matched by the port in its command line) once every export is done — don't
leave a stray server running past this session.

### 2. Open a tab and measure the real viewport

```
tabs_context_mcp { createIfEmpty: true }
navigate  -> http://127.0.0.1:8743/carousel_template.html
javascript_tool -> window.innerWidth, window.innerHeight (after the page has settled)
```

Don't hardcode a prior session's viewport height — screen resolution and OS chrome differ by
machine. This session measured `1920×957`; recompute per session.

### 3. Isolate one frame at a fixed, padding-free origin

For each frame index `i` (0–7, in the [8-frame map](#the-8-frame-map) order), navigate fresh and run:

```javascript
document.body.style.padding = '0';           // removes body's 40px padding entirely
document.body.style.zoom = '2.9';             // scale factor — see "Picking the zoom factor" below
const frames = [...document.querySelectorAll('.frame')];
frames.forEach((f, idx) => { if (idx !== i) f.style.display = 'none'; });
window.scrollTo(0, 0);
```

With every sibling hidden, the flex `.strip` collapses to just the one visible frame, which — once
`body` padding is zeroed — always lands at exactly `(0, 0)` regardless of which frame it is. Verify
this once with `getBoundingClientRect()`; after that it's safe to assume without re-querying.

### 4. Picking the zoom factor

The two-slice budget is `2 × innerHeight` (each slice can be at most `innerHeight` tall). At
`innerHeight = 957`, that's `1914`. Pick a zoom so `640 × zoom` sits comfortably under that budget —
`2.9` (→ `1856` tall, `1044` wide) leaves ~60px of margin for rounding, which is what this skill was
last verified against. Recompute against the actual measured `innerHeight` rather than reusing `2.9`
blindly: `zoom ≤ (2 × innerHeight × 0.97) / 640`.

### 5. Capture slice 1 (top)

```
computer.zoom { tabId, action: "zoom", region: [0, 0, frameWidth, innerHeight], save_to_disk: true }
```

**Retry once on failure.** The very first `computer.zoom` call after a `navigate` + DOM mutation
frequently times out (`CDP sendCommand "Page.captureScreenshot" timed out after 30000ms`) and then
succeeds immediately on retry — this happened repeatedly during testing and is not a sign anything is
actually wrong. Only escalate to the user if a *retry* also fails.

### 6. Scroll and capture slice 2 (bottom)

```javascript
window.scrollTo(0, innerHeight);   // e.g. scrollTo(0, 957)
```

Then capture region `[0, 0, frameWidth, frameHeight - innerHeight]` (e.g. `[0, 0, 1044, 899]` for a
`1856`-tall frame after a `957` first slice). Because the frame sat at `(0,0)` before scrolling,
scrolling by exactly the first slice's height aligns the frame's remaining content to viewport `y=0`
with no gap and no overlap — no need to eyeball or fudge an offset.

### 7. Stitch the two slices

```powershell
Add-Type -AssemblyName System.Drawing
$top = [System.Drawing.Image]::FromFile($topPath)
$bottom = [System.Drawing.Image]::FromFile($bottomPath)
$totalHeight = $top.Height + $bottom.Height    # compute the sum in its own variable — see note below
$out = New-Object System.Drawing.Bitmap($top.Width, $totalHeight)
$g = [System.Drawing.Graphics]::FromImage($out)
$g.DrawImage($top, 0, 0)
$g.DrawImage($bottom, 0, $top.Height)
$out.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $out.Dispose(); $top.Dispose(); $bottom.Dispose()
Remove-Item $topPath, $bottomPath -Force   # clean up the temp slices immediately
```

**Don't inline the height sum into `New-Object`'s argument list**
(`New-Object System.Drawing.Bitmap($top.Width, $top.Height + $bottom.Height)`). PowerShell's argument
parsing treats `,` and `+` unusually inside that parenthesized form and will silently call the
3-argument `Bitmap(width, height, PixelFormat)` overload instead, passing `$bottom.Height` as a
`PixelFormat` and failing with a confusing conversion error. Compute `$totalHeight` as its own
statement first, as shown above.

`$dest` is the final path: `docs/store/graphics/phone-screenshots/day/<slug>.png` for the light
template, `.../night/<slug>.png` for the dark one, using the slug from the
[8-frame map](#the-8-frame-map).

### 8. QA before calling it done

- [ ] All 8 day files and all 8 night files exist, named per the 8-frame map's slugs
- [ ] No visible seam at the slice boundary (open one and check where the stitch line falls —
      it should be invisible)
- [ ] Each file: PNG, well under 8 MB, 9:16 aspect ratio, both sides within 320–3,840 px
- [ ] Temp slice files and the local HTTP server are cleaned up

Tell the user which frames were exported and to which folders — this workflow doesn't touch the
templates, so there's nothing to report there.

## Adding, removing, or reordering a frame

- **Adding**: only if under the 8-frame cap. Pick a theme color not already adjacent to its
  neighbors (see the alternating pattern in the current 8), write a 2-line headline, and place it
  where it fits the narrative sequence (overview → drill-down features → action/export at the end).
- **Removing**: delete the whole `<!-- FRAME N — Name --> ... </div>` block and renumber the
  remaining HTML comments sequentially (`FRAME 1`, `FRAME 2`, ...) so they stay accurate for future
  editing sessions.
- **Reordering**: move the whole frame block; renumber the `<!-- FRAME N -->` comments to match.

## Common mistakes to avoid

- Referencing images by relative `src` path instead of embedding base64 — breaks when the HTML is
  viewed outside the exact folder it was built in.
- Assuming crop height, or the status-bar strip's pixel offset, from a previous session without
  re-verifying against the new screenshot batch — different devices/OS skins have different status
  bar heights.
- Leaving `.screen` background color mismatched against the actual screenshot content color — even a
  one-shade difference shows as a visible seam under the notch.
- Redesigning the skyline motif, type system, or color tokens without being asked — this skill is
  for swapping content into a locked design, not iterating the design itself.
- Editing only one of the two templates when a change (theme color, headline, frame order) should
  apply to both light and dark.
- (Export) Navigating to the templates via `file://` — the `claude-in-chrome` extension blocks it.
  Serve the directory over local HTTP first.
- (Export) Treating a single `computer.zoom` timeout as a real failure instead of retrying once —
  the first capture after a navigate+mutation times out routinely and succeeds on retry.
- (Export) Assuming a prior session's `innerHeight` or zoom factor still fits — screen resolution
  differs by machine; measure fresh and recompute the zoom factor each session.
- (Export) Inlining `$top.Height + $bottom.Height` directly into `New-Object Bitmap(...)`'s argument
  list instead of a separate `$totalHeight =` statement — silently calls the wrong constructor
  overload.

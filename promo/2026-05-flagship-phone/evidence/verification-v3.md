# TrainIQ Promo V3 Verification

Date: 2026-05-12

## Outputs

- Video: `trainiq-flagship-phone-promo-2026-05-v3.mp4`
- QA stills: `evidence/qa-v3/`

## Commands Run

- `npm.cmd run stills:qa:v3`
- `npm.cmd run render:v3`
- `npm.cmd audit --audit-level=low`

## Visual QA

Rendered 37 QA stills covering intro fly-in, requested transition frames, actual V3 cut points, phone exit, and final card.

Intro fly-in frames:

```text
0, 18, 36, 54, 72, 90, 108
```

Requested transition/final frames:

```text
144, 150, 160, 252, 258, 268, 366, 372, 382,
468, 474, 484, 580, 586, 596, 704, 708, 720, 744
```

Actual V3 cut-point frames:

```text
168, 186, 204, 270, 288, 306, 390, 408, 492, 510, 612
```

Spot checks confirmed:

- phone is absent at frame 0, flies in from below, and settles before the first app caption;
- screen transitions are one-way horizontal slides inside the persistent phone frame;
- the incoming screen does not restart its enter motion after the cut;
- no opacity-based text ghosting remains in the checked cut frames;
- captions stay below the phone frame;
- final card appears after the phone scene clears.

## MP4 Check

```json
{"bytes":7448610,"duration":"27.05s","tracks":["vide","soun"]}
```

The output stays within the requested 20-30 second range and includes both video and audio tracks.

## Audit

```text
found 0 vulnerabilities
```

## Notes

- Visible app UI remains sourced from real TrainIQ screenshots in `public/screenshots/`.
- V3 was written beside V2 and did not overwrite the reviewed V2 video.
- Itch still assets from V2 were not regenerated because this request only changed the promo motion.

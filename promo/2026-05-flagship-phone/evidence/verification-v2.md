# TrainIQ Promo V2 Verification

Date: 2026-05-12

## Outputs

- Video: `trainiq-flagship-phone-promo-2026-05-v2.mp4`
- Itch pack: `itch-upload-pack-v2/`
- Cover: `itch-upload-pack-v2/trainiq-itch-cover.png`
- Gallery screenshots: `itch-upload-pack-v2/trainiq-itch-screenshot-01.png` through `trainiq-itch-screenshot-06.png`
- Loose phone screenshots: `itch-upload-pack-v2/loose-phone-screenshots/`
- Itch notes: `itch-upload-pack-v2/itch-fields-v2.txt`

## Commands Run

- `npm.cmd install`
- `npm.cmd run stills:qa`
- `npm.cmd run itch:cover`
- `npm.cmd run itch:screenshots`
- `npm.cmd run render:v2`
- `npm.cmd audit --audit-level=low`

## QA Stills

Rendered to `evidence/qa-v2/`:

- Transition frames: `144`, `150`, `160`, `252`, `258`, `268`, `366`, `372`, `382`, `468`, `474`, `484`, `580`, `586`, `596`, `704`, `708`, `720`, `744`
- Steady-state frames: `120`, `210`, `320`, `420`, `530`, `640`, `780`

Spot-checked frames showed:

- no stacked phone frames;
- captions stay below the phone in a dedicated safe zone;
- outgoing captions fade before the next caption becomes readable;
- the final card appears after the phone scene has cleared;
- screen transitions happen inside the persistent phone frame.

## MP4 Check

Rendered file:

```json
{"bytes":7541200,"duration":"27.05s","tracks":["vide","soun"]}
```

The output stays within the requested 20-30 second range and includes both video and audio tracks.

## Itch Asset Dimensions

```text
trainiq-itch-cover.png        630x500
trainiq-itch-screenshot-01.png 1600x900
trainiq-itch-screenshot-02.png 1600x900
trainiq-itch-screenshot-03.png 1600x900
trainiq-itch-screenshot-04.png 1600x900
trainiq-itch-screenshot-05.png 1600x900
trainiq-itch-screenshot-06.png 1600x900
```

Loose raw phone screenshots copied into the v2 pack:

```text
01-home-dashboard.png             1080x2400
02-training-plan.png              1080x2400
03-nutrition.png                  1080x2400
04-progress.png                   1080x2400
05-coach.png                      1080x2400
06-settings-health-connect.png    1080x2400
07-active-workout.png             1080x2400
```

## Audit

`npm.cmd audit --audit-level=low` result:

```text
found 0 vulnerabilities
```

## Notes

- Visible app UI is sourced from existing real TrainIQ screenshots in `public/screenshots/`.
- Decorative imagery is limited to the generated backdrop and does not introduce fake app UI.
- `04-progress.png` is included in the loose screenshots and represented in the itch gallery export set.

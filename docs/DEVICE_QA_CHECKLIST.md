# Device QA Checklist

This checklist focuses on the latest high-risk areas: audio stability, battery save persistence, and virtual D-pad feel.

## Preconditions

- Install latest debug build (`app-debug.apk`).
- Import a ROM known to support in-game battery saves.
- In Settings, keep defaults first:
  - Audio sample rate: `44100 Hz`
  - Audio buffer size: `8192`
- Test with headphones and device speaker.

## 1) Audio Pitch and Underrun

### Case A: Default config

1. Start game and idle on title screen for 60s.
2. Enter gameplay and move continuously for 2 minutes.
3. Open/close game menu 5 times.
4. Pause/resume 10 times quickly.

Pass criteria:

- No low-pitch or slow-motion audio.
- No long mute gaps (> 300 ms).
- No repeating crackle bursts after resume.

### Case B: Config switching

1. While in game, switch sample rate across: `32000`, `44100`, `48000`.
2. Switch buffer across: `4096`, `8192`, `16384`.
3. After each change, play 30s and observe.

Pass criteria:

- Audio restarts cleanly after each switch.
- No stuck silence state.
- Pitch remains natural in all options.

## 2) In-Game Battery Save Persistence (.sav)

1. In game, create a native game save (not save-state).
2. Exit to game list via in-game menu.
3. Force close app from recent tasks.
4. Reopen app and launch same ROM.
5. Confirm native game save is still present.

Pass criteria:

- Save data survives app process death.
- Save data survives device reboot.
- No regressions to save-state slots.

## 3) D-pad and Joystick Touch Feel

### Wheel mode

1. Enable `WHEEL` mode.
2. Test cardinal directions with short taps.
3. Test diagonal movement with circular thumb motion.
4. Hold near direction boundaries for 10s.

Pass criteria:

- Quick tap triggers immediately.
- Fewer accidental direction flickers at boundaries.
- Diagonal input remains stable when intended.

### Joystick mode

1. Enable `JOYSTICK` mode.
2. Test tiny center movement and confirm no accidental movement.
3. Drag to edge and hold 4 directions + 4 diagonals.
4. Lift and tap repeatedly to verify immediate response.

Pass criteria:

- Better center dead-zone control.
- Direction lock is stable under minor finger jitter.
- Lift/re-touch does not miss the first input.

## Optional Log Hints

- Filter tags: `JBOY_Core`, `AudioOutput`, `EmulatorCore`.
- Look for frequent underrun-like gaps or repeated audio restart errors.

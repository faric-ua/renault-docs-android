# v0.5.7 Phone Test — live section surfing + timing

No Renault Menu point 9 is required.

## Primary navigation gate

1. Install v0.5.7 over v0.5.6.
2. Modern → NT8183A → 101.
3. Confirm the Classic left column is still hidden.
4. Tap `Розділи`.
5. Search for `103` or `генератор`.
6. Select 103.

PASS requires:
- the section panel opens over the current viewer;
- 101 is marked active before switching;
- search filters the native list;
- 103 opens without returning to the volume screen;
- the top-level Viewer/WebView is not visibly restarted;
- the Classic left column stays hidden;
- existing menu/nav/doc controls continue working.

Then repeat live switches:
`103 → 105 → 101`.

These should be materially faster than the first volume/runtime open.

## Rotation

After live-switching to 103:
1. rotate the phone;
2. reopen `Розділи`;
3. confirm 103 remains the active highlighted section;
4. confirm no automatic re-run changes the current section.

## Performance capture

After the first slow open, tap `DBG` and paste the report.

We specifically need:
- `runtimeTiming.clientAgeMs`;
- `runtimeTiming.fastPackPrepareMs`;
- `runtimeTiming.fastPackCopiedToLocalCache`;
- `navigationTiming`;
- initial hybrid `durationMs`.

Then live-switch to another section and take one more DBG report. It should include `liveSectionSwitch.durationMs`.

## Failure capture

If a live switch fails, leave the viewer in that state and immediately capture DBG.

# v0.5.38 Phone test — converter foundation

## Prepare

Install exact v0.5.38 APK.

Use a copied Renault source if you want to avoid modifying the current test output. Source remains read-only.

## Gate A — friendly paths

Open Converter.

Expected:
- source displays like `Documents/Renault/Megane II`;
- destination displays like `Documents/Renault`;
- no raw `content://com.android.externalstorage...` URI in the cards.

Also open Settings -> Backup.

Expected:
- same friendly path style.

## Gate B — scan speed / scan UI

Start the same Megane II conversion.

Expected:
- scan is noticeably faster than v0.5.37 on the same source/provider;
- scan bar is indeterminate;
- text says `Знайдено файлів: N`;
- no misleading `0 / 700` style counter.

If the provider rejects direct child queries, status may briefly say compatibility scan; that is allowed but should be recorded.

## Gate C — Gallery isolation

Let conversion proceed enough to create/copy image folders, preferably to completion.

Expected final output root:
- contains `.nomedia`;
- new PM / PC / ICONES / DRAPEAUX albums do not get created by the new output.

For albums already indexed from v0.5.37:
- they can remain visible due to Gallery/MediaStore cache;
- deleting the old v0.5.37 output and allowing a media refresh/reboot is the cleanest comparison.

## Gate D — existing output retrofit

If using an existing completed converter output:
- tap `Додати готову папку в бібліотеку`.

Expected:
- app ensures root `.nomedia` before DatasetReader validation.

## Regression

Verify:
- conversion still reaches copy/package/validation;
- output is still `<source>_android`;
- source remains unchanged;
- existing output is not overwritten.

## Closeout

v0.5.38 is PASS when friendly paths are clean, scan behavior is improved, and new converter output no longer pollutes Gallery.

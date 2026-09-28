# v0.5.37 Phone test — Converter Writer Wave 1

## Important

This test is about the new Android writer.

The source folder is read-only from the converter's point of view.
v0.5.37 never offers source deletion.

For the first run, prefer a small copied Renault source/subset if available. After that passes, run the full corpus.

## Gate A — plan

1. Open Library → `Конвертувати стару папку`.
2. Select the OLD Renault source folder.
3. Select a writable parent destination, e.g. `Documents/Renault`.
4. Tap `Перевірити план`.

Expected:
- planned output name is `<source>_android`;
- source and destination survive rotation;
- no conversion starts during rotation.

## Gate B — start / progress

Tap `Почати конвертацію`.

Expected:
- visible phase/progress starts;
- notification appears where Android allows converter notifications;
- file count/progress advances;
- screen rotation does not restart from zero;
- leaving ConversionActivity and returning reconnects to the same run.

## Gate C — cancel safety

On the first small test, cancel during copying.

Expected:
- operation stops;
- staging folder is removed;
- final `<source>_android` does not appear;
- source remains unchanged.

Then start again and allow completion.

## Gate D — completed output

Expected destination:
`<destination>/<source>_android`

Expected root/package files:
- original copied Renault files;
- `renault-dataset.json`;
- `conversion-report.json`;
- `_renault/START.html`;
- `_renault/volumes.json`;
- `_renault/modern-index.json`.

No `.renault-staging` folder should remain after success.

## Gate E — Library registration

Tap `Додати готову папку в бібліотеку`.

Expected:
- DatasetReader validation passes;
- dataset appears in Library;
- volume count is non-zero for a multi-volume corpus;
- Classic can open the generated catalog/original content.

Wave-1 limitation:
- full native Modern section/runtime data is intentionally not compiled yet;
- do not treat missing Runtime IR/Fast Pack as a v0.5.37 failure.

## Gate F — no overwrite

Try the same conversion again without removing the completed output.

Expected:
- converter refuses because `<source>_android` already exists;
- existing output is not overwritten.

## Closeout

v0.5.37 is PHONE PASS when:
- staged copy/normalization completes;
- cancel is safe;
- rotation/background does not restart the run;
- output validates and registers;
- source remains untouched.

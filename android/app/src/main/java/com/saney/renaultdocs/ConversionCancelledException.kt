package com.saney.renaultdocs

/**
 * Thrown from a worker checkpoint when a long operation (archive intake,
 * volume preparation, .rdpkg import) is cancelled.
 *
 * History: it used to live in SafConversionEngine.kt next to the old
 * «Конвертер». The converter was removed in v0.5.112 (owner decision
 * 2026-10-12; archive branch archive/legacy-converter-v0.5.111), but the
 * exception is still used by the other workers, so it moved here unchanged.
 */
class ConversionCancelledException :
    RuntimeException("Конвертацію скасовано.")

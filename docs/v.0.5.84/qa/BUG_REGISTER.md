# v0.5.84 issue findings

## BG-001 — conversion start resets persisted run on redelivery (CONFIRMED IN SOURCE)
`ConversionService.onStartCommand` calls `runStore.begin()` even on framework redelivery, resetting cancelRequested, progress and startedAt. Fix in this release.

## BG-002 — completed native start potentially replayable (SOURCE FINDING)
`NativeRdpkgPreparationService` starts fresh work for a valid `ACTION_START` after its old persisted state has finished, even if command is framework-redelivered. Guard redelivery with explicit stopped/terminal state.

## BG-003 — native resume clears pending cancellation (CONFIRMED IN SOURCE)
`NativeRdpkgRunStore.resumeAfterProcessRestart` resets `KEY_CANCEL_REQUESTED=false`. Preserve cancel request; never turn a cancelled user's intent back into continued conversion.

## BG-004 — Activity interrupted reconciliation may race service restart (REVIEW)
`ConversionActivity` rechecks at 1.5 seconds, using process-local `ConversionService.isActive()` and may incorrectly fail persisted running state during delayed Android restart. Evaluate and address without sacrificing historical output recovery. Do not assume any device failure was observed; this is source-level risk.

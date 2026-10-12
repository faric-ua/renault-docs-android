package com.saney.renaultdocs

/**
 * Thrown from a worker checkpoint after the owner pressed «Скасувати» (#181).
 * Workers stop between files, delete only their own temporary output and
 * persist CANCELLED. Finished volumes and earlier prepared files stay intact.
 */
class OperationCancelledException : RuntimeException("Скасовано користувачем.")

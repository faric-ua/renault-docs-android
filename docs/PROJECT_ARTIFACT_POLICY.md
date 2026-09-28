# Project Artifact Policy

Renault Docs project decisions must not exist only in ChatGPT conversation history.

## Rule

Whenever a discussion produces a durable decision, contract, roadmap, QA result, UX behavior, or architecture choice, it should be persisted in the repository before the work is considered complete.

## What must be saved

- release state and tested commit;
- phone QA findings;
- bugs and expected behavior;
- lifecycle/navigation/modal contracts;
- conversion and backup behavior;
- storage/cloud architecture;
- Settings and localization decisions;
- product roadmap;
- diagrams and design references.

## Diagram quality

For text-heavy diagrams:
1. save SVG/Mermaid/PlantUML as the canonical source;
2. optionally export PNG/PDF for convenience;
3. do not rely on an AI-generated raster image as the only project record.

## Source of truth

Repository documentation is the project source of truth.
Chat is a working interface, not permanent project storage.

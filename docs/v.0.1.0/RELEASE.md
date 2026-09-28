# Renault Docs v0.1.0

## Goal

Перший встановлюваний Android skeleton без Termux/Python для Library workflow.

## Scope

- Library screen;
- додавання вже готового dataset через Android Storage Access Framework;
- persistable read permission;
- валідація `renault-dataset.json`;
- локальне збереження registration;
- dataset tile;
- placeholder Viewer screen;
- debug APK CI artifact.

## Не входить у цей milestone

- конвертація старої Windows-папки всередині APK;
- production WebView viewer;
- Android PDF layer;
- release signing;
- updater.

## System contracts affected

- SAF;
- persistable URI;
- Library tiles;
- Back/navigation;
- rotation/recreation;
- APK build pipeline.

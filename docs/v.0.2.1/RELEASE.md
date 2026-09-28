# Renault Docs v0.2.1 — SAF WebView viewer

## Reason

Real-phone test of v0.2.0 confirmed that the Library correctly discovers the Laguna dataset and 10 volumes, but tapping the dataset opens only the placeholder Viewer screen.

## Goal

Replace the placeholder with a real SAF-backed WebView that opens `_renault/START.html` and navigates into the legacy Renault HTM/HTML/JS/GIF content.

## Scope

- controlled local virtual origin;
- all dataset resources resolved through persisted SAF tree URI;
- no raw `file://` dependency;
- JavaScript enabled for legacy Renault navigation;
- WebView history + Android Back ownership;
- WebView state restoration across rotation;
- missing-resource error response;
- PDF links are detected and shown as a clear not-yet-supported page instead of silent failure.

## Out of scope

Integrated Android PDF rendering remains a separate next wave.

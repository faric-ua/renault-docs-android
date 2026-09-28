#!/usr/bin/env python3
from __future__ import annotations

import argparse
import functools
import http.server
import mimetypes
import os
import socket
import sys
import webbrowser
from email.utils import formatdate
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlsplit

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from web.pdf_support import (
    PDFJS_ROUTE_PREFIX,
    PDF_RAW_ROUTE,
    is_pdf_request,
    pdfjs_vendor_ready,
    resolve_dataset_path,
    setup_required_html,
    viewer_html,
)


ENTRY_NAMES = (
    "index.htm",
    "index.html",
    "INDEX.HTM",
    "INDEX.HTML",
    "accueil.htm",
    "ACCUEIL.HTM",
)

mimetypes.add_type("text/javascript", ".mjs")


def find_entrypoints(root: Path, limit: int = 20) -> list[Path]:
    entries: list[Path] = []

    for name in ENTRY_NAMES:
        direct = root / name
        if direct.is_file():
            entries.append(direct)

    if len(entries) < limit:
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if path.name in ENTRY_NAMES and path not in entries:
                entries.append(path)
                if len(entries) >= limit:
                    break

    return entries


def local_ip() -> str:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.connect(("8.8.8.8", 80))
            return sock.getsockname()[0]
    except OSError:
        return "127.0.0.1"


class RenaultDocsHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(
        self,
        *args,
        directory: str,
        vendor_root: Path,
        **kwargs,
    ):
        self.dataset_root = Path(directory).resolve()
        self.vendor_root = vendor_root.resolve()
        super().__init__(*args, directory=directory, **kwargs)

    def do_GET(self) -> None:
        self._handle_request(head_only=False)

    def do_HEAD(self) -> None:
        self._handle_request(head_only=True)

    def _handle_request(self, head_only: bool) -> None:
        parsed = urlsplit(self.path)

        if parsed.path.startswith(PDFJS_ROUTE_PREFIX):
            self._serve_pdfjs_asset(parsed.path, head_only)
            return

        if parsed.path == PDF_RAW_ROUTE:
            self._serve_raw_pdf(parsed.query, head_only)
            return

        if is_pdf_request(self.path):
            try:
                target = resolve_dataset_path(self.dataset_root, parsed.path)
            except ValueError:
                self.send_error(404, "PDF path is outside dataset")
                return

            if target.is_file():
                page = (
                    viewer_html(parsed.path)
                    if pdfjs_vendor_ready(self.vendor_root)
                    else setup_required_html(parsed.path)
                )
                self._send_text(page, head_only)
                return

        if head_only:
            super().do_HEAD()
        else:
            super().do_GET()

    def _serve_raw_pdf(self, query: str, head_only: bool) -> None:
        params = parse_qs(query)
        values = params.get("path")
        if not values:
            self.send_error(400, "Missing PDF path")
            return

        relative = values[0]
        if not relative.lower().endswith(".pdf"):
            self.send_error(400, "Requested raw file is not a PDF")
            return

        try:
            target = resolve_dataset_path(self.dataset_root, "/" + relative)
        except ValueError:
            self.send_error(404, "PDF path is outside dataset")
            return

        self._send_file(target, "application/pdf", head_only)

    def _serve_pdfjs_asset(self, request_path: str, head_only: bool) -> None:
        relative = unquote(request_path[len(PDFJS_ROUTE_PREFIX) :]).lstrip("/")
        target = (self.vendor_root / relative).resolve()

        try:
            common = Path(
                os.path.commonpath(
                    [str(self.vendor_root), str(target)]
                )
            )
        except ValueError:
            self.send_error(404, "PDF.js asset path is invalid")
            return

        if common != self.vendor_root:
            self.send_error(404, "PDF.js asset path is outside vendor root")
            return

        content_type = (
            mimetypes.guess_type(str(target))[0]
            or "application/octet-stream"
        )
        self._send_file(target, content_type, head_only)

    def _send_text(self, text: str, head_only: bool) -> None:
        payload = text.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()

        if not head_only:
            self.wfile.write(payload)

    def _send_file(
        self,
        target: Path,
        content_type: str,
        head_only: bool,
    ) -> None:
        if not target.is_file():
            self.send_error(404, "File not found")
            return

        stat = target.stat()
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(stat.st_size))
        self.send_header(
            "Last-Modified",
            formatdate(stat.st_mtime, usegmt=True),
        )
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()

        if head_only:
            return

        try:
            with target.open("rb") as source:
                while True:
                    chunk = source.read(1024 * 1024)
                    if not chunk:
                        break
                    self.wfile.write(chunk)
        except BrokenPipeError:
            pass


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Serve normalized Renault documentation over local HTTP."
    )
    parser.add_argument("root", type=Path, help="Normalized documentation root")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--open", action="store_true", help="Open the first detected entry page")
    args = parser.parse_args()

    root = args.root.resolve()
    if not root.is_dir():
        raise SystemExit(f"Documentation root does not exist: {root}")

    vendor_root = Path(__file__).resolve().parent / "vendor" / "pdfjs"

    handler = functools.partial(
        RenaultDocsHandler,
        directory=str(root),
        vendor_root=vendor_root,
    )
    server = http.server.ThreadingHTTPServer((args.host, args.port), handler)

    entries = find_entrypoints(root)
    print(f"Serving: {root}")
    print(f"Local URL: http://127.0.0.1:{args.port}/")
    print(
        "PDF viewer: "
        + (
            "ready"
            if pdfjs_vendor_ready(vendor_root)
            else "not installed (run: python tools/install_pdfjs.py)"
        )
    )

    if args.host in ("0.0.0.0", "::"):
        print(f"LAN URL:   http://{local_ip()}:{args.port}/")

    if entries:
        print("\nDetected possible entry pages:")
        for entry in entries:
            rel = entry.relative_to(root).as_posix()
            print(f"  http://127.0.0.1:{args.port}/{rel}")
        if args.open:
            rel = entries[0].relative_to(root).as_posix()
            webbrowser.open(f"http://127.0.0.1:{args.port}/{rel}")
    else:
        print("\nNo common INDEX/ACCUEIL entry page was detected automatically.")
        print("Open the root URL and choose the needed HTML page manually.")

    print("\nPress Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

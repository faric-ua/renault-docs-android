#!/usr/bin/env python3
"""Promote an immutable, CI-verified stable-signed debug APK to GitHub Releases.

Invocation (GitHub Actions only): python scripts/publish_debug_release.py PUBLISH_MANIFEST
The PR/test pipeline calls validate_manifest() offline, without repository mutations.
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import zipfile


ROOT = Path(__file__).resolve().parents[1]
PROMOTION_DIR = (ROOT / "docs/release-promotions").resolve()
VERSION_RE = re.compile(r"0\.[0-9]+\.[0-9]+")
SHA_RE = re.compile(r"[0-9a-f]{40}")
ARTIFACT_DIGEST_RE = re.compile(r"sha256:[0-9a-f]{64}")


def command(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True, check=check)


def validate_manifest(path: str | Path) -> dict:
    source = Path(path)
    if not source.is_absolute():
        source = ROOT / source
    source = source.resolve()
    if source.parent != PROMOTION_DIR or not source.is_file():
        raise ValueError("Promotion must be a checked-in docs/release-promotions/*.json file")
    data = json.loads(source.read_text(encoding="utf-8"))
    if data.get("schema_version") != 1:
        raise ValueError("Unsupported promotion schema")
    version = data.get("version")
    if not isinstance(version, str) or not VERSION_RE.fullmatch(version):
        raise ValueError("Invalid version")
    if source.name != f"v{version}.json":
        raise ValueError("Manifest filename differs from version")
    if type(data.get("version_code")) is not int or data["version_code"] <= 0:
        raise ValueError("Invalid versionCode")
    if not isinstance(data.get("source_sha"), str) or not SHA_RE.fullmatch(data["source_sha"]):
        raise ValueError("Invalid source SHA")
    for key in ("signed_run_id", "artifact_id"):
        if type(data.get(key)) is not int or data[key] <= 0:
            raise ValueError(f"Invalid {key}")
    expected_name = f"Renault-Docs-v{version}-Debug"
    if data.get("artifact_name") != expected_name:
        raise ValueError("Artifact name does not match the version")
    if data.get("tag") != f"v{version}-debug":
        raise ValueError("Release tag must be versioned -debug")
    if data.get("prerelease") is not True:
        raise ValueError("Debug APK must be published as a prerelease")
    if data.get("qa_status") not in (
        "core-phone-pass-extended-paused",
        "phone-qa-pending",
        "full-phone-pass",
    ):
        raise ValueError("Unrecognized QA state")
    if not isinstance(data.get("artifact_digest"), str) or not ARTIFACT_DIGEST_RE.fullmatch(data["artifact_digest"]):
        raise ValueError("Missing GitHub artifact-bundle digest")

    app_gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
    if f'versionCode = {data["version_code"]}' not in app_gradle or f'versionName = "{version}"' not in app_gradle:
        raise ValueError("Promotion version differs from Android app Gradle version")

    release_meta = json.loads(
        (ROOT / f"docs/v.{version}/RELEASE_META.json").read_text(encoding="utf-8")
    )
    for meta_key, key in (
        ("versionName", "version"),
        ("versionCode", "version_code"),
        ("source_sha", "source_sha"),
        ("signed_android_apk_run", "signed_run_id"),
        ("artifact_id", "artifact_id"),
        ("artifact_name", "artifact_name"),
    ):
        # Older release metadata uses android_apk_run instead of signed_android_apk_run.
        if meta_key not in release_meta and meta_key == "signed_android_apk_run":
            meta_key = "android_apk_run"
        if release_meta.get(meta_key) != data[key]:
            raise ValueError(f"Release metadata mismatch for {key}")
    meta_digest = release_meta.get("artifact_digest") or release_meta.get("artifact_sha256")
    if meta_digest != data["artifact_digest"]:
        raise ValueError("Release metadata artifact digest differs from promotion")
    return data


def gh_api_json(endpoint: str) -> dict:
    result = command("gh", "api", endpoint)
    return json.loads(result.stdout)


def verify_ci_source(data: dict, repo: str) -> None:
    command("git", "cat-file", "-e", f'{data["source_sha"]}^{{commit}}')
    command("git", "merge-base", "--is-ancestor", data["source_sha"], "HEAD")
    # The run must be the stable-signed main build, not an ephemeral PR signer.
    run = gh_api_json(f"repos/{repo}/actions/runs/{data['signed_run_id']}")
    if (
        run.get("name") != "Android Debug APK"
        or run.get("event") not in ("push", "workflow_dispatch")
        or run.get("head_branch") != "main"
        or run.get("head_sha") != data["source_sha"]
        or run.get("status") != "completed"
        or run.get("conclusion") != "success"
    ):
        raise ValueError("Source workflow is not the successful main Android Debug APK run")
    artifacts = gh_api_json(
        f"repos/{repo}/actions/runs/{data['signed_run_id']}/artifacts"
    ).get("artifacts", [])
    matched = [
        x for x in artifacts
        if x.get("id") == data["artifact_id"]
        and x.get("name") == data["artifact_name"]
        and x.get("digest") == data["artifact_digest"]
        and not x.get("expired", True)
    ]
    if len(matched) != 1:
        raise ValueError("Signed APK artifact not found, changed or expired")


def check_downloaded_apk(folder: Path, version: str) -> tuple[Path, Path, str]:
    filename = f"Renault-Docs-v{version}-debug.apk"
    apk, checksum = folder / filename, folder / f"{filename}.sha256"
    if not apk.is_file() or not checksum.is_file() or apk.stat().st_size < 100000:
        raise ValueError("Missing APK or sha256 file in source artifact")
    lines = checksum.read_text(encoding="utf-8").strip().splitlines()
    if len(lines) != 1:
        raise ValueError("Expected exactly one checksum line")
    match = re.fullmatch(r"([0-9a-f]{64})\s+\*?" + re.escape(filename), lines[0])
    if not match:
        raise ValueError("Malformed APK checksum file")
    digest = hashlib.file_digest(apk.open("rb"), "sha256").hexdigest()
    if digest != match.group(1):
        raise ValueError("APK checksum mismatch")
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        if "AndroidManifest.xml" not in names or "classes.dex" not in names:
            raise ValueError("Artifact is not a valid Android APK")
    return apk, checksum, digest


def publish(data: dict) -> None:
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    if repo != "faric-ua/renault-docs-android":
        raise ValueError("Refusing release from a fork or another repository")
    if os.environ.get("GITHUB_REF") != "refs/heads/main":
        raise ValueError("Only trusted main branch can publish")
    if not os.environ.get("GH_TOKEN"):
        raise ValueError("Missing GitHub Actions token")

    verify_ci_source(data, repo)
    tag = data["tag"]
    existing = command(
        "gh", "api", f"repos/{repo}/releases/tags/{tag}", check=False
    )
    if existing.returncode == 0:
        raise ValueError(f"Release {tag} already exists; never silently overwrite")
    if "HTTP 404" not in existing.stderr:
        raise RuntimeError(f"Cannot verify release absence: {existing.stderr[-500:]}")

    # Do not attach the .zip container from Actions. Attach the original signed
    # .apk and its checksum from the verified build, unchanged.
    with tempfile.TemporaryDirectory(prefix="renault-release-") as temporary:
        folder = Path(temporary)
        command(
            "gh", "run", "download", str(data["signed_run_id"]),
            "--repo", repo, "--name", data["artifact_name"], "--dir", str(folder),
        )
        apk, checksum, digest = check_downloaded_apk(folder, data["version"])
        note = folder / "RELEASE_NOTES.md"
        note.write_text(
            f"# Renault Docs v{data['version']} — developer-signed debug build\n\n"
            "This is an Android **debug pre-release**, not a Play Store release.\n"
            "Install over the existing Renault Docs app only when the signing "
            "certificate matches; do not uninstall or clear user data.\n\n"
            f"- Android versionCode: {data['version_code']}\n"
            f"- Source commit: {data['source_sha']}\n"
            f"- Successful stable-signed CI run: {data['signed_run_id']}\n"
            f"- Phone QA: {data['qa_status']} (additional QA may be pending)\n"
            f"- APK SHA-256: {digest}\n\n"
            "This release contains the original signed APK and its independent "
            "checksum. The earlier GitHub Actions archive may expire, but "
            "the Release assets stay available until removed by the repository owner.\n",
            encoding="utf-8",
        )
        command(
            "gh", "release", "create", tag, str(apk), str(checksum),
            "--repo", repo, "--target", data["source_sha"],
            "--title", f"Renault Docs v{data['version']} (debug)",
            "--notes-file", str(note), "--prerelease",
        )
        print(f"Published {tag}, APK SHA-256 {digest}")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: publish_debug_release.py docs/release-promotions/vX.Y.Z.json")
    data = validate_manifest(sys.argv[1])
    print(f"Validated signed promotion request: {data['tag']}")
    if os.getenv("GITHUB_ACTIONS") == "true":
        publish(data)


if __name__ == "__main__":
    main()

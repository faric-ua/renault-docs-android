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
# Public digest of the stable developer signing certificate (not a secret). Core Stable
# (#147, owner 2026-10-17) must be installable over the existing app, so it can only be
# the very same signed artifact as the phone-tested -debug release.
STABLE_SIGNER_SHA256 = "dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802"
CORE_STABLE_SUFFIX = "-core-stable"


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
    core_stable = data.get("channel") == "core-stable"
    if "channel" in data and not core_stable:
        raise ValueError("Unknown release channel")
    expected_file = f"v{version}{CORE_STABLE_SUFFIX}.json" if core_stable else f"v{version}.json"
    if source.name != expected_file:
        raise ValueError("Manifest filename differs from version")
    if type(data.get("version_code")) is not int or data["version_code"] <= 0:
        raise ValueError("Invalid versionCode")
    if not isinstance(data.get("source_sha"), str) or not SHA_RE.fullmatch(data["source_sha"]):
        raise ValueError("Invalid source SHA")
    for key in ("signed_run_id", "signed_run_number", "artifact_id"):
        if type(data.get(key)) is not int or data[key] <= 0:
            raise ValueError(f"Invalid {key}")
    expected_name = f"Renault-Docs-v{version}-Debug"
    if data.get("artifact_name") != expected_name:
        raise ValueError("Artifact name does not match the version")
    if core_stable:
        return validate_core_stable(data, source)
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

    release_meta = json.loads(
        (ROOT / f"docs/v.{version}/RELEASE_META.json").read_text(encoding="utf-8")
    )
    for meta_key, key in (
        ("versionName", "version"),
        ("versionCode", "version_code"),
        ("source_sha", "source_sha"),
        ("signed_android_apk_run", "signed_run_number"),
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


def validate_core_stable(data: dict, source: Path) -> dict:
    """Core Stable = the same signed artifact as an already promoted -debug release.

    Nothing is rebuilt: run, artifact and digest must equal the -debug promotion of the
    same version (which itself is checked against RELEASE_META), the owner's phone pass
    must be written down, and the known limitations are part of the request.
    """
    version = data["version"]
    if data.get("tag") != f"v{version}{CORE_STABLE_SUFFIX}":
        raise ValueError("Core Stable tag must be versioned -core-stable")
    if data.get("prerelease") is not False:
        raise ValueError("Core Stable is a full release, not a prerelease")
    if data.get("qa_status") != "core-stable-phone-pass":
        raise ValueError("Core Stable needs the owner's phone pass")
    evidence = data.get("phone_evidence")
    if not isinstance(evidence, list) or not evidence or not all(
        isinstance(x, str) and x.strip() for x in evidence
    ):
        raise ValueError("Core Stable needs written phone evidence")
    limits = data.get("known_limitations")
    if not isinstance(limits, list) or not limits or not all(
        isinstance(x, str) and x.strip() for x in limits
    ):
        raise ValueError("Core Stable needs the list of known limitations")
    if not isinstance(data.get("artifact_digest"), str) or not ARTIFACT_DIGEST_RE.fullmatch(data["artifact_digest"]):
        raise ValueError("Missing GitHub artifact-bundle digest")
    debug = validate_manifest(source.parent / f"v{version}.json")
    for key in (
        "version_code", "source_sha", "signed_run_id", "signed_run_number",
        "artifact_id", "artifact_name", "artifact_digest",
    ):
        if data.get(key) != debug[key]:
            raise ValueError(f"Core Stable differs from the tested -debug build: {key}")
    return data


def apk_signer_sha256(apk: Path) -> str:
    """Signer certificate digest via apksigner of the GitHub runner's Android SDK."""
    sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME") or ""
    # Same tool and flags as android-debug.yml, which already checks the stable signer there.
    pinned = Path(sdk, "build-tools/35.0.0/apksigner") if sdk else None
    tools = [pinned] if pinned and pinned.is_file() else (
        sorted(Path(sdk, "build-tools").glob("*/apksigner")) if sdk else []
    )
    if not tools:
        raise ValueError("apksigner not found on the runner")
    out = command(str(tools[-1]), "verify", "--verbose", "--print-certs", str(apk)).stdout
    digests = {
        d.replace(":", "").lower()
        for d in re.findall(r"certificate SHA-256 digest: ([0-9a-fA-F:]{64,95})", out)
    }
    if len(digests) != 1:
        raise ValueError(
            f"Expected one signer certificate digest, got {len(digests)}: {out.strip()[-400:]}"
        )
    return digests.pop()


def core_stable_notes(data: dict, digest: str, signer: str) -> str:
    lines = [
        f"# Renault Docs v{data['version']} — Core Stable",
        "",
        "Перша стабільна версія основних функцій (#147). Без входу, ліцензій і оплати.",
        "Це **той самий підписаний файл**, що й перевірений на телефоні "
        f"`v{data['version']}-debug` — ставиться поверх, проєкти й томи лишаються.",
        "Не видаляй застосунок і не чисть дані.",
        "",
        "## Перевірено на телефоні",
        *[f"- {x}" for x in data["phone_evidence"]],
        "",
        "## Що поки не вміє (відомі обмеження)",
        *[f"- {x}" for x in data["known_limitations"]],
        "",
        "## Дані збірки",
        f"- Версія: {data['version']} (versionCode {data['version_code']})",
        f"- Код: {data['source_sha']}",
        f"- Збірка GitHub Actions: run {data['signed_run_id']} (#{data['signed_run_number']})",
        f"- Сертифікат підпису SHA-256: {signer}",
        f"- APK SHA-256: {digest}",
        "",
    ]
    return "\n".join(lines)


def gh_api_json(endpoint: str) -> dict:
    result = command("gh", "api", endpoint)
    return json.loads(result.stdout)


def verify_ci_source(data: dict, repo: str) -> None:
    command("git", "cat-file", "-e", f'{data["source_sha"]}^{{commit}}')
    command("git", "merge-base", "--is-ancestor", data["source_sha"], "HEAD")
    built_source = command(
        "git", "show", f'{data["source_sha"]}:android/app/build.gradle.kts'
    ).stdout
    if (
        f'versionCode = {data["version_code"]}' not in built_source
        or f'versionName = "{data["version"]}"' not in built_source
    ):
        raise ValueError("Pinned source commit has a different APK version")
    # The run must be the stable-signed main build, not an ephemeral PR signer.
    run = gh_api_json(f"repos/{repo}/actions/runs/{data['signed_run_id']}")
    if (
        run.get("name") != "Android Debug APK"
        or run.get("event") not in ("push", "workflow_dispatch")
        or run.get("head_branch") != "main"
        or run.get("head_sha") != data["source_sha"]
        or run.get("run_number") != data["signed_run_number"]
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
    with apk.open("rb") as source:
        digest = hashlib.file_digest(source, "sha256").hexdigest()
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
        if data.get("channel") == "core-stable":
            signer = apk_signer_sha256(apk)
            if signer != STABLE_SIGNER_SHA256:
                raise ValueError("APK is not signed with the stable developer certificate")
            note.write_text(core_stable_notes(data, digest, signer), encoding="utf-8")
            command(
                "gh", "release", "create", tag, str(apk), str(checksum),
                "--repo", repo, "--target", data["source_sha"],
                "--title", f"Renault Docs v{data['version']} — Core Stable",
                "--notes-file", str(note), "--latest",
            )
            print(f"Published {tag} (Core Stable), APK SHA-256 {digest}")
            return
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
        raise SystemExit("Usage: publish_debug_release.py docs/release-promotions/vX.Y.Z[-core-stable].json")
    data = validate_manifest(sys.argv[1])
    print(f"Validated signed promotion request: {data['tag']}")
    if os.getenv("GITHUB_ACTIONS") == "true":
        publish(data)


def annotate_failure(error: BaseException) -> None:
    """Logs of Actions cannot be read from Claude's environment, annotations can (16.10 lesson)."""
    detail = str(error)
    if isinstance(error, subprocess.CalledProcessError):
        tail = (error.stderr or error.stdout or "").strip()[-600:]
        detail = f"{' '.join(map(str, error.cmd))[:200]} → exit {error.returncode}: {tail}"
    message = detail.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::error title=Publisher::{type(error).__name__}: {message}")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # noqa: BLE001 — re-raised after the annotation
        if os.getenv("GITHUB_ACTIONS") == "true":
            annotate_failure(error)
        raise

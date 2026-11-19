#!/usr/bin/env python3
"""Writes my own prompts from Claude Code session logs to docs/ai/prompts.md.

The brief asks for the prompts behind any AI-assisted work. Claude Code keeps each session as a JSONL log, one JSON
object per line. A log also holds everything Claude did (tool calls and their output, the agents' reports, skill text,
system reminders) and local paths, so the logs are never committed. This script keeps what I wrote and redacts what
mustn't be public.

Kept:
- every message I typed, including those typed while Claude was working;
- my answers to Claude's multiple-choice questions, with the question and its options, because several decisions were
  made there.

Dropped: system reminders, tool results, task notifications, skill injections, compaction summaries, interruption
markers, command wrappers and empty messages.

Redacted: home paths become ~/, /Volumes/<name> becomes [another drive], temporary paths become [temp path], email
addresses become [email] and my employer's name becomes [work]. A pasted copy of the OfferZen brief becomes a pointer
to spec section 2.

Usage:
    python3 tools/ai/extract_prompts.py SESSION.jsonl [SESSION.jsonl ...] [--output FILE]
    python3 tools/ai/extract_prompts.py --check

--check proves each filter and redaction on sample records and lines, and exits non-zero if one fails.
Python 3.9 or newer, standard library only.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

DEFAULT_OUTPUT = Path(__file__).resolve().parents[2] / "docs" / "ai" / "prompts.md"

# --- Redaction -------------------------------------------------------------------------------------------------------

BRIEF_MARKER = "[the OfferZen assignment brief, pasted; see spec §2]"
# The assignment PDF's title, and the headings of the PDF and of OfferZen's covering email.
BRIEF_TITLE = ("Senior Android Engineer", "Technical Vetting Assignment")
BRIEF_HEADINGS = (
    "Assessment details",
    "Recording your video",
    "Submission requirements",
    "The technical interview",
    "Instructions & Requirements",
    "Task 1: Restaurant browsing and details",
    "Task 2: Networked data and offline baseline",
    "Task 3: Scale and evolve",
    "Time Estimate",
)
# A message this long, without a paste wrapper, is treated as a paste when it looks like the brief.
LONG_PASTE = 800

PASTED_BLOCK = re.compile(r"<pasted_content[^>]*>(.*?)</pasted_content[^>]*>", re.DOTALL)
TEMP_PATH = re.compile(r"(?<![\w~./:-])(?:/private)?/(?:tmp|var/folders)/[^\s'\"`)\]]+")
OTHER_DRIVE = re.compile(r"/Volumes/(?:\\.|[^/\s\\'\"`)\]])+")
HOME_PATH = re.compile(r"(?<![\w~])/(?:Users|home)/(?:\\.|[^/\s\\'\"`)\]])+")
EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}")

# Words that must never appear in this public repository: the SHA-256 of the word in lowercase, its length and its
# replacement. Matching by hash keeps the words themselves out of this file too. Letters in the text that hash to an
# entry are replaced in any case, and inside longer words such as a domain name or a possessive.
PRIVATE_WORDS = {
    "8e959586273d943ebeb2049cd9c64a4b374f4b0c56edfc5934383f76f73343fb": (5, "[work]"),  # my employer
}


def looks_like_brief(text: str) -> bool:
    if all(title in text for title in BRIEF_TITLE):
        return True
    return sum(heading in text for heading in BRIEF_HEADINGS) >= 2


def replace_brief(text: str) -> str:
    """Replaces a pasted brief with BRIEF_MARKER, keeping my own words around it. Other pastes are kept, unwrapped."""

    def paste(match: re.Match) -> str:
        inner = match.group(1)
        return BRIEF_MARKER if looks_like_brief(inner) else inner.strip("\n")

    text = PASTED_BLOCK.sub(paste, text)
    if len(text) >= LONG_PASTE and looks_like_brief(text):
        return BRIEF_MARKER
    return text


def redact_private_words(text: str, words: dict[str, tuple[int, str]]) -> str:
    out, i = [], 0
    while i < len(text):
        for digest, (length, replacement) in words.items():
            window = text[i : i + length]
            if len(window) == length and window.isalpha() and sha256(window.lower()) == digest:
                out.append(replacement)
                i += length
                break
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()


def redact(text: str) -> str:
    # Order matters: the brief is recognised before anything in it changes, a drive's name is replaced before it could
    # match a private word, and an email address before its domain could.
    text = replace_brief(text)
    text = TEMP_PATH.sub("[temp path]", text)
    text = OTHER_DRIVE.sub("[another drive]", text)
    text = HOME_PATH.sub("~", text)
    text = EMAIL.sub("[email]", text)
    return redact_private_words(text, PRIVATE_WORDS)


# --- Extraction ------------------------------------------------------------------------------------------------------

SYSTEM_REMINDER = re.compile(r"<system-reminder>.*?</system-reminder>", re.DOTALL)
INTERRUPTION = re.compile(r"\[Request interrupted by user[^\]]*\]")
DROPPED_PREFIXES = (
    "Base directory for this skill",  # a skill's instructions, injected when it loads
    "This session is being continued",  # a compaction summary
    "<task-notification>",  # an agent or background command finished
    "Caveat: The messages below were generated by the user while running local commands",
)
COMMAND_WRAPPERS = (
    "<command-name>",
    "<command-message>",
    "<command-args>",
    "<local-command-stdout>",
    "<local-command-stderr>",
    "<local-command-caveat>",
    "<bash-input>",
    "<bash-stdout>",
    "<bash-stderr>",
)


@dataclass(frozen=True)
class Prompt:
    at: datetime
    source: str  # "typed", "queued" (typed while Claude was working), or "answer" or "own answer" to Claude's question
    text: str  # my words, redacted
    context: str = ""  # for an answer: Claude's question and its options, redacted


def timestamp(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(timezone.utc)


def content_text(content) -> str:
    """The text of a message's content; empty for tool results, whose output I didn't write."""
    if isinstance(content, str):
        return content
    if not isinstance(content, list):
        return ""
    if any(isinstance(block, dict) and block.get("type") == "tool_result" for block in content):
        return ""
    return "\n\n".join(
        block.get("text", "") for block in content if isinstance(block, dict) and block.get("type") == "text"
    )


def clean(text: str) -> str | None:
    text = SYSTEM_REMINDER.sub("", text)
    text = INTERRUPTION.sub("", text).strip()
    if not text or text.startswith(DROPPED_PREFIXES) or any(tag in text for tag in COMMAND_WRAPPERS):
        return None
    return text


def answers(record: dict) -> list[Prompt]:
    """My answers to an AskUserQuestion call: the only tool results kept, because I wrote them."""
    result = record.get("toolUseResult")
    if not isinstance(result, dict) or not isinstance(result.get("answers"), dict):
        return []
    notes = result.get("annotations") or {}
    prompts = []
    for question in result.get("questions") or []:
        asked = question.get("question", "")
        answer = result["answers"].get(asked)
        if not answer:
            continue
        labels = [option.get("label", "") for option in question.get("options") or []]
        context = f'Claude asked: "{asked}" The options were ' + ", ".join(f'"{label}"' for label in labels) + "."
        source = "answer" if answer in labels else "own answer"
        note = notes.get(asked)
        note = note.get("notes") if isinstance(note, dict) else note
        text = f"{answer}\n\nNote: {note}" if note else answer
        prompts.append(Prompt(timestamp(record["timestamp"]), source, redact(text), redact(context)))
    return prompts


def prompts_in(record: dict) -> list[Prompt]:
    if record.get("isSidechain"):
        return []
    if record.get("type") == "user":
        if record.get("toolUseResult") is not None:
            return answers(record)
        if record.get("isMeta") or record.get("isCompactSummary"):
            return []
        if (record.get("origin") or {}).get("kind", "human") != "human":
            return []
        text = content_text((record.get("message") or {}).get("content"))
        source = "queued" if record.get("promptSource") == "queued" else "typed"
        at = record.get("timestamp")
    elif record.get("type") == "attachment":
        # A message typed while Claude was working, delivered in the middle of its turn.
        attachment = record.get("attachment") or {}
        if attachment.get("type") != "queued_command" or attachment.get("commandMode") != "prompt":
            return []
        if (attachment.get("origin") or {}).get("kind", "human") != "human":
            return []
        text = content_text(attachment.get("prompt"))
        source = "queued"
        at = attachment.get("timestamp") or record.get("timestamp")
    else:
        return []
    text = clean(text)
    if text is None or not at:
        return []
    return [Prompt(timestamp(at), source, redact(text))]


def read_log(path: Path):
    with path.open(encoding="utf-8") as log:
        for number, line in enumerate(log, start=1):
            if not line.strip():
                continue
            try:
                yield json.loads(line)
            except json.JSONDecodeError:
                print(f"{path.name}:{number}: not JSON, skipped", file=sys.stderr)


def extract(paths: list[Path]) -> list[Prompt]:
    seen, prompts = set(), []
    for path in paths:
        for record in read_log(path):
            for prompt in prompts_in(record):
                key = (prompt.at, prompt.context, prompt.text)
                if key not in seen:
                    seen.add(key)
                    prompts.append(prompt)
    return sorted(prompts, key=lambda prompt: prompt.at)


# --- Markdown --------------------------------------------------------------------------------------------------------

SOURCE_LABELS = {
    "typed": "typed",
    "queued": "typed while Claude was working",
    "answer": "my answer to Claude's question",
    "own answer": "my answer to Claude's question, in my own words rather than one of its options",
}

INTRO = """# Prompts

Everything I said to Claude Code while building this project, in order: what I typed, and how I answered its \
multiple-choice questions. The brief asks for the prompts behind AI-assisted work, and \
[`AI_USAGE.md`](../../AI_USAGE.md) explains the decisions they led to and why I made them.

- **How this file is made:** [`tools/ai/extract_prompts.py`](../../tools/ai/extract_prompts.py) reads Claude Code's \
session logs and writes it. The logs aren't committed: they also hold tool output, local paths and the agents' working.
- **Left out:** system reminders, tool output, task notifications, skill text, compaction summaries, interruption \
markers, command wrappers and empty messages.
- **Redacted:** home paths become `~/`, other drives `[another drive]`, email addresses `[email]` and my employer's \
name `[work]`. The OfferZen brief I pasted in at the start is replaced by a pointer to spec §2, which maps each \
of its requirements to the design.
- **Claude's questions:** where I answered one, the question and its options come first. "(Recommended)" is Claude's \
label for the option it recommended.
- **Not here:** the prompts Claude wrote for its own subagents. The milestone plans in [`docs/plans/`](../plans/) are \
what the implementing agents worked from, and the verifier's reports are in [`docs/qa/`](../qa/).
- **Times** are UTC. Cape Town is two hours ahead."""


def quote(text: str) -> str:
    # Escape anything that looks like an HTML tag, so GitHub shows it as text.
    text = re.sub(r"<(?=[A-Za-z/!?])", "&lt;", text)
    return "\n".join(f"> {line.rstrip()}" if line.strip() else ">" for line in text.splitlines())


def render(prompts: list[Prompt]) -> str:
    parts = [INTRO]
    if prompts:
        last = prompts[-1].at
        parts.append(f"{len(prompts)} prompts, the last at {last:%Y-%m-%d %H:%M} UTC.")
    day = None
    for number, prompt in enumerate(prompts, start=1):
        if prompt.at.date() != day:
            day = prompt.at.date()
            parts.append(f"## {day:%Y-%m-%d}")
        parts.append(f"### Prompt {number}")
        parts.append(f"*{prompt.at:%H:%M} UTC · {SOURCE_LABELS[prompt.source]}*")
        if prompt.context:
            parts.append(prompt.context)
        parts.append(quote(prompt.text))
    return "\n\n".join(parts) + "\n"


# --- Self-check ------------------------------------------------------------------------------------------------------


def check() -> int:
    """Runs every filter and redaction on sample lines and records shaped like Claude Code's; 0 when all pass."""
    failures = []

    def expect(name: str, actual, expected) -> None:
        if actual != expected:
            failures.append(f"{name}:\n  expected {expected!r}\n  got      {actual!r}")

    # Redactions.
    stand_in = {sha256("acme"): (4, "[work]")}  # PRIVATE_WORDS's mechanism, on a word that can be written here
    brief = '<pasted_content id="1">\nSenior Android Engineer\nTechnical Vetting Assignment\n</pasted_content id="1">'
    email = "<pasted_content>Hi!\nAssessment details\n...\nSubmission requirements\nhr@example.com</pasted_content>"
    for name, actual, expected in [
        ("home path", redact("open /Users/someone/Downloads/brief.pdf"), "open ~/Downloads/brief.pdf"),
        ("home path, Linux", redact("/home/someone/notes.md"), "~/notes.md"),
        ("another drive", redact("~/Development and /Volumes/Office"), "~/Development and [another drive]"),
        ("another drive, nested", redact("/Volumes/Office/docs/CLAUDE.md"), "[another drive]/docs/CLAUDE.md"),
        ("temp path", redact("in /private/tmp/claude-1/x.md now"), "in [temp path] now"),
        ("temp path, not in a URL", redact("https://example.com/tmp/x"), "https://example.com/tmp/x"),
        ("email", redact("send it to someone.else@example.co.za, thanks"), "send it to [email], thanks"),
        ("private word, any case", redact_private_words("ACME and acme", stand_in), "[work] and [work]"),
        ("private word, inside words", redact_private_words("Acme's acme.co", stand_in), "[work]'s [work].co"),
        ("private word, others kept", redact_private_words("Acne", stand_in), "Acne"),
        ("private-word table", all(re.fullmatch("[0-9a-f]{64}", d) for d in PRIVATE_WORDS), True),
        ("brief, by its title", redact(f"the brief -> {brief} then go"), f"the brief -> {BRIEF_MARKER} then go"),
        ("brief, by its headings", redact(email), BRIEF_MARKER),
        ("brief, not wrapped", redact("Senior Android Engineer, Technical Vetting Assignment " * 20), BRIEF_MARKER),
        ("other paste kept", redact("<pasted_content>a log line</pasted_content> and more"), "a log line and more"),
    ]:
        expect(name, actual, expected)

    # Filters.
    def user(content, **extra) -> dict:
        return {"type": "user", "timestamp": "2026-11-18T06:33:54Z", "message": {"content": content}, **extra}

    def queued(prompt, mode="prompt") -> dict:
        attachment = {"type": "queued_command", "commandMode": mode, "origin": {"kind": "human"}, "prompt": prompt}
        return {"type": "attachment", "timestamp": "2026-11-18T07:50:45Z", "attachment": attachment}

    def kept(record: dict) -> list[str]:
        return [prompt.text for prompt in prompts_in(record)]

    human = {"origin": {"kind": "human"}, "promptSource": "typed"}
    notification = "<task-notification>done</task-notification>"
    result = [{"type": "tool_result", "tool_use_id": "t1", "content": "ok"}]
    for name, actual, expected in [
        ("typed message", kept(user("approach A", **human)), ["approach A"]),
        ("system reminder", kept(user("merged<system-reminder>x</system-reminder>", **human)), ["merged"]),
        ("tool result", kept(user(result, toolUseResult={"stdout": "ok"})), []),
        ("tool result, no metadata", kept(user(result + [{"type": "text", "text": "a hook's note"}])), []),
        ("task notification", kept(user(notification, origin={"kind": "task-notification"})), []),
        ("task notification, older log", kept(user(notification)), []),
        ("not from me", kept(user('Agent "Spec review" finished', origin={"kind": "task-notification"})), []),
        ("skill injection", kept(user("Base directory for this skill: x", isMeta=True)), []),
        ("skill injection, unflagged", kept(user("Base directory for this skill: x", **human)), []),
        ("screenshot placeholder", kept(user("[Image: original 1080x2400, displayed at 900x2000.]", isMeta=True)), []),
        ("compaction summary", kept(user("This session is being continued from ...", isCompactSummary=True)), []),
        ("compaction summary, flagged only", kept(user("Summary: the spec is done.", isCompactSummary=True)), []),
        ("interruption marker", kept(user([{"type": "text", "text": "[Request interrupted by user]"}])), []),
        ("command wrapper", kept(user("<command-name>/clear</command-name><command-args></command-args>")), []),
        ("command output", kept(user("<local-command-stdout>Done</local-command-stdout>")), []),
        ("empty message", kept(user("   ", **human)), []),
        ("subagent's message", kept(user("do Task 1", isSidechain=True)), []),
        ("typed while Claude worked", kept(queued("use older versions")), ["use older versions"]),
        ("notification while Claude worked", kept(queued(notification, mode="task-notification")), []),
        ("shell command while Claude worked", kept(queued("git status", mode="bash")), []),
    ]:
        expect(name, actual, expected)

    # Answers to Claude's questions.
    question = {"question": "Maps?", "options": [{"label": "Map card (Recommended)"}, {"label": "No maps"}]}
    own = prompts_in(user(result, toolUseResult={"questions": [question], "answers": {"Maps?": "No maps, no data"}}))
    chosen = prompts_in(user(result, toolUseResult={"questions": [question], "answers": {"Maps?": "No maps"}}))
    asked = 'Claude asked: "Maps?" The options were "Map card (Recommended)", "No maps".'
    expect("answer, with the question", [p.context for p in own + chosen], [asked, asked])
    expect("answer in my own words", [(p.source, p.text) for p in own], [("own answer", "No maps, no data")])
    expect("answer from the options", [(p.source, p.text) for p in chosen], [("answer", "No maps")])

    if failures:
        print("\n".join(failures), file=sys.stderr)
        print(f"check: {len(failures)} failed", file=sys.stderr)
        return 1
    print("check: every filter and redaction passed")
    return 0


# --- Command line ----------------------------------------------------------------------------------------------------


def main() -> int:
    parser = argparse.ArgumentParser(description="Write my prompts from Claude Code session logs to Markdown.")
    parser.add_argument("logs", nargs="*", type=Path, help="Claude Code session logs (.jsonl)")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="default: docs/ai/prompts.md")
    parser.add_argument("--check", action="store_true", help="prove the filters and redactions on samples, then exit")
    args = parser.parse_args()
    if args.check:
        return check()
    if not args.logs:
        parser.error("give at least one session log, or --check")
    prompts = extract(args.logs)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(render(prompts), encoding="utf-8")
    counts = Counter(prompt.source for prompt in prompts)
    print(
        f"{len(prompts)} prompts from {len(args.logs)} log(s): {counts['typed']} typed, {counts['queued']} typed while "
        f"Claude was working, {counts['answer'] + counts['own answer']} answers to Claude's questions. "
        f"Review {args.output.name} before publishing it."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())

# Renault Docs — Assistant Response Contract

This contract defines how the assistant should communicate during Renault Docs work.

## Default response shape

For normal project work, use this order:

1. **Main answer**
   - explain what was found, changed, verified, or decided;
   - include only the technical detail needed to understand the current state;
   - distinguish clearly between code/CI evidence and real-phone evidence;
   - do not mark phone PASS without actual phone evidence.

2. **What the user needs to do**
   - end every actionable reply with a short, concrete next-step block;
   - keep it brief;
   - state exactly what the user should press/check/send back;
   - do not bury the user action inside the middle of the explanation.

## Phone-side work rule

The user's normal operational work is done through the **Renault Termux menu**.

Therefore:
- prefer Renault Menu item numbers/names over raw shell commands;
- do not give manual Git/gh/bash commands when the required action already exists in the menu;
- use manual shell commands only for emergency diagnostics or when the menu has no equivalent;
- when a phone action is required, phrase it as a short menu path, e.g. `5 → 19 → 8 → 13`;
- never tell the user to use menu item 7 as the normal install/update path when the established compatible-build flow applies.

## During phone QA

When the user sends a screenshot/result:
- classify the observed gate as PASS / FAIL / finding only when evidence supports it;
- record important findings/checkpoints in project docs before moving on;
- then give exactly the next phone step;
- avoid jumping several gates ahead unless the user asks for the whole checklist.

## End-of-reply convention

For actionable Renault Docs work, finish with a compact section equivalent to:

**Від тебе зараз:** <one or a few concrete actions through Renault Menu / app UI>.

If no user action is required, say so explicitly instead of inventing one.

This project-specific communication rule is durable and should be applied in future chats/sessions.

---
name: human-voice-writing
description: >-
  Use when the user asks to write, draft, edit, or improve any human-facing text
  — blog posts, Slack messages, emails, documentation prose, README sections, or
  social posts. Do not activate for code, technical analysis, or structured
  data.
metadata:
  disable-model-invocation: true
---

# Human Voice Writing

Before delivering any text, complete exactly two passes: draft, then a full scan against every numbered check below. Do not mention this process to the user — just deliver clean output.

---

## Banned words and phrases

Never use these words or their close synonyms:

**AI-signature vocabulary (first wave):** delve, interplay, testament, tapestry, nuanced, multifaceted, pivotal, underscore, leverage (as a verb), streamline, robust, groundbreaking, comprehensive, crucial, vital, foster, realm, beacon, game-changer, cutting-edge, innovative, seamless, holistic

**AI-signature vocabulary (second wave):** dive deep, shed light on, unpack, explore, navigate (metaphorical), journey (metaphorical), evolve, transform (as a business buzzword), empower, supercharge, elevate, unlock, harness, spearhead, champion (as a verb), prioritize (outside product/engineering contexts), synthesize, in this context (as a transition), it's important to remember, let's explore

**Filler openers:** Certainly, Absolutely, Of course, Sure, By all means

**Sycophantic openers:** Great idea, Excellent idea, Excellent point, Great point, You got it, Perfect, Wonderful, Fantastic, That's a great question, What a great question, Happy to help, I'd be happy to — never open a response by praising the user's input. Start with the substance.

**Fake-casual openers:** Let's be honest, Here's the thing:, Here's what you need to know:, The good news is, The bad news is — these are structural tics, not genuine warmth.

**Padding phrases:** It is worth noting that, It is important to note that, It goes without saying, In today's world, In the ever-evolving landscape of, At the end of the day

---

## Banned structures

- **No closing summary paragraph.** Never end with "In conclusion…", "In summary…", "As we have seen…", or any restatement of what was just written.
- **No bullet-by-default.** Use bullets only when items are genuinely list-like and parallel. If the content reads naturally as prose, write prose.
- **No three-part boilerplate.** Avoid the pattern: intro paragraph → bullet list → closing paragraph.
- **No excessive signposting.** Do not write "Firstly… Secondly… Finally…" — just say the thing.
- **No hedge stacking.** One hedge per sentence maximum. Not: "It could be argued that in many cases this might potentially…"
- **No negative parallelism as filler.** "It's not just X, it's Y" is only permitted when the contrast is the genuine point, not decoration.
- **No both-sidesing when one side is clearly stronger.** Take a position.

---

## Required qualities

- **Vary sentence length.** Mix short punchy sentences with longer ones. Uniform medium-length sentences are an AI tell.
- **Use em dashes once per piece maximum** — only if removing it would genuinely weaken the sentence. If you cannot articulate why a comma or a new sentence would be worse, do not use it.
- **Write with a point of view.** The text should have an opinion, a preference, or a recommendation — and that opinion must appear in the first third of the piece, not buried at the end.
- **Match the register to the medium.** A Slack message is casual; a README is direct and technical; a blog post has personality. If the medium was not stated, ask before drafting. Do not apply a uniform formal tone across all contexts.
- **Prefer concrete over abstract.** Replace abstract claims ("it improves efficiency") with specific ones ("it cuts the build time from 4 minutes to 40 seconds").
- **Prefer active voice.** Passive is only acceptable when the actor is genuinely unknown or irrelevant. "It was decided that…", "Users are encouraged to…", "This can be achieved by…" all have actors — name them.

---

## Self-check before delivering

Scan the draft for all six items. Fix before delivering.

1. **Banned words** — any word from either banned list → replace.
2. **Closing summary** — any restatement paragraph at the end → delete.
3. **Sentence-length monotony** — if five or more consecutive sentences are within three words of each other in length, break the rhythm.
4. **Bullet overuse** — in any piece under 300 words, more than one list is too many; convert the weaker one to prose.
5. **Opinion placement** — locate where the clearest opinion or recommendation appears; if it falls in the final third, move it earlier.
6. **Register mismatch** — re-read the first sentence and confirm the register (formal / casual / technical) matches the stated medium.

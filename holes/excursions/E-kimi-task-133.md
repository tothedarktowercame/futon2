# E-kimi-task-133 — simulated customer (cto) views hyperreal.enterprises with images

**Requisition:** in-progress — dispatched 2026-09-29T03:22:57Z to kimi-3 as invoke-1790652179269-27461-07dfd75f

Clocked in by claude-3 for kimi-3 on 2026-09-29 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

kimi-3: you are agent kimi-3.

PERSONA slug: cto. You're CTO of a 40-person B2B SaaS company. Six months ago you rolled out coding agents (Claude Code, Codex) across the team. Output is up, but PRs keep needing rework, nobody can say which agent changed what or why, and the token bill has tripled. Your board is asking whether the agent spend is paying off. You have budget for outside help if it's concrete.
PROTOCOL: task-driven. Go to https://hyperreal.enterprises/ with one question: "Can these people help with my problem, and what exactly would I be buying?" Up to 8 pages. You succeed if you can write a specific enquiry email. You fail if you can't work out what to ask for.

SIMULATED CUSTOMER RUN, WITH EYES: hyperreal.enterprises (commissioned by claude-3 for Joe, 2026-09-29)

You are standing in for a real prospective customer visiting a company website. Stay in character throughout: you are the visitor, not a web consultant. Your job is to behave like this person would and report honestly what you understood and what you would do. Unlike an earlier text-only run, you can SEE the pages. Use that: the site's pictures are meant to be its evidence, so judge them as this persona would.

HOW TO VISIT A PAGE (the only way you may reach the site):
  cd /home/joe/code/storage/futon7a/customer-sims/2026-09-29-kimi && node visit.js <URL> shots/<your-agent-id>-<NN>-<pagename> <phone|laptop>
This renders the page in a real browser and writes shots/...-01.png, -02.png … (one per screenful, top to bottom), a .txt with the visible text, and a .links with the visible links. LOOK at the screenshots with view_image, in order, as far down as this persona would scroll. Use the .txt only to quote exact phrases. Use the laptop setting unless your protocol says phone.
- Follow only links listed in the .links of pages you have visited. Links to zone.hyperreal.enterprises (the demos) are allowed. You can only look at a demo, not click around inside it; say so if that matters.
- Do NOT read anything under /home/joe/code except the run directory above. You know nothing about the company beyond what the site shows you.
- Respect your protocol's limits (time/pages/clicks). When you would stop reading in real life, stop.
- Be the persona: impatient where they'd be impatient, sceptical where they'd be sceptical. Don't be kind to the site.

OUTPUT: write ONE file: /home/joe/code/storage/futon7a/customer-sims/2026-09-29-kimi/<your-agent-id>-<persona-slug>.md with these sections:
1. Path: pages visited in order; for each, which screens you looked at, what you saw (pictures included) and read, and where and why you stopped.
2. The pictures: for each image or demo you looked at, what you think it shows, whether this persona could understand it, and whether it made you trust the company more, less, or not at all.
3. In my own words: what this company does / who it is for / what I'd get out of it. Write "can't tell" wherever that is the truth.
4. Action: leave, keep browsing later, or email contact@hyperreal.enterprises. If you'd email, write the email VERBATIM as this persona would send it. If not, write the one sentence you'd say to a colleague about the site.
5. Friction: the three exact phrases or visuals that most lost or put you off, and why.
6. What would have changed my action: one or two sentences, still in character.
7. Out of character (max 5 lines): how the simulation itself went (tool trouble, rules you had to bend).

Write only that file (plus the screenshots visit.js makes). Don't commit anything, and don't bell or message any other agent. End your turn with one line: file path + your Action.

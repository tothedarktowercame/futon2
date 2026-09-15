"""Package the agent's explicit judgments; no retrieval, scoring or actuation."""
import json, hashlib, re
from pathlib import Path
BASE=Path('/home/joe/code'); OUT=Path(__file__).parent
R=json.loads((OUT/'retrieval.json').read_text()); C=json.loads((OUT/'candidate-text.json').read_text())
MISSION='futon2/holes/missions/M-zaif-harness-v1.md'
def pin(path):
 p=BASE/path; return {'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
def cite(path,start,end):
 p=BASE/path;return {**pin(path),'lines':[start,end],'quote':'\n'.join(p.read_text().splitlines()[start-1:end])}
# Relevance is authored judgment, never the retrieval score.
J={
'agent/provisional-claims-ledger':(True, 'Its IF missing confirmation and THEN provisional ledger address the explicitly pending witness at mission lines 208-215.'),
'agent/evidence-over-assertion':(True,'Its THEN anchors every substantive claim in evidence directly serves the re-derivable reporting gate at mission lines 89-91.'),
'coordination/session-durability-check':(False,'Its HOWEVER accepted-but-not-stored concerns persistence failures; mission lines 101-105 already identify shipped ledger and persistence, while 108-110 name different missing work.'),
'coordination/cross-validation-protocol':(True,'Its HOWEVER single-agent self-validation and THEN replayable validation address independent rerun still pending at mission lines 208-215.'),
'war-machine/operational-not-decorative':(True,'Its THEN derive status from evidence answers the mission lines 44-47 inability to establish component readiness from evidence.'),
'sidecar/append-only-semantic-audit':(False,'Its IF updates overwrite states is not established by mission lines 101-110, which identify a shipped ledger and missing tests rather than overwrite defects.'),
'stack-coherence/maturity-evidence-audit':(True,'Its THEN check claimed readiness against evidence addresses mission lines 44-47, though the mission does not declare devmap maturity tags required by its IF.'),
'stack-coherence/ready-blocked-triage':(True,'Its THEN evidence-and-blocker status reporting fits the pending certificate and explicit gate at mission lines 214-215 and 226-229.'),
'social/verify-before-compose':(True,'Its THEN test namespaces before composition supports per-component tests explicitly missing at mission lines 44-47 and 108-110.'),
'social/tension-before-code':(True,'Its THEN invariant tests before implementation matches the per-node planted expectations in mission lines 150-155; the repaired defects at 208-215 are retrospective evidence of the need.'),
'futon-theory/mission-interface-signature':(True,'Its THEN typed and tested output ports supports the phase-to-node artifact interfaces at mission lines 56-70, though this packet does not redesign those ports.'),
'devmap-coherence/ifr-f1-dhammavicaya':(False,'Its THEN FUTON3 check DSL and MUSN transcript twins targets transport certification, not the missing harness readiness evidence at mission lines 44-47.'),
'storage/open-world-velocity-validation':(False,'Its IF rapid background-knowledge ingest is not the component readiness and witness gap at mission lines 44-47 and 208-215.'),
'wr-1-three-futon-split':(False,'Its THEN split the three repositories addresses organisational timescales, not the post-repair witness obligation at mission lines 208-215.'),
'ready-blocked-triage':(True,'Its THEN evidence-and-blocker status reporting fits the pending certificate and explicit gate at mission lines 214-215 and 226-229.'),
'honest-holes-gate-composed-claims':(True,'Its THEN publish holes or typed none supports the honest gap list required by mission lines 92-94, but its composed dimensions are not enumerated in this mission.'),
'spec-and-implementation-must-converge-to-runtime-certificates':(True,'Its HOWEVER behaviour-matches-intent certificates describes mission lines 44-47 and 208-215, but its THEN routes why edges rather than implementing verification.'),
'storage/rapid-debugging':(False,'Its IF missing storage-error context is not established by the named and already repaired defects at mission lines 208-215.'),
'structural-obstruction-as-theorem':(False,'Its IF failures persist across parameter settings is contradicted as a task framing by concrete defects fixed at mission lines 211-214; no obstruction theorem is sought.'),
'storage/durability-first':(False,'Its IF writes return before XTDB durability is not asserted in mission lines 101-110 and is distinct from the pending replay at 214.'),
'monitors-measure-the-work':(False,'Its IF repairing a stall detector and THEN cursor commissioning do not address the component readiness/reporting and replay scope at mission lines 44-47 and 208-215.'),
'letter-from-the-future':(False,'Its THEN future client letters addresses portfolio direction, while mission lines 72-94 already state explicit completion criteria.'),
'wr-15-head-as-escrow-is-a-sanctioned-pattern':(False,'Its HOWEVER predecessor schema not committed does not describe the landed route map and node registry at mission lines 101-106.'),
'baldwin-ratchet-defeats-darkroom':(False,'Its THEN require the same failing agent to adapt to an upgraded exotype is not the independent reviewer obligation in mission lines 208-215.'),
'classify-over-one-clean-source':(False,'Its IF multiple drifting provenance classifiers is not established by the mission readiness gap at lines 44-47.'),
'elbow-immediate':(False,'Its IF currently deployed harm requiring emergency interruption conflicts with the pending certificate and gated flip at mission lines 214 and 226.'),
'f2/p4':(False,'No local pattern file was resolved for this indexed ID, so its IF/HOWEVER cannot support the mission lines 44-47 readiness claim.'),
'grant-to-pitch':(False,'Its THEN reframe grants as consulting pitches is unrelated to component tests and reporting gates at mission lines 108-110.'),
'have-a-temperament':(True,'Its THEN explicitly authored policy-grain ordering is explicitly cited by the mission ARGUE at lines 140-146; it remains outside the narrow post-repair verification move.')}
ids=[]
for k in ['embedding','baseline-embedding']:ids += [x[0] for x in R[k]['cascade']]
for k in ['tier0','baseline-tier0']:ids += [x['pattern'] for x in R[k]]
assert set(ids)==set(J), (set(ids)-set(J),set(J)-set(ids))
judgments=[]
for pid in dict.fromkeys(ids):
 matches=[c for c in C if c['candidate']==pid]
 judgments.append({'candidate':pid,'relevant':J[pid][0],'judgment':J[pid][1], 'author':'codex-27',
                   'sources':[{**pin(c['path']),'clauses':c['blocks']} for c in matches],
                   'finding':None if matches else 'pattern-source-missing'})
# Fact values are the pinned mission's affirmations/explicit absences, not a live readback.
F=[('components-built',True,101,105,'Ready route-map components are available, including dark/opt-in ones.'),
   ('node-spec-map-exists',True,118,125,'The design table names node, component and unit test.'),
   ('complete-per-node-suite-exists',False,108,110,'The complete per-node suite is explicitly Missing; later specific regression tests do not assert completion of that suite.'),
   ('reporting-gate-exists',False,108,110,'The reporting gate U8 is explicitly Missing in MAP.'),
   ('regression-fixes-landed',True,208,214,'All three witness-discovered defects are reported fixed with regression tests.'),
   ('independent-replay-confirmed',False,213,215,'Rerun requested and Lean status pending; confirmation is not established.'),
   ('pending-status-recorded',True,213,215,'Pending is explicitly recorded, not inferred from silence.'),
   ('replay-request-recorded',True,213,215,'Re-run requested is affirmative documentary evidence of a request only.'),
   ('certificate-promotion-complete',False,226,229,'Certificate flip remains gated on the rerun.')]
facts=[{'id':i,'q0':v,'meaning':meaning,'source':cite(MISSION,a,b),'authority':'mission-document-assertion','availability':'available' if v else 'outstanding-to-WANT'} for i,v,a,b,meaning in F]
def fact(i):return ['fact',i]
def NOT(i):return ['not',fact(i)]
def AND(*args):return ['and',*args]
# Effects below are completion contracts, never claims that issuing a request succeeds.
I=[('agent/evidence-over-assertion',AND(fact('components-built'),NOT('reporting-gate-exists')),{'reporting-gate-exists':True},'Implement and independently validate U8 so its substantive claims point to queryable typed evidence; the effect is admitted only after the gate tests pass.'),
   ('war-machine/operational-not-decorative',AND(fact('node-spec-map-exists'),NOT('complete-per-node-suite-exists')),{'complete-per-node-suite-exists':True},'Make every mapped component testable against its spec; completion requires the complete per-node suite, not just the three regression tests.'),
   ('coordination/cross-validation-protocol',AND(fact('regression-fixes-landed'),NOT('independent-replay-confirmed')),{'replay-request-recorded':True},'Invoke independent replay through the evidence chain; this projected effect records only the request, never successful replay.'),
   ('stack-coherence/ready-blocked-triage',AND(fact('components-built'),NOT('independent-replay-confirmed')),{'pending-status-recorded':True},'Project the clause-classification move onto the mission certificate obligation and emit pending with its replay blocker; devmap syntax is adapted explicitly to this obligation.'),
   ('social/verify-before-compose',AND(fact('components-built'),NOT('complete-per-node-suite-exists')),{'complete-per-node-suite-exists':True},'Load and smoke-test each mapped component before integration; require completion evidence for all mapped components before admitting this postcondition.'),
   ('social/tension-before-code',AND(fact('node-spec-map-exists'),NOT('complete-per-node-suite-exists')),{'complete-per-node-suite-exists':True},'Write per-node invariant tests from the spec first and validate their negative controls; this is a design alternative to smoke tests, not evidence that tests already exist.')]
interpretations=[]
for pid,guard,effect,note in I:
 c=next(c for c in C if c['candidate']==pid)
 interpretations.append({'pattern':pid,'authority':'documented-interpretation','author':'codex-27','source':pin(c['path']), 'quotes':c['blocks'],'guard':guard,'effect':effect,'argument':note,'effect-semantics':'postcondition of successfully completed witnessed work; no guaranteed transition probability assigned'})
new='coordination/bind-promotion-to-post-repair-replay';np='futon3/library/'+new+'.flexiarg';text=(BASE/np).read_text();blocks={}
for key in ['IF','HOWEVER','THEN']:
 blocks[key]=re.search(r'(?ms)^  \+ '+key+r':(.*?)(?=^  \+ |\Z)',text).group(1).strip()
interpretations.append({'pattern':new,'authority':'documented-interpretation','author':'codex-27','source':pin(np),'quotes':blocks,
 'guard':AND(fact('regression-fixes-landed'),NOT('independent-replay-confirmed')),
 'effect':{'replay-request-recorded':True,'pending-status-recorded':True},
 'argument':'Request/retain replay and pending status only. q0 already has both, so this move is idempotent here, not claimed progress. Promotion requires a future independently validated matching receipt; the named Boolean alone does not encode revision identity.',
 'effect-semantics':'request and pending-status bookkeeping only; never sets independent-replay-confirmed or certificate-promotion-complete'})
interpreted={i['pattern'] for i in interpretations};holes=[]
for j in judgments:
 if not j['relevant']:continue
 pid=j['candidate']
 if pid=='ready-blocked-triage' or pid in interpreted:continue
 reason={'agent/provisional-claims-ledger':'No mission facts specify expiry/owner lifecycle for provisional ledger entries; pending status alone is not that full ledger.',
 'stack-coherence/maturity-evidence-audit':'Mission readiness facts do not assert devmap maturity tags; do not invent them to satisfy IF.',
 'honest-holes-gate-composed-claims':'Composed dimensions and their checked hole census are absent from the selected facts; cannot assert a typed-none census.',
 'spec-and-implementation-must-converge-to-runtime-certificates':'Problem node THEN routes why edges, not an executable witness action.',
 'futon-theory/mission-interface-signature':'Port-level types, baseline inventory and consumers are beyond the selected named facts.',
 'have-a-temperament':'Cascade-order edits need a policy carrier; this packet contains mission facts only.'}[pid]
 holes.append({'kind':'missing-pattern-interpretation','pattern':pid,'reason':reason})
metrics={}
for key in ['embedding','baseline-embedding','tier0','baseline-tier0']:
 candidates=[x[0] for x in R[key]['cascade']] if 'embedding' in key else [x['pattern'] for x in R[key]]
 metrics[key]={'returned':len(candidates),'judged-relevant':sum(J[i][0] for i in candidates)}
record={'schema':'wm/interpreted-pattern-set-v1','author':'codex-27','authority':'documented-interpretation','status':'design-only','scoring-permitted':False,
 'target':{'id':'M-zaif-harness-v1','source':pin(MISSION),'status-source':cite(MISSION,3,9),
 'rule':'Tension-clarity fallback over the inventory of four requested mission roots, not the Phase-8 allow-list: this mission explicitly names built components, missing readiness tests and a concrete fixed-but-unreplayed witness. Rank 3 in a retained unrestricted backtrace is corroboration only, not a current scheduler authorization.',
 'ranking-finding':'Latest inspected tick fb96bf07 contains inherited repair-backtrace rankings, not a fresh unrestricted current ranking; do not mislabel their dates.',
 'selection-record':pin(str((OUT/'target-selection.edn').relative_to(BASE))), 'allow-list-filtered':False},
 'retrieval':R,'candidate-judgments':judgments,'comparison':{'counts':metrics,'qualification':'Single mission, author-judged relevance of each returned candidate; unequal constructor lengths, no general retrieval-quality proof. Both arms run before new pattern registration.'},
 'facts':facts,'state':{'kind':'named-Boolean-mission-facts','q0':{i:v for i,v,*_ in F},'WANT':[i for i,v,*_ in F if not v], 'source-time':'Pinned mission assertions including Sep-12 checkpoint; not independent confirmation of present runtime.'},
 'guard-language':{'fact':'lookup named Boolean','not':'Boolean negation','and':'all operands true'},
 'interpretations':interpretations,'new-patterns':[{'id':new,'source':pin(np),'gap':'Retrieved independent-validation pattern omits explicit post-repair revision matching and request-versus-completion distinction.','index-row':'futon3/resources/sigils/patterns-index.tsv','sigil':'🐜/予','validation':'futon3.chops/stamp valid, lili/e'}],
 'findings':[{'kind':'missing-preference-C','reason':'Joe question remains open; no coverage or named-fact preference rows invented.'},
 {'kind':'runtime-state-unverified','reason':'q0 records the mission document, not fresh measurements; rerun receipt and complete suite may have changed elsewhere.'},
 {'kind':'stochastic-outcomes-unmodelled','reason':'Test/replay success is not guaranteed by executing a design move; effect contracts require witnessed completion and cannot yet feed deterministic CascadeEFE transitions.'},
 {'kind':'revision-identity-not-in-Boolean-carrier','reason':'New pattern documents source-bound promotion but this fact projection cannot validate revision equality; promotion remains a hole.'}]+holes}
(OUT/'interpreted-pattern-set.json').write_text(json.dumps(record,indent=2)+'\n')
print(metrics);print('interpretations',len(interpretations),'holes',len(holes))

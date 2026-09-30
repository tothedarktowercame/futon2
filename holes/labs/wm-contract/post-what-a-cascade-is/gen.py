#!/usr/bin/env python3
"""Generate the zone post from /tmp/claude-1/post/examples.json (+ optional lean.html fragment)."""
import json, html, os, sys
D = json.load(open('/tmp/claude-1/post/examples.json'))
OUT = sys.argv[1] if len(sys.argv) > 1 else '/tmp/claude-1/post/post.html'
LEAN = open('/tmp/claude-1/post/lean.html').read() if os.path.exists('/tmp/claude-1/post/lean.html') else ''
STATUS = open('/tmp/claude-1/post/status.html').read() if os.path.exists('/tmp/claude-1/post/status.html') else ''
E = html.escape
T = {t['target']: t for t in D['targets']}

def short(pid):
    ns, _, name = pid.rpartition('/')
    return ns, name

def wrap(s, n=24):
    words, lines, cur = s.replace('-', '- ').split(' '), [], ''
    for w in words:
        if len(cur) + len(w) > n and cur:
            lines.append(cur); cur = w
        else:
            cur += w
    if cur: lines.append(cur)
    return lines[:3]

def svg(p):
    units = p['units']; ids = [u['id'] for u in units]
    desc = [tuple(d) for d in p['descent']]
    depth = {i: 0 for i in ids}
    for _ in range(len(ids) + 1):
        for a, b in desc:
            if a in depth and b in depth and depth[b] < depth[a] + 1:
                depth[b] = depth[a] + 1
    layers = {}
    for i in ids: layers.setdefault(depth[i], []).append(i)
    W, H, GX, GY = 190, 58, 22, 46
    maxn = max(len(v) for v in layers.values())
    LM = 64
    width = LM + maxn * (W + GX) + GX; height = (max(layers) + 1) * (H + GY) + GY + 14
    pos = {}
    for d, row in layers.items():
        off = LM + (width - LM - (len(row) * (W + GX) - GX)) / 2
        for k, i in enumerate(row):
            pos[i] = (off + k * (W + GX), GY / 2 + 14 + d * (H + GY))
    authored = {}
    for e in (p.get('raw_edges') or []):
        authored[frozenset((e['a'], e['b']))] = (e.get('direction') is not None, e.get('kind-used') or '')
    o = [f'<svg viewBox="0 0 {width:.0f} {height:.0f}" width="{min(width,1100):.0f}" role="img" xmlns="http://www.w3.org/2000/svg" style="max-width:100%;height:auto;font-family:inherit">',
         '<defs><marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#333"/></marker>'
         '<marker id="ag" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0,0 L10,5 L0,10 z" fill="#b5651d"/></marker></defs>']
    for d in layers:
        y = GY / 2 + 14 + d * (H + GY) + H / 2
        o.append(f'<text x="4" y="{y:.0f}" font-size="10" fill="#777">round {d+1}+</text>')
    kinds = {(e['from'], e['to']): e['kind'] for e in p['edges']}
    for a, b in desc:
        (x1, y1), (x2, y2) = pos[a], pos[b]
        key = frozenset((a, b)); is_ret = p['kind'] == 'retraction'
        auth, gk = authored.get(key, (True, ''))
        col, mark, dash = ('#333', 'ah', '') if (auth or not is_ret) else ('#b5651d', 'ag', ' stroke-dasharray="5 3"')
        o.append(f'<line x1="{x1+W/2:.0f}" y1="{y1+H:.0f}" x2="{x2+W/2:.0f}" y2="{y2:.0f}" stroke="{col}" stroke-width="1.3"{dash} marker-end="url(#{mark})"/>')
        lab = gk if is_ret else ''
        if lab:
            o.append(f'<text x="{x1+W/2+0.72*(x2-x1)+5:.0f}" y="{y1+H+0.72*(y2-y1-H)+3:.0f}" font-size="9" fill="{col}">{E(lab)}</text>')
    for e in p['edges']:
        if e['kind'] == 'overlap' and e['from'] in pos and e['to'] in pos:
            (x1, y1), (x2, y2) = pos[e['from']], pos[e['to']]
            o.append(f'<line x1="{x1+W:.0f}" y1="{y1+H/2:.0f}" x2="{x2:.0f}" y2="{y2+H/2:.0f}" stroke="#2a6f97" stroke-width="1.3" stroke-dasharray="2 3"/>')
    roots = set(p['roots'])
    for u in units:
        x, y = pos[u['id']]; ns, name = short(u['pattern'])
        fill = '#eef6ee' if u['id'] in roots else '#fff'
        o.append(f'<rect x="{x:.0f}" y="{y:.0f}" width="{W}" height="{H}" rx="6" fill="{fill}" stroke="#333"/>')
        o.append(f'<text x="{x+6:.0f}" y="{y+12:.0f}" font-size="9" fill="#666">{E(ns)}{(" · fragment " + str(u["fragment"])) if u["fragment"] is not None else ""}</text>')
        for k, ln in enumerate(wrap(name)):
            o.append(f'<text x="{x+6:.0f}" y="{y+26+k*12:.0f}" font-size="11">{E(ln)}</text>')
    o.append('</svg>')
    return '\n'.join(o)

def fnum(x): return f'{x:.2f}'

def policy_block(t, p, n):
    g = p['g_terms']
    kind = {'reading-alternatives': 'read from the text, one pattern per fragment',
            'reading-overlap': 'read from the text, all cited patterns of a fragment together',
            'retraction': 'cut from the pattern graph around the reading’s patterns'}[p['kind']]
    npat = len({u['pattern'] for u in p['units']})
    auth = [e for e in (p.get('raw_edges') or []) if e.get('direction')]
    extra = ''
    if p['kind'] == 'retraction':
        tot = len(p.get('raw_edges') or [])
        extra = f' Of its {tot} graph edges, {len(auth)} carry an authored direction; the rest are drawn dashed in brown.'
    return (f'<h4>Policy {n}: {E(kind)}</h4>'
            f'<p>{len(p["units"])} units over {npat} distinct patterns; {len(p["descent"])} ordering edges; {len(p["roots"])} root{"s" if len(p["roots"])!=1 else ""}.{extra}</p>'
            f'<div class="table-scroll">{svg(p)}</div>'
            f'<table class="terms"><tr><th>F (fit)</th><th>G</th><th>risk</th><th>ambiguity</th><th>information gain</th></tr>'
            f'<tr><td>{fnum(p["F"])}</td><td>{fnum(p["G"])}</td><td>{fnum(g["risk"])}</td><td>{fnum(g["ambiguity"])}</td><td>{fnum(g["expected-information-gain"])}</td></tr></table>')

def target_block(name, note, featured=True):
    t = T[name]
    o = [f'<h3 id="{E(name)}">{E(name)}</h3>', note,
         f'<p class="evidence">Source: <code>{E(t["source_path"].replace("/home/joe/code/",""))}</code>, {E(t["source_kind"])}, {len(t["source_text"])} characters.</p>',
         '<details><summary>The text that was read</summary><pre class="src">' + E(t['source_text']) + '</pre></details>',
         '<details' + (' open' if featured else '') + '><summary>How 象 read it: fragments and the patterns cited</summary><table class="frags"><tr><th>#</th><th>fragment</th><th>role</th><th>patterns cited</th><th>patterns considered and rejected</th></tr>']
    for f in t['fragments']:
        if not f['refs'] and not f['rejections']: continue
        o.append(f'<tr><td>{f["index"]}</td><td>{E(f["text"].strip()[:160])}{"…" if len(f["text"].strip())>160 else ""}</td><td>{E(", ".join(f["relations"] or []))}</td><td>{"<br>".join(E(r) for r in f["refs"])}</td><td>{"<br>".join(E(r) for r in f["rejections"])}</td></tr>')
    o.append('</table></details>')
    fl = '; '.join(f'{E(f["kind"])} ({E(", ".join(f.get("seeds") or []))})' for f in t['failures']) or 'none'
    o.append(f'<p>Policy set: {t["reported"]} cascades reported, {t["distinct"]} structurally distinct. Counted failures: {fl}.</p>')
    for n, p in enumerate(t['policies'], 1):
        o.append(policy_block(t, p, n))
    return '\n'.join(o)

pin = D['pin_raw']
npol = sum(len(t['policies']) for t in D['targets'])
body = open('/tmp/claude-1/post/body.html').read()
ex = {
 'WEB': target_block('M-web-arxana-ui-improvements', '<p>The smallest example, and an awkward one. This mission’s HEAD section contains only the template sentence that every mission file starts with, so the reading is a reading of boilerplate. 象 cited two patterns for that one sentence. The policy set is still six cascades.</p>'),
 'ESS': target_block('M-essays-diachronic-model', '<p>A short opening paragraph. One of the two cited patterns has no edges in the pattern graph, so it cannot seed a retraction; the retraction that remains is a single pattern.</p>'),
 'SDS': target_block('M-self-documenting-stack', '<p>A longer opening. This is the target the joint run chose on 30 September, through its three retractions. Look at how the three differ.</p>'),
 'REST': '\n'.join('<details><summary>' + E(n) + '</summary>' + target_block(n, '', False) + '</details>' for n in ['M-distributed-proofreaders', 'M-value-creation-loop', 'M-metric-harness', 'M-war-machine-aif-completion']),
 'PIN': f'{pin["pattern-id-count"]:,} library patterns, {pin["edge-count"]:,} edges, {pin["nodes-without-edges"]} patterns with no edge, largest connected part {pin["giant-component-size"]}; built from {pin["inputs"]["analyses"]:,} analysed turns plus {pin["inputs"]["authored-relations"]["why"]} authored “why” and {pin["inputs"]["authored-relations"]["how"]} authored “how” relations; file digest <code>{pin["sha256"][:12]}…</code>',
 'NPOL': str(npol), 'LEAN': LEAN, 'STATUS': STATUS,
}
for k, v in ex.items(): body = body.replace('{{' + k + '}}', v)
open(OUT, 'w').write(body)
print('wrote', OUT, len(body))

#!/usr/bin/env python3
"""Extract deterministic cross-document incidence hypergraph."""
import hashlib, json, re, subprocess
from pathlib import Path

ROOT = Path('/home/joe/code')
REPOS = ['futon2','futon3','futon3a','futon3b','futon3c','futon4','futon5','futon5a','futon6']
OUT = Path(__file__).with_name('graph.edn')

def git_sha(repo):
    return subprocess.check_output(['git','-C',str(ROOT/repo),'rev-parse','HEAD'], text=True).strip()

def classify(rel):
    s = rel.as_posix()
    n = rel.name
    if re.search(r'(^|/)holes/missions/M-[^/]*\.md$', s): return 'mission'
    if re.search(r'(^|/)holes/excursions/E-[^/]*\.md$', s): return 'excursion'
    if re.search(r'(^|/)holes/problems/P-[^/]*\.md$', s): return 'problem'
    if '/library/problems/' in '/'+s and rel.is_file(): return 'problem'
    if '/holes/tickets/' in '/'+s and rel.is_file(): return 'ticket'
    if n.startswith('TICKET-') and rel.is_file(): return 'ticket'
    return None

def files(repo):
    base=ROOT/repo
    ans=[]
    for p in base.rglob('*'):
        if not p.is_file(): continue
        rel=p.relative_to(base)
        if any(x in {'.git','data','.history'} for x in rel.parts): continue
        typ=classify(rel)
        if typ: ans.append((rel,typ))
    return sorted(ans,key=lambda x:x[0].as_posix())

def main():
    nodes=[]
    for repo in REPOS:
        for rel,typ in files(repo):
            nodes.append({'id':f'{repo}:{rel.as_posix()}','type':typ,'repo':repo,'path':rel.as_posix()})
    nodes.sort(key=lambda x:x['id'])
    aliases={}
    for n in nodes:
        p=Path(n['path'])
        for a in {p.name,p.stem,n['path']}:
            if len(a)>=4: aliases.setdefault(a,set()).add(n['id'])
    alias_pattern=re.compile(r'(?<![A-Za-z0-9_-])('+('|'.join(re.escape(a) for a in sorted(aliases,key=lambda x:(-len(x),x))))+r')(?![A-Za-z0-9_-])')
    edges=[]; isolates=[]
    for n in nodes:
        text=(ROOT/n['repo']/n['path']).read_text(errors='replace')
        refs=set()
        for match in alias_pattern.finditer(text): refs.update(aliases[match.group(1)])
        refs.discard(n['id'])
        if refs: edges.append({'source':n['id'],'members':sorted(refs)})
        else: isolates.append(n['id'])
    census={r:{t:sum(1 for n in nodes if n['repo']==r and n['type']==t) for t in ['mission','excursion','problem','ticket']} for r in REPOS}
    payload={'schema':'d1-cross-reference-hypergraph-v1','repo-shas':{r:git_sha(r) for r in REPOS},
      'extraction-rule':'Enumerate mission M-*.md, excursion E-*.md, problem P-*.md, every regular file under library/problems, and every regular file under holes/tickets or named TICKET-*; skip any path containing .git, data, or .history. For each source document, exact boundary-match any corpus basename, stem, or repo-relative path in its text; its distinct referenced document ids are one hyperedge. Exclude self references and omit empty hyperedges.',
      'ticket-rule':'A ticket is any regular file under holes/tickets or whose basename begins TICKET-.',
      'census-by-repo-and-type':census,'nodes':nodes,'hyperedges':edges,'reference-isolates':isolates}
    OUT.write_text(json.dumps(payload,sort_keys=True,indent=2)+'\n')
    print(hashlib.sha256(OUT.read_bytes()).hexdigest(),OUT)
if __name__=='__main__': main()

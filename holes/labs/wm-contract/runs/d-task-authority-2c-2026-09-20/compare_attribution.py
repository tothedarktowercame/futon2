import re,json,pathlib,collections,hashlib
root=pathlib.Path(__file__).resolve().parent
def extract(name):
 s=(root/name).read_text()
 result=[]
 for m in re.finditer(r'^(FAIL|ERROR) in .*$',s,re.M):
  lines=s[m.end():].splitlines()
  expected=next((x for x in lines[:8] if x.startswith('expected:')),None)
  actual=next((x for x in lines[:8] if x.startswith('  actual:')),None)
  result.append({'header':m.group(),'line':s[:m.start()].count('\n')+1,'expected':expected,'actual':actual})
 return result
a=extract('baseline-full-runner.log');b=extract('precommit-full-loop-runner.log')
def expected_key(r):
 return (r['header'],re.sub(r'__\d+#','__GENSYM#',r['expected'] or ''))
ka=collections.Counter(expected_key(r) for r in a)
kb=collections.Counter(expected_key(r) for r in b)
comparison={'comparison-rule':'failure/error header including assertion source location plus expected expression; only compiler gensym suffixes normalized. Raw diagnostics retained, not asserted equal.', 'baseline-counts':dict(collections.Counter(r['header'].split()[0] for r in a)),'draft-counts':dict(collections.Counter(r['header'].split()[0] for r in b)),
'draft-only-signatures':list((kb-ka).elements()),'baseline-only-signatures':list((ka-kb).elements()),'baseline':a,'draft':b,
'log-sha256':{n:hashlib.sha256((root/n).read_bytes()).hexdigest() for n in ['baseline-full-runner.log','precommit-full-loop-runner.log']}}
(root/'attribution-comparison.json').write_text(json.dumps(comparison,indent=2)+'\n')
print(json.dumps({k:v for k,v in comparison.items() if k not in ['baseline','draft']},indent=2))

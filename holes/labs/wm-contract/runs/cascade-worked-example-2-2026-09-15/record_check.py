"""Execute exact constructed tests; enumerate the sampling outcome space."""
from pathlib import Path
import hashlib
import json

HERE = Path(__file__).resolve().parent
sha = lambda b: hashlib.sha256(b).hexdigest()
enc = lambda x: json.dumps(x, sort_keys=True, separators=(',', ':')).encode()
TESTS = list(range(10))

def execute(artifact):
    return [artifact[i] == 0 for i in TESTS]

def capture(artifact):
    return {'artifact': artifact, 'code_sha': sha(enc(artifact)),
            'tests': TESTS, 'test_sha': sha(enc(TESTS)),
            'environment': 'Python deterministic integer equality, isolated fixture',
            'results': execute(artifact)}

def verify(record):
    if sha(enc(record['artifact'])) != record['code_sha'] or sha(enc(record['tests'])) != record['test_sha']:
        return 'refusal'
    # This reads the retained result, NOT another call to execute.
    return 'clean' if all(record['results']) else 'buggy'

rows = []
for strength in ['dense', 'sparse']:
    clean = [0]*10
    bug = [1]*10 if strength == 'dense' else [0]*9+[1]
    for truth,artifact in [('clean',clean),('buggy',bug)]:
        record = capture(artifact)
        result = verify(record)
        # Exact likelihoods of the observed result vector under the two hypotheses.
        likelihood = [int(execute(hypothesis) == record['results']) for hypothesis in [clean,bug]]
        posterior = [x/sum(likelihood) for x in likelihood]
        confidence = max(posterior)
        samples = [{'index': i, 'passes': artifact[i] == 0,
                    'record_verdict': result,
                    'confident_true': confidence >= .95 and result == truth}
                   for i in TESTS]
        assert all(s['confident_true'] for s in samples)
        altered = dict(record, code_sha='0'*64)
        assert verify(altered) == 'refusal'
        rows.append({'strength':strength,'truth':truth,'record':record,
                     'posterior_clean_buggy':posterior,'confidence':confidence,
                     'samples':samples,'true_verdict_probability':1.0,
                     'full_rerun_verdict':'clean' if all(execute(artifact)) else 'buggy'})
# Diagnostic without decisive record: sparse sampling pass with 1/2 prior.
p_clean_given_pass = .5/(.5+.5*.9)
assert p_clean_given_pass < .95
out = {'scope':'executed record consistency check, full A/B computation blocked on model inputs',
       'predeclaration_sha256':sha((HERE/'PREDECLARATION.md').read_bytes()),
       'rows':rows,'no_record_sparse_pass_posterior_clean':p_clean_given_pass,
       'finding':'A complete deterministic retained result detects both bugs regardless of sampled index.',
       'not_computed':['G_A','G_B','fuel','adequacy-error-dependent fallback costs']}
(HERE/'results.json').write_text(json.dumps(out,indent=2)+'\n')
print('4 artifact/strength cases; 40 sampled draws; all record verdicts confidently true.')
print('Without the decisive record, sparse sampling pass gives P(clean)=',p_clean_given_pass)

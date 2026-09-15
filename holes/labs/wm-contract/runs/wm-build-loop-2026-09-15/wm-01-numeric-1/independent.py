"""Independent represented totals from literal fixtures, without Clojure code.
IEEE values are exposed as float.hex and interpreted with Fraction.from_float.
Exact EDN ratios/integers/BigDecimal literals never pass through float.
"""
import json
import re
from fractions import Fraction
from pathlib import Path
root = Path(__file__).resolve().parent
cases = {}
for name, body in re.findall(r':([\w-]+)\s+\{([^{}]*)\}', (root / 'cases.edn').read_text()):
    values = []
    for key, literal in re.findall(r':([\w-]+)\s+([^\s,}]+)', body):
        ieee = '.' in literal or 'e' in literal.lower()
        if literal.endswith('M'):
            value = Fraction(literal[:-1]); encoding = {'decimal': literal[:-1]}
        elif ieee:
            hexbits = float(literal).hex()
            value = Fraction.from_float(float.fromhex(hexbits)); encoding = {'ieee64-hex': hexbits}
        else:
            value = Fraction(literal); encoding = {'exact': literal}
        values.append({'support': key, 'literal': literal, **encoding, 'exact': str(value)})
    total = sum((Fraction(v['exact']) for v in values), Fraction(0))
    cases[name] = {'values': values, 'total': str(total), 'deviation': str(abs(total-1))}
(root / 'independent.json').write_text(json.dumps(cases, indent=2, sort_keys=True)+'\n')
(root / 'independent.edn').write_text('{\n'+''.join(
    f' :{name} {{:total {v["total"]} :deviation {v["deviation"]}}}\n'
    for name,v in sorted(cases.items()))+'}\n')
for name, v in sorted(cases.items()):
    print(name, 'total='+v['total'], 'deviation='+v['deviation'])

"""Independent Decimal(90) reference, not a call to the Clojure implementation."""
from decimal import Decimal as D, localcontext
from pathlib import Path
with localcontext() as ctx:
    ctx.prec = 90
    q = [D(1)/3, D(2)/3]
    a = [[D(3)/4, D(1)/4], [D(1)/5, D(4)/5]]
    c = [D(2)/5, D(3)/5]
    j = [[q[s]*a[s][o] for o in range(2)] for s in range(2)]
    marginal = [sum(j[s][o] for s in range(2)) for o in range(2)]
    risk = sum(marginal[o]*(marginal[o]/c[o]).ln() for o in range(2))
    amb = -sum(j[s][o]*a[s][o].ln() for s in range(2) for o in range(2))
    cross = -sum(marginal[o]*c[o].ln() for o in range(2))
    mi = sum(j[s][o]*(j[s][o]/(q[s]*marginal[o])).ln() for s in range(2) for o in range(2))
    values = dict(risk=risk, ambiguity=amb, g=risk+amb,
                  **{'preference-cross-entropy':cross, 'mutual-information':mi,
                     'equivalent-g':cross-mi})
    Path(__file__).with_name('reference.edn').write_text(
        '{'+ '\n '.join(':'+k+' '+str(v)+'M' for k,v in values.items())+'}\n')
    print('Decimal precision:',ctx.prec,'G:',values['g'])

#!/usr/bin/env python3
"""Numerically interpret the generated Lean Real.log expressions.

This deliberately is not a second Clojure implementation. Inputs are exact
binary64 rationals emitted beside the Lean terms. Decimal arithmetic at 90
digits supplies an independent numerical interpretation of the symbolic law.
"""
import csv
from decimal import Decimal, getcontext
from fractions import Fraction

getcontext().prec = 90

def dec_ratio(text):
    q = Fraction(text)
    return Decimal(q.numerator) / Decimal(q.denominator)

def pi():
    # Gauss-Legendre; 7 iterations exceed the 90-digit working precision.
    one, two = Decimal(1), Decimal(2)
    a, b, t, p = one, one / two.sqrt(), Decimal("0.25"), one
    for _ in range(7):
        an = (a + b) / two
        b = (a * b).sqrt()
        t -= p * (a - an) ** 2
        a, p = an, two * p
    return (a + b) ** 2 / (Decimal(4) * t)

totals = {}
with open("holes/labs/wm-contract/runs/row-16-r8-policy-f-2026-09-12/symbolic-input.tsv") as f:
    for row in csv.DictReader(f, delimiter="\t"):
        r, v = dec_ratio(row["residual"]), dec_ratio(row["variance"])
        term = ((Decimal(2) * pi() * v).ln() + r*r/v) / Decimal(2)
        totals[row["candidate"]] = totals.get(row["candidate"], Decimal(0)) + term

with open("holes/labs/wm-contract/runs/row-16-r8-policy-f-2026-09-12/symbolic-reference.tsv", "w") as f:
    f.write("candidate\tdecimal90\tbinary64\n")
    for candidate in sorted(totals, key=lambda x: int(x)):
        value = totals[candidate]
        f.write(f"{candidate}\t{value}\t{float(value)!r}\n")

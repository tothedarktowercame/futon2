import json,pathlib
from fractions import Fraction
R=pathlib.Path(__file__).resolve().parent
W=pathlib.Path((R/'workspace.txt').read_text().strip())
D=W/'DarkTower/WarMachine'
p=D/'MachineModelSpec.lean'; s=p.read_text(); s=s.replace('  nearNormalised :', '  support_nodup : support.Nodup\n  mass_eq_zero_of_not_mem : ∀ o, o ∉ support → mass o = 0\n  nearNormalised :')
s=s.replace('end DarkTower.WarMachine.MachineModelSpec','''/-- The existing tolerance rules out empty support; no extra field is needed. -/
theorem FloatCarriedRow.support_ne_nil {O : Type} (r : FloatCarriedRow O) :
    r.support ≠ [] := by
  intro h
  have bound := r.nearNormalised
  rw [h] at bound
  norm_num [floatRowBound] at bound

/-- Preserve the rational coordinates. Exact normalization is an explicit premise,
not a consequence of approximate admission. No renormalization is performed. -/
noncomputable def FloatCarriedRow.toProbabilityKernel {O : Type}
    (r : FloatCarriedRow O) (h : (r.support.map r.mass).sum = 1) :
    ProbabilityKernel Unit O where
  support _ := r.support
  mass _ o := (r.mass o : ℝ)
  nonnegative _ o := by exact_mod_cast r.nonnegative o
  normalised _ := by
    have cast_sum : ∀ xs : List O,
        (xs.map (fun o => (r.mass o : ℝ))).sum = ((xs.map r.mass).sum : ℚ) := by
      intro xs
      induction xs with
      | nil => simp
      | cons o xs ih => simp [ih]
    rw [cast_sum, h]
    norm_num
  support_nodup _ := r.support_nodup
  mass_eq_zero_of_not_mem _ o ho := by simp [r.mass_eq_zero_of_not_mem o ho]

theorem FloatCarriedRow.toProbabilityKernel_coordinate {O : Type}
    (r : FloatCarriedRow O) (h : (r.support.map r.mass).sum = 1) (o : O) :
    (r.toProbabilityKernel h).mass () o = (r.mass o : ℝ) := rfl

theorem FloatCarriedRow.toProbabilityKernel_normalised {O : Type}
    (r : FloatCarriedRow O) (h : (r.support.map r.mass).sum = 1) :
    ((r.toProbabilityKernel h).support () |>.map
      ((r.toProbabilityKernel h).mass ())).sum = 1 :=
  (r.toProbabilityKernel h).normalised ()

end DarkTower.WarMachine.MachineModelSpec''')
p.write_text(s)
p=D/'MachineForwardModelWitness.lean'; s=p.read_text().replace('  support := allO','  support := allO\n  support_nodup := by simp [allO]\n  mass_eq_zero_of_not_mem := by intro o h; cases o <;> simp_all [allO]'); p.write_text(s)
values=json.loads((R.parent/'wm-01-binding-1/independent-arithmetic.json').read_text())['values']
def term(v):
 f=Fraction(v); return f'({f.numerator} : ℚ) / {f.denominator}'
s='''import DarkTower.WarMachine.MachineModelSpec

namespace DarkTower.WarMachine.FloatCarriedRowCorrespondence
open DarkTower.WarMachine.MachineBeliefState
open DarkTower.WarMachine.MachineModelSpec

/-- Exact rational images of the seven retained binary64 values from binding-1.
This represents that one row; it does not verify the runtime reader. -/
def retainedMass : Status → ℚ
'''+''.join(f'  | .{k} => {term(v)}\n' for k,v in values.items())+'''
def retainedRow : FloatCarriedRow Status where
  support := Status.all
  mass := retainedMass
  nonnegative := by intro s; cases s <;> norm_num [retainedMass]
  support_nodup := by simp [Status.all]
  mass_eq_zero_of_not_mem := by intro s h; cases s <;> simp_all [Status.all]
  nearNormalised := by norm_num [Status.all, retainedMass, floatRowBound]

theorem retained_not_exact : (retainedRow.support.map retainedRow.mass).sum ≠ 1 := by
  norm_num [retainedRow, Status.all, retainedMass]

'''
for k,v in values.items(): s+=f'theorem retained_{k} : retainedRow.mass .{k} = {term(v)} := rfl\n'
s+='\nend DarkTower.WarMachine.FloatCarriedRowCorrespondence\n'
(D/'FloatCarriedRowCorrespondence.lean').write_text(s)
print('Prepared three permitted sources in isolated copy',W)

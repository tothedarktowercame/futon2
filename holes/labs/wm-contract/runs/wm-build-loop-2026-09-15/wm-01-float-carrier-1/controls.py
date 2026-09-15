import pathlib
R=pathlib.Path(__file__).resolve().parent
header='''import DarkTower.WarMachine.FloatCarriedRowCorrespondence
open DarkTower.WarMachine.MachineModelSpec
open DarkTower.WarMachine.FloatCarriedRowCorrespondence
namespace FloatCarrierControls
'''
pos=header+'''def sparseMass : Fin 3 → ℚ := fun o => if o = 0 then 1/3 else if o = 1 then 2/3 else 0
def sparse : FloatCarriedRow (Fin 3) where
  support := [0, 1]
  mass := sparseMass
  nonnegative := by intro o; fin_cases o <;> norm_num [sparseMass]
  support_nodup := by decide
  mass_eq_zero_of_not_mem := by intro o h; fin_cases o <;> norm_num [sparseMass] at *
  nearNormalised := by norm_num [sparseMass, floatRowBound]
theorem sparse_exact : (sparse.support.map sparse.mass).sum = 1 := by
  norm_num [sparse, sparseMass]
noncomputable def exactKernel := sparse.toProbabilityKernel sparse_exact
theorem unequal_coordinates : exactKernel.mass () 0 = 1/3 ∧ exactKernel.mass () 1 = 2/3 := by
  norm_num [exactKernel, FloatCarriedRow.toProbabilityKernel, sparse, sparseMass]
theorem outside_is_zero : (2 : Fin 3) ∉ sparse.support ∧ sparse.mass 2 = 0 := by
  decide
theorem exact_normalised : (exactKernel.support () |>.map (exactKernel.mass ())).sum = 1 :=
  sparse.toProbabilityKernel_normalised sparse_exact
example : retainedRow.support ≠ [] := retainedRow.support_ne_nil
#print axioms sparse
#print axioms sparse_exact
#print axioms exactKernel
#print axioms unequal_coordinates
#print axioms outside_is_zero
#print axioms exact_normalised
end FloatCarrierControls
'''
(R/'Positive.lean').write_text(pos)
for name,support,mass,bad in [('Duplicate','[false, false]','fun b => if b then 0 else 1/2','support_nodup'),('Hidden','[false]','fun _ => 1','mass_eq_zero_of_not_mem'),('OutsideBound','[false]','fun b => if b then 0 else 1 + 2/10^12','nearNormalised')]:
 s=header+f'def bad : FloatCarriedRow Bool where\n  support := {support}\n  mass := {mass}\n'
 fields={'nonnegative':'by intro b; cases b <;> norm_num',
 'support_nodup':'by decide',
 'mass_eq_zero_of_not_mem':'by intro b h; cases b <;> norm_num at *',
 'nearNormalised':'by norm_num [floatRowBound]'}
 for k,v in fields.items():
  if k==bad:
   tactic='intro b h; cases b <;> norm_num at *' if k=='mass_eq_zero_of_not_mem' else 'norm_num [floatRowBound]'
   v=f'by\n    refine ?{k}\n    all_goals {tactic}'
  s+=f'  {k} := {v}\n'
 (R/f'{name}.lean').write_text(s+'end FloatCarrierControls\n')
(R/'RetainedConversion.lean').write_text(header+'''noncomputable def rejected := retainedRow.toProbabilityKernel (by
  refine ?exact_total_one_premise
  all_goals norm_num [retainedRow, retainedMass, DarkTower.WarMachine.MachineBeliefState.Status.all])
end FloatCarrierControls
''')
axioms=['DarkTower.WarMachine.MachineModelSpec.'+x for x in ['FloatCarriedRow','FloatCarriedRow.support_ne_nil','FloatCarriedRow.toProbabilityKernel','FloatCarriedRow.toProbabilityKernel_coordinate','FloatCarriedRow.toProbabilityKernel_normalised','exact_row_admissible']]
axioms+=['DarkTower.WarMachine.FloatCarriedRowCorrespondence.'+x for x in ['retainedMass','retainedRow','retained_not_exact',*[f'retained_{s}' for s in ['spawned','refined','strengthened','addressed','falsified','foreclosed','reopened']]]]
(R/'Axioms.lean').write_text(header+''.join(f'#print axioms {a}\n' for a in axioms)+'end FloatCarrierControls\n')
print('Wrote positive, four intended negatives and axiom probe')

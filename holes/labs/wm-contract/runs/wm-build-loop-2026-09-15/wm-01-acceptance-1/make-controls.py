from pathlib import Path
out=Path(__file__).resolve().parent
prefix='import DarkTower.WarMachine.LocalPreferenceModule\nopen DarkTower.WarMachine.Holes\nopen DarkTower.WarMachine.LocalPreferenceModule\nnoncomputable section\n'
(out/'Valid.lean').write_text(prefix+'''
def sparseKernel : ProbabilityKernel Bool Bool where
  support s := [s]
  mass s o := if o = s then 1 else 0
  nonnegative := by intros; split <;> norm_num
  normalised := by intro s; simp
  support_nodup := by intro s; simp
  mass_eq_zero_of_not_mem := by intro s o h; simp_all

def exactThirds : ExactTable Bool where
  support := [false, true]
  mass b := if b then 2/3 else 1/3
  nonnegative := by intro b; cases b <;> norm_num
  normalised := by norm_num
  support_nodup := by simp
  mass_eq_zero_of_not_mem := by intro b h; cases b <;> simp_all

def sparseExact : ExactTable Bool where
  support := [false]
  mass b := if b then 0 else 1
  nonnegative := by intro b; cases b <;> norm_num
  normalised := by norm_num
  support_nodup := by simp
  mass_eq_zero_of_not_mem := by intro b h; cases b <;> simp_all

def zeroPrecision : PrecisionMap := fun _ => ⟨0, by norm_num⟩
def twoPrecision : PrecisionMap := fun _ => ⟨2, by norm_num⟩
theorem zeroPrecisionAccepted (e : Channel → ℝ) :
    (variationalFreeEnergy zeroPrecision e).value = 0 := by
  simp [variationalFreeEnergy, zeroPrecision, Channel.all]
theorem sparseOutsideZero : sparseKernel.mass false true = 0 := by
  norm_num [sparseKernel]
theorem exactCastOutsideZero : sparseExact.kernel.mass () true = 0 := by
  norm_num [ExactTable.kernel, sparseExact]
theorem thirdsPreserved : exactThirds.kernel.mass () false = (1/3 : ℝ) := by
  norm_num [ExactTable.kernel, exactThirds]
#print axioms sparseKernel
#print axioms exactThirds
#print axioms sparseExact
#print axioms zeroPrecisionAccepted
#print axioms twoPrecision
#print axioms sparseOutsideZero
#print axioms exactCastOutsideZero
#print axioms thirdsPreserved
''')
for kind in ['Kernel','ExactTable']:
 for obligation in ['support_nodup','mass_eq_zero_of_not_mem','nonnegative','normalised']:
  table=kind=='ExactTable'; arg='' if table else '_ '
  typ='Unit' if obligation in ['support_nodup','normalised'] else 'Bool'
  support='[(), ()]' if obligation=='support_nodup' else ('[()]' if typ=='Unit' else ('[false]' if obligation=='mass_eq_zero_of_not_mem' else '[false, true]'))
  mass='1/2' if typ=='Unit' else ('if b then 100 else 1' if obligation=='mass_eq_zero_of_not_mem' else 'if b then 2 else -1')
  fields={
   'nonnegative':('by intro b; cases b <;> norm_num' if table else 'by intro s b; cases b <;> norm_num') if typ=='Bool' else 'by intros; norm_num',
   'normalised':'by norm_num' if table else 'by intro s; norm_num',
   'support_nodup':'by simp' if table else 'by intro s; simp',
   'mass_eq_zero_of_not_mem':('by intro o h; cases o <;> simp_all' if table else 'by intro s o h; cases o <;> simp_all')}
  fields[obligation]='?'+obligation
  text=prefix+f'\ndef rejected{kind} : '+(f'ExactTable {typ}' if table else f'ProbabilityKernel Unit {typ}')+' := by\n  refine {\n'
  text+=f'    support := '+(support if table else f'fun _ => {support}')+'\n'
  text+='    mass := fun '+('b' if typ=='Bool' else '_')+' => '+mass+'\n' if table else '    mass := fun _ '+('b' if typ=='Bool' else '_')+' => '+mass+'\n'
  for name,proof in fields.items(): text+=f'    {name} := {proof}\n'
  text+='  }\n  all_goals '
  intros={'support_nodup':('' if table else 'intro s; '),'normalised':('' if table else 'intro s; '),'nonnegative':('intro o; cases o <;> ' if table else 'intro s o; cases o <;> '),'mass_eq_zero_of_not_mem':('intro o h; cases o <;> ' if table else 'intro s o h; cases o <;> ')}
  text+=intros[obligation]+('simp_all' if obligation=='support_nodup' else 'norm_num at *')+'\n'
  (out/f'Invalid{kind}-{obligation}.lean').write_text(text)
(out/'InvalidPrecision.lean').write_text(prefix+'''
def negativePrecision : PrecisionMap := by
  intro channel
  refine { value := -2, nonnegative := ?precision_nonnegative }
  all_goals norm_num
''')

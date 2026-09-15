import DarkTower.WarMachine.LocalPreferenceModule
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.LocalPreferenceModule
noncomputable section

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

(do (futon2.aif.wm.debugger/attach!)
 (doseq [p [:selection :construction :author-wait :build-resolution :reviewer-wait :grounding :close-evidence-manifest]]
   (futon2.aif.wm.debugger/arm-breakpoint! [:after p]))
 {:attached (futon2.aif.wm.debugger/attached?) :breakpoints (futon2.aif.wm.debugger/armed-breakpoints)})

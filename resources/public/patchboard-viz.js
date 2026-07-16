(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  if (root) root.PatchboardModel = api;
})(typeof window !== "undefined" ? window : null, function () {
  "use strict";

  const TT3 = ["000","001","010","100","011","101","110","111"];
  const ZERO_RULE = "00000000";
  const range = n => Array.from({length:n}, (_,i) => i);
  const identity = n => range(n);
  const clampShift = n => range(n).map(k => Math.max(k - 1, 0));

  function assertWiring(wiring) {
    const n = Array.isArray(wiring) ? wiring.length : 0;
    if (!n || wiring.some(j => !Number.isInteger(j) || j < 0 || j >= n)) {
      throw new Error("wiring must be a non-empty total map into its own jack set");
    }
    return wiring;
  }

  function choices(n) {
    let out = [[]];
    for (let i=0; i<n; i++) out = out.flatMap(prefix => [prefix.concat(0), prefix.concat(1)]);
    return out;
  }

  function analyse(wiring, pin=0) {
    assertWiring(wiring);
    const n=wiring.length, image=new Set(wiring);
    const free=range(n).filter(j => !image.has(j));
    const unsat=range(n).filter(k => wiring[k] === k);
    let values=Array(n).fill(null); free.forEach(j => values[j]=pin);
    for(let pass=0; pass<n; pass++) for(let k=0; k<n; k++) {
      const j=wiring[k];
      if(values[k]!==null && j!==k && values[j]===null) values[j]=1-values[k];
    }
    const chain=range(n).filter(j => values[j]!==null && !free.includes(j) && !unsat.includes(j));
    const attractors=choices(unsat.length).flatMap(bits => {
      const candidate=values.slice(); unsat.forEach((j,i)=>candidate[j]=bits[i]);
      if(candidate.some(x=>x===null)) return [];
      const bitstring=candidate.join("");
      return [{bits:bitstring,value:parseInt(bitstring,2)}];
    }).filter((x,i,a)=>a.findIndex(y=>y.bits===x.bits)===i).sort((a,b)=>a.value-b.value);
    return {size:n,wiring:wiring.slice(),free,chain,unsat,pin,attractors};
  }

  // Deterministic, small PRNG so a visible seed completely specifies a replay.
  function makeRng(seed) {
    let a=(Number(seed) || 0) >>> 0;
    return function () {
      a=(a+0x6D2B79F5)>>>0;
      let t=a; t=Math.imul(t^(t>>>15),t|1); t^=t+Math.imul(t^(t>>>7),t|61);
      return ((t^(t>>>14))>>>0)/4294967296;
    };
  }

  function ruleBit(rule, triple) {
    const index=TT3.indexOf(triple);
    if(index<0) throw new Error(`unknown legacy neighbourhood ${triple}`);
    return rule[index];
  }

  function mutateRule(rule, wiring, rng, writes) {
    let bits=rule.split("");
    for(let i=0; i<writes; i++) {
      const source=Math.floor(rng()*8), target=wiring[source];
      bits[target]=bits[source]==="0" ? "1" : "0";
    }
    return bits.join("");
  }

  function stepGenotype(row, wiring, rng, writes=2) {
    return row.map((self,x) => {
      const left=x ? row[x-1] : ZERO_RULE;
      const right=x<row.length-1 ? row[x+1] : ZERO_RULE;
      let output="";
      for(let bit=0; bit<8; bit++) {
        if(left[bit]===right[bit]) output+=left[bit];
        else output+=ruleBit(self,left[bit]+self[bit]+right[bit]);
      }
      return mutateRule(output,wiring,rng,writes);
    });
  }

  function stepPhenotype(genotype, phenotype) {
    return phenotype.map((self,x) => {
      const left=x ? phenotype[x-1] : "0";
      const right=x<phenotype.length-1 ? phenotype[x+1] : "0";
      return ruleBit(genotype[x],left+self+right);
    });
  }

  function createSimulation(width=80, seed=8) {
    const rng=makeRng(seed);
    const genotype=range(width).map(()=>range(8).map(()=>rng()<.5?"0":"1").join(""));
    const phenotype=range(width).map(()=>rng()<.5?"0":"1");
    return {rng,generation:0,genotype,phenotype,genHistory:[genotype],pheHistory:[phenotype]};
  }

  function advance(sim, wiring, writes=2, historyLimit=120) {
    const phenotype=stepPhenotype(sim.genotype,sim.phenotype);
    const genotype=stepGenotype(sim.genotype,wiring,sim.rng,writes);
    sim.generation++; sim.genotype=genotype; sim.phenotype=phenotype;
    sim.genHistory.push(genotype); sim.pheHistory.push(phenotype);
    if(sim.genHistory.length>historyLimit){sim.genHistory.shift();sim.pheHistory.shift();}
    return sim;
  }

  function boot() {
    if(typeof document==="undefined") return;
    const terminals=TT3.map((condition,jack)=>({condition,jack}));
    const state={wiring:clampShift(8),pin:0,pending:null,running:true,tickMs:60,
      gensPerTick:2,writes:2,width:80,seed:8,timer:null,sim:null};
    const byId=id=>document.getElementById(id);
    const fmtSet=xs=>`{${xs.join(", ")}}`;
    const kindFor=(shape,k)=>shape.unsat.includes(k)?"unsat":shape.chain.includes(k)?"chain":"free";

    function shapeHtml(shape){
      const attrs=shape.attractors.length<=16
        ? shape.attractors.map(x=>`${x.value} · ${x.bits}`).join(" &nbsp;|&nbsp; ")
        : `${shape.attractors.length} states`;
      return `<div class="shape"><div class="free-text"><b>FREE</b>${fmtSet(shape.free)}<span class="meaning">GIVENS / AXIOMS</span></div><div class="chain-text"><b>CHAIN</b>${fmtSet(shape.chain)}<span class="meaning">DERIVATION</span></div><div class="unsat-text"><b>UNSAT</b>${fmtSet(shape.unsat)}<span class="meaning">IRREDUCIBLE TENSION / HOWEVER</span></div><div class="attractors"><b>ATTR</b>${attrs||"∅ · unseeded closed cycle"}</div></div>`;
    }

    function boardSvg(shape){
      const row=40,left=154,right=430,height=8*row+15;
      const cables=range(8).map(k=>{const y1=18+k*row,y2=18+state.wiring[k]*row;return `<path class="cable ${kindFor(shape,k)}" d="M${left},${y1} C${left+90},${y1} ${right-90},${y2} ${right},${y2}"/>`;}).join("");
      const sources=terminals.map((t,k)=>{const y=18+k*row;return `<g class="jack source ${kindFor(shape,k)} ${state.pending===k?"selected":""}" data-side="source" data-jack="${k}" transform="translate(${left},${y})"><circle r="8"/><text x="-142">${k} · IF ${t.condition}</text><text class="sub" x="-142" y="12">THEN bit ${k} → ${state.wiring[k]}</text></g>`;}).join("");
      const targets=terminals.map((t,j)=>{const y=18+j*row,incoming=state.wiring.filter(x=>x===j).length;return `<g class="jack target ${shape.free.includes(j)?"free":""}" data-side="target" data-jack="${j}" transform="translate(${right},${y})"><circle r="8"/><text x="14">${j} · in ${incoming}</text></g>`;}).join("");
      return `<svg viewBox="0 0 570 ${height}" role="img" aria-label="MetaCA patch board">${cables}${sources}${targets}</svg>`;
    }

    function renderBoard(){
      const shape=analyse(state.wiring,state.pin);
      byId("shape").innerHTML=shapeHtml(shape);
      byId("patch-board").innerHTML=boardSvg(shape);
    }

    function drawHistory(canvas,history,genotype){
      const width=state.width,height=120; canvas.width=width;canvas.height=height;
      const ctx=canvas.getContext("2d"),image=ctx.createImageData(width,height);
      for(let y=0;y<height;y++) for(let x=0;x<width;x++){
        const present=y<history.length;
        const value=present?(genotype?parseInt(history[y][x],2):(history[y][x]==="1"?0:255)):7;
        const p=(y*width+x)*4; image.data[p]=value;image.data[p+1]=value;image.data[p+2]=value;image.data[p+3]=255;
      }
      ctx.putImageData(image,0,0);
    }

    function renderLive(){
      const center=state.sim.genotype[Math.floor(state.width/2)];
      byId("live-bits").innerHTML=`<div style="color:var(--muted);font-size:11px;margin-bottom:7px">LIVE TERMINALS · centre cell · rule ${parseInt(center,2)}</div><div class="bits">${center.split("").map((v,k)=>`<div class="bit ${v==="1"?"on":""}">${k}<br>${v}</div>`).join("")}</div>`;
      drawHistory(byId("genotype-canvas"),state.sim.genHistory,true);
      drawHistory(byId("phenotype-canvas"),state.sim.pheHistory,false);
      const counts={};state.sim.genotype.forEach(g=>counts[g]=(counts[g]||0)+1);
      const rules=Object.entries(counts).sort((a,b)=>parseInt(a[0],2)-parseInt(b[0],2)).map(([g,n])=>`${parseInt(g,2)}:${n}`).join("  ");
      byId("run-readout").textContent=`generation ${state.sim.generation} · terminal rule histogram ${rules}`;
      byId("tick-readout").textContent=`generation ${state.sim.generation}`;
    }

    function reset(){state.sim=createSimulation(state.width,state.seed);renderLive();}
    function tick(){if(!state.running)return;for(let i=0;i<state.gensPerTick;i++)advance(state.sim,state.wiring,state.writes);renderLive();}
    function reschedule(){clearInterval(state.timer);state.timer=setInterval(tick,state.tickMs);}

    document.addEventListener("click",e=>{
      const jack=e.target.closest(".jack");
      if(jack){const side=jack.dataset.side,j=Number(jack.dataset.jack);if(side==="source")state.pending=j;else if(state.pending!==null){state.wiring[state.pending]=j;state.pending=null;}renderBoard();return;}
      const preset=e.target.closest("button[data-preset]");if(!preset)return;
      state.wiring=preset.dataset.preset==="figure8"?clampShift(8):identity(8);state.pending=null;renderBoard();
    });
    byId("running").addEventListener("change",e=>state.running=e.target.checked);
    byId("tick-ms").addEventListener("change",e=>{state.tickMs=Math.max(40,Number(e.target.value)||60);e.target.value=state.tickMs;reschedule();});
    byId("gens-per-tick").addEventListener("change",e=>state.gensPerTick=Math.max(1,Math.min(20,Number(e.target.value)||1)));
    byId("mutation-writes").addEventListener("change",e=>state.writes=Math.max(0,Math.min(12,Number(e.target.value)||0)));
    byId("free-pin").addEventListener("change",e=>{state.pin=Number(e.target.value);renderBoard();});
    byId("width").addEventListener("change",e=>{state.width=Math.max(24,Math.min(180,Number(e.target.value)||80));e.target.value=state.width;reset();});
    byId("seed").addEventListener("change",e=>{state.seed=Math.max(0,Number(e.target.value)||0);e.target.value=state.seed;reset();});
    byId("reset").addEventListener("click",reset);
    renderBoard();reset();reschedule();
  }

  if(typeof document!=="undefined")document.addEventListener("DOMContentLoaded",boot);
  return {TT3,identity,clampShift,analyse,makeRng,mutateRule,stepGenotype,stepPhenotype,createSimulation,advance};
});

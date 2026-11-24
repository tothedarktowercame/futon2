import importlib.util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LOOP = ROOT / "scripts" / "wm_click_repair_loop.py"


def load_loop(name):
    spec = importlib.util.spec_from_file_location(name, LOOP)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_worktree_root_defaults_to_home(monkeypatch):
    monkeypatch.delenv("FUTON_WM_WORKTREE_ROOT", raising=False)
    module = load_loop("wm_click_worktree_default")
    assert module.WORKTREE_ROOT == Path.home() / "worktrees"
    assert Path.home() / "worktrees" / "futon2" / "self-heal" in module.WRITE_ROOTS


def test_worktree_root_honors_environment(monkeypatch, tmp_path):
    selected = tmp_path / "relocated-worktrees"
    monkeypatch.setenv("FUTON_WM_WORKTREE_ROOT", str(selected))
    module = load_loop("wm_click_worktree_env")
    assert module.WORKTREE_ROOT == selected
    assert selected / "futon2" / "self-heal" in module.WRITE_ROOTS
    assert Path.home() / "worktrees" / "futon2" / "self-heal" not in module.WRITE_ROOTS

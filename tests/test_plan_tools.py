import contextlib
import importlib.util
import io
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path
from unittest import mock


MODULE_PATH = Path(__file__).resolve().parents[1] / "scripts" / "plan_tools.py"
SPEC = importlib.util.spec_from_file_location("plan_tools", MODULE_PATH)
plan_tools = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = plan_tools
SPEC.loader.exec_module(plan_tools)


class PlanToolsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)

        self.root = Path(self.temp_dir.name)
        self.docs_dir = self.root / "docs"
        self.specs_dir = self.docs_dir / "specs"
        self.specs_dir.mkdir(parents=True)
        self.plan_path = self.docs_dir / "plan.md"

        self.module_patcher = mock.patch.multiple(
            plan_tools,
            ROOT=self.root,
            PLAN_PATH=self.plan_path,
            SPECS_DIR=self.specs_dir,
        )
        self.module_patcher.start()
        self.addCleanup(self.module_patcher.stop)

    def write_plan(self, text: str) -> None:
        self.plan_path.write_text(textwrap.dedent(text).strip() + "\n", encoding="utf-8")

    def write_spec(self, name: str, text: str) -> Path:
        path = self.specs_dir / name
        path.write_text(text, encoding="utf-8")
        return path

    def run_done(self, target: str) -> tuple[int, str]:
        buffer = io.StringIO()
        with contextlib.redirect_stdout(buffer):
            result = plan_tools.command_done(target)
        return result, buffer.getvalue()

    def test_next_lists_dependency_free_plans(self) -> None:
        self.write_plan(
            """
            # Pending implementation plan

            plan-001-alpha:
                First summary.

            plan-002-beta: plan-001-alpha
                Second summary.

            plan-003-gamma:
                Third summary.
            """
        )

        buffer = io.StringIO()
        with contextlib.redirect_stdout(buffer):
            result = plan_tools.command_next()

        self.assertEqual(result, 0)
        self.assertEqual(
            buffer.getvalue().strip().splitlines(),
            [
                "plan-001-alpha",
                "  First summary.",
                "plan-003-gamma",
                "  Third summary.",
            ],
        )

    def test_done_requires_changed_spec_with_keyword(self) -> None:
        self.write_plan(
            """
            # Pending implementation plan

            plan-001-alpha:
                First summary.
            """
        )
        self.write_spec("feature.md", "Alpha is implemented here.\n")

        with mock.patch.object(plan_tools, "changed_spec_paths", return_value=[]):
            with self.assertRaises(SystemExit) as error:
                self.run_done("alpha")

        self.assertIn("staged or unstaged", str(error.exception))
        self.assertIn("alpha", self.plan_path.read_text(encoding="utf-8").lower())

    def test_done_removes_plan_and_dependents_when_changed_spec_matches(self) -> None:
        self.write_plan(
            """
            # Pending implementation plan

            plan-001-alpha:
                First summary.

            plan-002-beta: plan-001-alpha
                Depends on alpha.
            """
        )
        spec_path = self.write_spec("feature.md", "Alpha is implemented here.\n")

        with mock.patch.object(plan_tools, "changed_spec_paths", return_value=[spec_path]):
            result, output = self.run_done("alpha")

        self.assertEqual(result, 0)
        self.assertIn("Removed plan-001-alpha", output)
        self.assertIn("feature.md", output)
        self.assertEqual(
            self.plan_path.read_text(encoding="utf-8"),
            "# Pending implementation plan\n\nplan-002-beta:\n    Depends on alpha.\n",
        )

    def test_changed_spec_paths_collects_staged_and_unstaged_specs(self) -> None:
        completed = subprocess.CompletedProcess(
            args=["git", "status", "--short", "--", "docs/specs"],
            returncode=0,
            stdout=" M docs/specs/one.md\nA  docs/specs/two.md\n?? docs/specs/three.md\n",
            stderr="",
        )

        with mock.patch.object(plan_tools.subprocess, "run", return_value=completed) as run_mock:
            changed = plan_tools.changed_spec_paths()

        run_mock.assert_called_once()
        self.assertEqual(
            changed,
            [
                plan_tools.SPECS_DIR / "one.md",
                plan_tools.SPECS_DIR / "two.md",
                plan_tools.SPECS_DIR / "three.md",
            ],
        )


if __name__ == "__main__":
    unittest.main()

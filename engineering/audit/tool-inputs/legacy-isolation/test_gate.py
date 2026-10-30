#!/usr/bin/env python3
import importlib.util, json, tempfile, unittest
from pathlib import Path

SPEC=importlib.util.spec_from_file_location("gate", Path(__file__).with_name("gate.py")); gate=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(gate)
SANITIZE_SPEC=importlib.util.spec_from_file_location("sanitize_scene_profile", Path(__file__).with_name("sanitize_scene_profile.py")); sanitize_scene_profile=importlib.util.module_from_spec(SANITIZE_SPEC); SANITIZE_SPEC.loader.exec_module(sanitize_scene_profile)
class GateTest(unittest.TestCase):
    def test_legacy_word_alone_is_not_a_rule(self):
        self.assertFalse(any(rx.search("class LegacyAccountAdapter {}") for rx in gate.RULES.values()))
    def test_dynamic_import_is_detected(self): self.assertTrue(gate.RULES["dynamic-loader"].search("import(resolveName())"))
    def test_literal_import_is_not_detected(self): self.assertFalse(gate.RULES["dynamic-loader"].search("import('./local.js')"))
    def test_old_database_direct_is_detected(self): self.assertTrue(gate.RULES["database-direct"].search("select * from qh_user"))
    def test_legacy_brand_and_sentiment_are_detected(self):
        self.assertTrue(gate.RULES["legacy-brand"].search("qh_server"))
        self.assertTrue(gate.RULES["legacy-brand"].search("情怀旧入口"))
        self.assertFalse(gate.RULES["legacy-brand"].search("sha512-QHJxC1"))
    def test_legacy_interface_is_detected(self):
        self.assertTrue(gate.RULES["legacy-interface"].search("'/api/v1/legacy/users'"))
        self.assertFalse(gate.RULES["legacy-interface"].search("'/api/v1/users'"))
    def test_retired_legacy_interface_is_isolated_but_live_one_is_not(self):
        self.assertEqual("isolated-compatibility", gate.rule_classification("legacy-interface", "Server/server/A.java", ['createContext("/legacy/x", e -> reply(e, 410, "retired"));'], 0))
        self.assertIsNone(gate.rule_classification("legacy-interface", "Server/server/A.java", ['createContext("/legacy/x", this::write);'], 0))
    def test_required_coverage_is_explicit(self):
        self.assertIn("native-prefab", gate.COVERAGE)
        self.assertIn("duplicate-entry", gate.COVERAGE)
        self.assertIn("legacy-dependency", gate.COVERAGE)
    def test_allowlist_cannot_reclassify_production(self):
        self.assertEqual("production-reachable", gate.classification(Path("Client/assets/a.ts")))
        self.assertEqual("readonly-evidence", gate.classification(Path("Server/docs/a.md")))
    def test_retired_scene_profile_uuids_are_permanent_gate_inputs(self):
        self.assertEqual(6, len(gate.RETIRED_SCENE_PROFILE_UUIDS))
        self.assertIn("3842e73c-4567-42eb-b5b6-7a1e499f42cd", gate.RETIRED_SCENE_PROFILE_UUIDS)
        self.assertEqual("Client/profiles/v2/packages/scene.json", gate.SCENE_PROFILE_PATH)
    def test_scene_profile_sanitizer_removes_definitions_and_references(self):
        retired = next(iter(gate.RETIRED_SCENE_PROFILE_UUIDS)); current = "11111111-2222-4333-8444-555555555555"
        with tempfile.TemporaryDirectory() as directory:
            profile = Path(directory) / "scene.json"
            profile.write_text(json.dumps({"camera-infos": {retired: {}, current: {}}, "camera-uuids": [retired, current]}))
            self.assertEqual(1, sanitize_scene_profile.sanitize(profile))
            value = json.loads(profile.read_text())
            self.assertEqual({current}, set(value["camera-infos"]))
            self.assertEqual([current], value["camera-uuids"])
if __name__ == "__main__": unittest.main()

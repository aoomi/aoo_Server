#!/usr/bin/env python3
import tempfile
import unittest
from pathlib import Path

import legacy_config_converter as converter
import mysql_integration_gate as mysql_gate


class LegacyConverterTest(unittest.TestCase):
    def test_catalog_canonicalizes_region_but_never_auto_publishes(self):
        rows, by_code, issues = converter.normalize_catalog({
            "7": {"id": 7, "gameName": "Test_Game", "gameType": "poker", "region": "sichaun", "isOpen": 1, "sortNum": 9}
        })
        self.assertEqual("test_game", rows[0]["gameCode"])
        self.assertEqual("CN-51", rows[0]["regionCode"])
        self.assertEqual("DRAFT", rows[0]["targetStatus"])
        self.assertTrue(rows[0]["sourceEnabled"])
        self.assertEqual([], issues)
        self.assertIs(rows[0], by_code["test_game"])

    def test_duplicate_ui_keys_are_losslessly_renamed_and_quarantined(self):
        catalog, by_code, _ = converter.normalize_catalog({
            "7": {"id": 7, "gameName": "test", "gameType": "poker", "region": "all", "isOpen": 0}
        })
        source = {
            "1": {"ID": 1, "GameName": "test", "Key": "mode", "ToggleType": 0, "ToggleCount": 2, "ToggleDesc": "A,B", "ShowIndexs": "1"},
            "2": {"ID": 2, "GameName": "test", "Key": "mode", "ToggleType": 0, "ToggleCount": 2, "ToggleDesc": "A,B", "ShowIndexs": "1"},
        }
        rows, issues = converter.normalize_ui(source, by_code)
        self.assertEqual(2, len(catalog) + 1)
        self.assertEqual({"mode__legacy_1", "mode__legacy_2"}, {row["optionKey"] for row in rows})
        self.assertTrue(all(row["mappingStatus"] == "REVIEW_REQUIRED" for row in rows))
        self.assertEqual(2, sum(1 for issue in issues if issue["type"] == "DUPLICATE_UI_OPTION_KEY"))

    def test_room_cost_expands_player_range_and_payer_modes(self):
        _, by_code, _ = converter.normalize_catalog({
            "7": {"id": 7, "gameName": "test", "gameType": "poker", "region": "all", "isOpen": 0}
        })
        records, issues = converter.normalize_room_cost({
            "x": {"id": "x", "GameType": "TEST", "PeopleMin": 2, "PeopleMax": 3, "SetCount": 8,
                  "AaCostCount": 1, "WinCostCount": 2, "CostCount": 3, "ClubAaCostCount": 4,
                  "ClubWinCostCount": 5, "ClubCostCount": 6, "UnionCostCount": 7, "Sign": 1}
        }, by_code)
        self.assertEqual(10, len(records))
        self.assertEqual({"AA", "WINNER", "OWNER", "CLUB", "UNION"}, {row["payerMode"] for row in records})
        self.assertEqual([], issues)

    def test_output_boundary_rejects_non_database_directory(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            with self.assertRaises(ValueError):
                converter.require_output_boundary(Path(temp_dir))


class MigrationOrderingTest(unittest.TestCase):
    def test_flyway_subversions_sort_after_base_version(self):
        base = Path("V20260822_04__base.sql")
        sub = Path("V20260822_04_1__sub.sql")
        self.assertLess(mysql_gate.migration_key(base), mysql_gate.migration_key(sub))

    def test_invalid_name_fails_closed(self):
        with self.assertRaises(ValueError):
            mysql_gate.migration_key(Path("migration.sql"))


if __name__ == "__main__":
    unittest.main()

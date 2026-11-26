import json
import unittest
from pathlib import Path

import generate_room_card_tables as generator


class RoomCardPricingBootstrapTest(unittest.TestCase):
    def test_exact_authoritative_play_set_and_safe_tables(self):
        docs = generator.load_documents()
        self.assertEqual(32, len(docs))
        self.assertEqual(set(generator.authority_catalog()), {d["playCode"] for d in docs})
        self.assertEqual(32, len({d["tableName"] for d in docs}))

    def test_every_play_has_confirmed_zero_default(self):
        for doc in generator.load_documents():
            policy = doc["policies"][0]
            self.assertEqual("PLAY_DEFAULT", policy["matchScope"])
            self.assertEqual(0, policy["amountMinor"])
            self.assertEqual("CONFIRMED", policy["confirmationStatus"])
            self.assertTrue(policy["freePolicyRef"])
            self.assertTrue(policy["refundPolicyRef"])

    def test_migration_contains_no_mutable_price_seed(self):
        ddl = generator.emit_sql()
        self.assertEqual(32, ddl.count("CREATE TABLE IF NOT EXISTS `aoo_room_card_price_"))
        self.assertNotIn("INSERT INTO", ddl)
        self.assertNotIn("region_code", ddl.lower())


if __name__ == "__main__":
    unittest.main()

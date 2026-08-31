import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[3]
EDGE = ROOT / "deploy/gateway/production-edge.yaml"


class ProductionEdgeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = EDGE.read_text(encoding="utf-8")

    def test_one_public_tls_host_and_no_loopback(self):
        hosts = re.findall(r"host: ([^\s]+)", self.text)
        self.assertEqual(hosts, ["api.production.invalid"])
        self.assertNotRegex(self.text, r"localhost|127\.0\.0\.1|::1")
        self.assertIn('force-ssl-redirect: "true"', self.text)

    def test_every_player_route_has_exactly_one_authoritative_backend(self):
        expected = {
            "/api/v2/gateway": "aoo-gateway", "/api/v2/account": "aoo-account",
            "/api/v1/location": "aoo-external", "/v1/activities": "aoo-activity",
            "/v1/luck-draw": "aoo-luckdraw", "/v1/rankings": "aoo-ranking",
            "/v1/achievements": "aoo-ranking", "/v1/player-profile": "aoo-player",
            "/v1/inventory": "aoo-inventory", "/v1/store": "aoo-inventory",
            "/v1/history": "aoo-records", "/v1/replays": "aoo-records",
        }
        for path, backend in expected.items():
            pattern = rf"path: {re.escape(path)}[^\n]+name: {backend}"
            self.assertEqual(len(re.findall(pattern, self.text)), 1, path)

    def test_client_bootstrap_uses_only_canonical_base(self):
        payload = re.search(r"    (\{\"apiBaseUrl\".*\})", self.text).group(1)
        config = json.loads(payload)
        self.assertEqual(set(config), {"apiBaseUrl", "requestTimeoutMs", "requestMaxRetries"})
        self.assertTrue(config["apiBaseUrl"].startswith("https://"))


if __name__ == "__main__":
    unittest.main()

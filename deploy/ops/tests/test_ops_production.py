#!/usr/bin/env python3
import os, pathlib, re, subprocess, unittest
ROOT = pathlib.Path(__file__).resolve().parents[3]
OPS = ROOT / "deploy/ops"
SERVICES = ("account", "gateway", "bootstrap", "hall", "njpdk", "cdxzmj", "admin")

class ProductionAssemblyTest(unittest.TestCase):
    def text(self, relative): return (OPS / relative).read_text(encoding="utf-8")
    def test_complete_topology_and_safety_controls(self):
        runtime = self.text("kubernetes/runtime.yaml")
        rendered = subprocess.run(["kubectl", "kustomize", str(OPS)], text=True, capture_output=True, check=True).stdout
        self.assertEqual(rendered.count("kind: Deployment"), 7)
        self.assertEqual(rendered.count("kind: StatefulSet"), 1)
        self.assertIn("image: mongo:8.0.29-noble", rendered)
        self.assertNotRegex(rendered, r"image:\s*mongo:(?:8\.0|8\.2|8\.3|latest)(?:\s|$)")
        for service in SERVICES:
            self.assertIn(f"name: aoo-{service}", runtime)
            self.assertIn(f"aoo/{service}:release", runtime)
        for control in ("startupProbe:", "readinessProbe:", "livenessProbe:", "resources:",
                        "RollingUpdate", "revisionHistoryLimit:", "progressDeadlineSeconds:",
                        "secretRef:", "runAsNonRoot: true", "readOnlyRootFilesystem: true"):
            self.assertGreaterEqual(rendered.count(control), 7, control)
        self.assertIn("namespace-default-deny", rendered)
        self.assertIn("kind: PodDisruptionBudget", rendered)
    def test_render_requires_real_digest_pins(self):
        digest = "0123456789abcdef" * 4
        env = dict(os.environ, AOO_PUBLIC_HOST="account.example.com")
        for service in SERVICES: env[f"AOO_{service.upper()}_IMAGE"] = f"registry.example/aoo/{service}@sha256:{digest}"
        result = subprocess.run([str(OPS / "scripts/render-production.sh")], env=env, text=True, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotRegex(result.stdout, r"aoo/(?:account|gateway|bootstrap|hall|njpdk|cdxzmj|admin):release")
        self.assertEqual(result.stdout.count("@sha256:"), 7)
        env.pop("AOO_GATEWAY_IMAGE")
        self.assertNotEqual(subprocess.run([str(OPS / "scripts/render-production.sh")], env=env).returncode, 0)
    def test_release_is_signed_and_verified(self):
        workflow = (ROOT / ".github/workflows/release.yml").read_text()
        for token in ("actions/download-artifact", "cosign sign-blob", "cosign verify-blob", "output-signature", "output-certificate"):
            self.assertIn(token, workflow)
        self.assertNotRegex(workflow, r"(?m)^\s*- run: echo .*sign")
        self.assertIn("id-token: write", workflow)

if __name__ == "__main__": unittest.main()

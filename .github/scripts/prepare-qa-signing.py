"""Restore the private QA signing bundle without putting key material in logs."""
import base64
import json
import hashlib
import os
from pathlib import Path
import subprocess


def main():
    output_path = Path(os.environ["GITHUB_OUTPUT"])
    bundle_text = os.environ.get("OC_QA_SIGNING_JSON", "").strip()
    if not bundle_text:
        with output_path.open("a") as output:
            output.write("ready=false\n")
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as summary:
            summary.write(
                "## Permanent signing needs one-time setup\n"
                "Add the private signing bundle as the repository Actions secret "
                "`OC_QA_SIGNING_JSON`, then rerun this job. "
                "No installable feature APK is published without that key.\n"
            )
        return

    try:
        bundle = json.loads(bundle_text)
        alias = bundle["key_alias"]
        store_password = bundle["store_password"]
        key_password = bundle["key_password"]
        values = (alias, store_password, key_password)
        if any(not isinstance(value, str) or not value or "\n" in value or "\r" in value for value in values):
            raise ValueError("Invalid signing metadata")
        key_bytes = base64.b64decode(bundle["keystore_base64"], validate=True)
        if not key_bytes:
            raise ValueError("Empty keystore")
    except (ValueError, KeyError, TypeError):
        raise SystemExit("OC_QA_SIGNING_JSON is not a valid signing bundle") from None

    for password in (store_password, key_password):
        print("::add-mask::" + password.replace("%", "%25"))

    keystore = Path(os.environ["RUNNER_TEMP"]) / "opencloud-personal-signing.p12"
    keystore.write_bytes(key_bytes)
    keystore.chmod(0o600)
    check_env = dict(os.environ, OC_QA_STORE_PASSWORD=store_password)
    result = subprocess.run(
        ["keytool", "-exportcert", "-keystore", str(keystore), "-storetype", "PKCS12",
         "-storepass:env", "OC_QA_STORE_PASSWORD", "-alias", alias],
        env=check_env, capture_output=True,
    )
    if result.returncode:
        keystore.unlink(missing_ok=True)
        raise SystemExit("The signing keystore could not be opened with the configured alias/password")

    expected = (Path(__file__).resolve().parent.parent / "qa-signing-certificate.sha256").read_text().strip()
    if hashlib.sha256(result.stdout).hexdigest() != expected:
        keystore.unlink(missing_ok=True)
        raise SystemExit("The signing certificate differs from this app's permanent key; no APK will be published")

    with Path(os.environ["GITHUB_ENV"]).open("a") as env_file:
        for name, value in {
            "OC_QA_KEYSTORE": str(keystore),
            "OC_QA_KEYSTORE_PASSWORD": store_password,
            "OC_QA_KEY_ALIAS": alias,
            "OC_QA_KEY_PASSWORD": key_password,
        }.items():
            env_file.write(f"{name}={value}\n")
    with output_path.open("a") as output:
        output.write("ready=true\n")


if __name__ == "__main__":
    main()

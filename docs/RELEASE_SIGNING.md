# Release signing

This document records the public details of the upload key used to sign
Forseti's Google Play uploads. Nothing in this file is secret — the
fingerprint and certificate subject are published by Play Console on the
app's listing page once uploaded. The keystore file itself and its
passwords are **never** committed; see "Where the secrets live" below.

## Upload key (Google Play upload signing)

| Field | Value |
|---|---|
| Alias | `forseti-upload` |
| Algorithm | RSA, 2048-bit |
| Signature | SHA256withRSA |
| Validity | 10,000 days from May 11, 2026 |
| Subject | `CN=Asvaettir Labs, OU=Forseti, O=Asvaettir Labs, L=Portsmouth, ST=RI, C=US` |
| SHA-256 fingerprint | `CA:16:3E:BE:90:B6:20:5A:BA:36:4D:98:1C:7A:DA:F9:73:2D:01:C3:EB:80:F6:F8:9C:84:9F:71:1F:F5:37:79` |

This is the **upload key**. Once the first build is uploaded to Play
Console, Google generates a separate **app signing key** that it uses to
re-sign the APKs delivered to users; Play will display that second
fingerprint separately under *Setup → App integrity*. Both fingerprints
should be recorded here once the app is published.

```
TODO after first Play upload:
App signing key SHA-256: <fill in from Play Console → App integrity>
App signing key SHA-1:   <fill in from Play Console → App integrity>
```

## Where the secrets live

Nothing sensitive is in this repo. The build expects either:

1. A `keystore.properties` file in the repo root (gitignored), structured
   like `keystore.properties.example`, or
2. The equivalent env vars `FORSETI_KEYSTORE_FILE`, `FORSETI_KEYSTORE_PASSWORD`,
   `FORSETI_KEY_ALIAS`, `FORSETI_KEY_PASSWORD` (preferred for CI).

If neither is present, `assembleRelease` falls back to debug signing so
local smoke tests still work; Play uploads will fail until real
credentials are supplied.

The actual `forseti-upload.jks` file lives outside the repo
(`~/forseti-upload.jks` on the build machine) and is backed up to:

- TODO: list your offline/offsite backup locations here, e.g. an encrypted
  USB drive and a password-manager attachment. Losing this file or its
  password means we can never publish updates for `com.forseti` again.

## Verifying a signed bundle

After `./gradlew :app:bundleRelease`, confirm the AAB was signed with the
upload key and not the debug key:

```bash
unzip -p app/build/outputs/bundle/release/app-release.aab META-INF/*.RSA \
  | keytool -printcert | grep -E 'Owner|SHA256'
```

The `SHA256` line must match the fingerprint at the top of this file.

You can also re-print the fingerprint directly from the keystore at any
time without touching the password file:

```bash
keytool -list -v -keystore ~/forseti-upload.jks -alias forseti-upload
```

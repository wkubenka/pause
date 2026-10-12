# Pause

A personal Android app that walks you through a smart-spending checklist before non-essential purchases, with scrutiny scaled to the price. Local-only: no accounts, no network.

## How it works

1. Type what you're thinking of buying (the time is recorded).
2. Enter the all-in price; a tier is chosen automatically.
3. Answer the tier's questions, one per screen.
4. Review a summary with warning signs highlighted. The app never tells you what to do.
5. Bigger purchases get an advisory cool-off measured from when you first considered them, with a reminder when it ends.
6. 90 days after buying, two quick look-back questions.

Tiers, questions and notifications are all editable in Settings.

## Building

Requires JDK 17 and the Android SDK (API 36).

    ./gradlew assembleDebug
    ./gradlew testDebugUnitTest

## Releasing (Astute Repo)

Pause is distributed through [Astute Repo](https://github.com/wkubenka/astute-apps). To release:

    git tag v1.2.3 && git push origin v1.2.3

CI runs tests, builds a release APK signed with the repo's keystore secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), sets versionName `1.2.3` / versionCode `10203`, and attaches the APK to a GitHub Release. Astute Repo picks it up automatically. Always sign with the same keystore, or installed copies can't update. Check locally with `./scripts/verify-release.sh`.

## Credits

Default questions are from *The Smart Spending Checklist* by Sunny Kind Journey, used for personal use.

## License

Private — personal use only.

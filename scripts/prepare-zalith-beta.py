#!/usr/bin/env python3
"""Prepare the pinned Zalith Launcher source as the unofficial BlockPojav Beta fork."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1]).resolve()
module = root / "ZalithLauncher"
props = module / "gradle.properties"
build_file = module / "build.gradle.kts"
main_screen = module / "src/main/java/com/movtery/zalithlauncher/ui/screens/main/MainScreen.kt"
manifest = module / "src/main/AndroidManifest.xml"

def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected one {label} anchor, found {count}")
    return text.replace(old, new, 1)

properties = props.read_text()
for key, value in {
    "launcher_name": "BlockPojav",
    "launcher_app_name": "BlockPojav Beta",
    "launcher_short_name": "BP Beta",
    "url_home": "https://github.com/Fosre/BL-BlockLauncher",
    "launcher_version_code": "200050",
    "launcher_version_name": "0.1.0-beta.1",
}.items():
    properties, count = re.subn(rf"(?m)^{re.escape(key)}=.*$", f"{key}={value}", properties)
    if count != 1:
        raise SystemExit(f"Expected one {key} property, found {count}")
properties = re.sub(r"(?m)^default_(?:store|key)_password=.*\n", "", properties)
props.write_text(properties)

build = build_file.read_text()
build = replace_once(build, 'applicationId = zalithPackageName', 'applicationId = "com.blockpojav.launcher.beta"', "application id")
build = replace_once(build, '        applicationIdSuffix = ".v2"\n', "", "application suffix")
build = replace_once(build, '            signingConfig = signingConfigs.getByName("debugBuild")\n', "", "debug signing key")
build = replace_once(build, '            signingConfig = signingConfigs.getByName("releaseBuild")\n', "", "release signing key")
build_file.write_text(build)

screen = main_screen.read_text()
anchor = '''                        Text(
                            text = BuildKeys.LAUNCHER_IDENTIFIER,
                            style = style,
                            softWrap = softWarp,
                            maxLines = maxLines
                        )'''
replacement = '''                        Text(
                            text = "BlockPojav БЕТА",
                            style = style,
                            softWrap = softWarp,
                            maxLines = maxLines
                        )'''
main_screen.write_text(replace_once(screen, anchor, replacement, "home beta notice"))
screen = main_screen.read_text()
top_bar_end = '''                changeExpandedState = {
                    changeTasksExpandedState()
                },
            )

            Box('''
with_banner = '''                changeExpandedState = {
                    changeTasksExpandedState()
                },
            )

            Text(
                text = "НЕОФИЦИАЛЬНАЯ МОДИФИКАЦИЯ",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                softWrap = false,
                maxLines = 1
            )

            Box('''
main_screen.write_text(replace_once(screen, top_bar_end, with_banner, "unofficial banner"))

manifest_text = manifest.read_text()
manifest_text = replace_once(manifest_text, 'android:icon="@mipmap/ic_launcher"', 'android:icon="@drawable/ic_blockpojav_beta"', "launcher icon")
manifest_text = replace_once(manifest_text, 'android:roundIcon="@mipmap/ic_launcher_round"', 'android:roundIcon="@drawable/ic_blockpojav_beta"', "round launcher icon")
manifest.write_text(manifest_text)

icon = module / "src/main/res/drawable/ic_blockpojav_beta.xml"
icon.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0A0C10" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#53BEFF" android:pathData="M22,20h16v68h-16zM38,20h30v16h-30zM68,36h16v18h-16zM38,48h30v16h-30zM68,64h16v18h-16zM38,72h30v16h-30z" />
    <path android:fillColor="#B8E8FF" android:pathData="M22,20h16v7h-16zM38,20h30v7h-30z" />
</vector>
''')

for keystore in module.glob("*.jks"):
    keystore.unlink()

print("Prepared BlockPojav Beta: app id, beta version and visible unofficial-build notice set.")

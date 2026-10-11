#!/usr/bin/env python3
from pathlib import Path
import json,re,hashlib,sys
root=Path(__file__).resolve().parents[1]
errors=[]
def need(path):
 p=root/path
 if not p.exists(): errors.append(f"MISSING {path}")
 return p
for f in ['settings.gradle.kts','app/build.gradle.kts','app/src/main/AndroidManifest.xml','authority/raven_design_authority.json','authority/visual_environment_authority.json','authority/release_gate_checklist.json']:
 need(f)
manifest=need('app/src/main/AndroidManifest.xml').read_text()
gradle=need('app/build.gradle.kts').read_text()
if 'com.ravencore.zero' not in gradle: errors.append('package identity missing')
for j in ['authority/raven_design_authority.json','authority/visual_environment_authority.json','authority/release_gate_checklist.json']:
 try: json.loads(need(j).read_text())
 except Exception as e: errors.append(f'{j}: {e}')
kt=list((root/'app/src/main/java/com/ravencore/zero').rglob('*.kt'))
if len(kt)<20: errors.append(f'insufficient production Kotlin files: {len(kt)}')
for forbidden in ['com.phoneassistant.local','Hallow Kingdom']:
 for p in kt:
  if forbidden.lower() in p.read_text(errors='ignore').lower(): errors.append(f'forbidden legacy content in {p.relative_to(root)}')
print(f'KOTLIN_FILES={len(kt)}')
print('AUTHORITY_STATIC_VERIFICATION=' + ('FAIL' if errors else 'PASS'))
for e in errors: print('ERROR='+e)
sys.exit(bool(errors))

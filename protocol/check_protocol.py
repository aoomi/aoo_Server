#!/usr/bin/env python3
import json, pathlib, re, sys
root=pathlib.Path(__file__).resolve().parent
source=json.loads((root/'aoo-protocol-v2.json').read_text(encoding='utf-8'))
baseline=json.loads((root/'aoo-protocol-v2.baseline.json').read_text(encoding='utf-8'))
errors=[]; ids=[]
required={'protocolVersion','msgId','kind','requestId','seq','timestamp','traceId','body'}
if not required.issubset(source.get('requiredRequestFields',[])): errors.append('request envelope fields missing')
for message in source.get('messages',[]):
 mid=message.get('msgId',''); ids.append(mid)
 if not re.fullmatch(r'[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+',mid): errors.append(f'invalid msgId: {mid}')
 for field in ('kind','direction','transport','version','stage','auth','write','idempotency','request','response','errors'):
  if field not in message: errors.append(f'{mid}: missing {field}')
if len(ids)!=len(set(ids)): errors.append('duplicate msgId')
reserved=set(source.get('reservedMessageIds',[]))
if reserved.intersection(ids): errors.append('reserved msgId reused')
current={m['msgId']:m for m in source['messages']}
for old in baseline.get('messages',[]):
 if old['msgId'] not in current and old['msgId'] not in reserved: errors.append(f'removed msgId not reserved: {old["msgId"]}')
 elif old['msgId'] in current:
  new=current[old['msgId']]
  for side in ('request','response'):
   old_req=set(old.get(side,{}).get('required',[])); new_req=set(new.get(side,{}).get('required',[]))
   if not new_req.issubset(old_req): errors.append(f'{old["msgId"]}: new required {side} field without major version')
   for name,typ in old.get(side,{}).get('properties',{}).items():
    if name not in new.get(side,{}).get('properties',{}): errors.append(f'{old["msgId"]}: removed {side}.{name}')
    elif new[side]['properties'][name]!=typ: errors.append(f'{old["msgId"]}: changed type {side}.{name}')
gold=(root/'golden-v2.json').read_bytes()
try: json.loads(gold.decode('utf-8'))
except Exception as exc: errors.append(f'invalid UTF-8 golden sample: {exc}')
if not gold.endswith(b'\n'): errors.append('golden sample must end with LF')
if errors:
 print('\n'.join(errors),file=sys.stderr);sys.exit(1)
print(f'protocol gate passed: {len(ids)} unique messages, compatibility and UTF-8 golden sample verified')

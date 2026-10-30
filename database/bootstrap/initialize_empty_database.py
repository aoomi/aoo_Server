#!/usr/bin/env python3
"""B0-only clean bootstrap. Never migrates an existing database or creates resources.

validate-inputs is offline. apply/verify require a preimport approval and an exact
isolated Docker resource receipt from the resource owner. DDL failures require
owner-controlled destruction/recreation of this task's new database, never retry.
Credentials are read only from the explicit mode-0600 JSON file, not repository env.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import subprocess
import sys
import threading
import uuid
import datetime
import zipfile

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
RUNTIME = 'server/tools/canonical-runtime-metadata-manifest.json'
RUNTIME_SHA = 'a453cb986a674f0f6044a581f8e69927a4cfbf94f531567ef021dfaee07a8282'


class Rejected(Exception):
    pass


def require(condition, reason):
    if not condition:
        raise Rejected(reason)


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def digest(data):
    return hashlib.sha256(data).hexdigest()


def hash_json(value):
    return digest(canonical(value).encode())


def local_file(path):
    p = (ROOT / path).resolve()
    require(p.is_relative_to(ROOT) and p.is_file(), 'missing or out-of-root input')
    return p


def validate_inputs(manifest):
    require(manifest['kind'] == 'B0_EMPTY_DATABASE_BOOTSTRAP', 'wrong bootstrap kind')
    require(digest((HERE / 'current-schema.sql').read_bytes()) == manifest['schemaSha256'], 'schema drift')
    for row in manifest['inputs']:
        require(digest(local_file(row['path']).read_bytes()) == row['sha256'], 'input drift: ' + row['path'])
    require(digest(local_file(RUNTIME).read_bytes()) == RUNTIME_SHA, 'formal issuance drift')
    require(manifest['deploymentRoute']['publicEndpoint']=='ws://127.0.0.1:18660/api/v2/gateway/ws','prospective route endpoint mismatch')
    require(manifest['deploymentRoute']['priority']==100 and manifest['deploymentRoute']['status']=='ACTIVE','route task settings mismatch')
    actual_schema=(HERE/'current-schema.sql').read_text()
    for match in re.finditer(r'CREATE TABLE `([^`]+)` \([\s\S]*?\n\) [^\n]*;',actual_schema):
        require(table_semantics(match[0])==manifest['schemaExpectations']['tables'][match[1]],'source DDL expectation mismatch')
    formal = json.loads(local_file(RUNTIME).read_text())
    rows = manifest['publications']
    require([p['metadata'] for p in rows] == formal['rows'], 'formal rows changed')
    ids = [p['metadata']['gameId'] for p in rows]
    require(len(ids) == len(set(ids)) == 32 and 630 not in ids and 2999 in ids, 'current identity set')
    require(sorted(ids) == manifest['expectedCurrentIds'], 'identity membership mismatch')
    require(len({p['metadata']['releaseId'] for p in rows}) == 32, 'duplicate release')
    require(len({p['metadata']['indexGeneration'] for p in rows}) == 32, 'duplicate generation')
    require(hash_json(rows) == manifest['publicPayloadSha256'], 'public payload drift')
    locators = {r['logicalPath']: r for r in manifest['sourceLocationMap']}
    require(len(locators) == len(manifest['sourceLocationMap']) == 34, 'locator cardinality')
    identities = {r['gameId']: r for r in json.loads(local_file('server/release-input/identity/room-rule-play-identities.json').read_text())['plays']}
    catalog = {r['gameId']: r for r in json.loads(local_file('server/release-input/identity/canonical-identity-registry.json').read_text())['rows']}
    for p in rows:
        r = p['metadata']; identity = p['identity']; gid = r['gameId']
        require(identity == identities[gid] and p['catalog'] == catalog[gid], 'identity source mismatch')
        records = []
        for binding in r['providerBinding']['implementationEvidence']:
            logical, expected = binding.split('#sha256=') if '#sha256=' in binding else binding.split('#sha256:')
            locator = locators.get(logical)
            require(locator is not None and locator['sha256'] == expected, 'unmapped source binding')
            require(digest(local_file(locator['physicalPath']).read_bytes()) == expected, 'source binding drift')
            records.append({'path': logical, 'sha256': expected})
        require(hash_json(records) == r['componentContentHash'], 'component hash mismatch')
        with zipfile.ZipFile(local_file(identity['workbookPath'])) as archive:
            members = [{'member': name, 'sha256': digest(archive.read(name))} for name in sorted(archive.namelist()) if not name.endswith('/') and name != 'docProps/core.xml']
        workbook_hash = digest(json.dumps(members, ensure_ascii=False, separators=(',', ':')).encode())
        require(workbook_hash == r['sourceArtifact']['normalizedWorkbookHash'], 'normalized workbook drift')
        require(hash_json(identity) == r['ruleContentHash'], 'rule identity hash mismatch')
        revision = hash_json({'components': records, 'workbook': {'path': identity['workbookPath'], 'normalizedHash': workbook_hash}})
        require(revision == r['sourceRevision'], 'source revision mismatch')
        binding = {'gameId': gid, 'gameCode': r['gameCode'], 'providerRoute': r['providerBinding']['route'], 'providerClass': r['providerBinding']['providerClass'], 'artifactVersion': r['artifactVersion'], 'ruleContentHash': r['ruleContentHash'], 'normalizedWorkbookHash': workbook_hash, 'componentContentHash': r['componentContentHash'], 'sourceRevision': revision, 'contractVersion': r['contractVersion']}
        require(hash_json(binding) == r['sourceHash'], 'formal source hash mismatch')
    return {'inputCount': len(manifest['inputs']), 'currentPublications': len(rows), 'schemaSha256': manifest['schemaSha256'], 'publicPayloadSha256': manifest['publicPayloadSha256']}


def ident(value):
    require(isinstance(value, str) and re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', value), 'unsafe SQL identifier')
    return '`' + value + '`'


def sql_value(value):
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return '1' if value else '0'
    if isinstance(value, int):
        return str(value)
    require(isinstance(value, str), 'unsupported SQL value')
    # Hex UTF-8 avoids SQL mode/backslash/quote dependence.
    return "CONVERT(X'" + value.encode().hex() + "' USING utf8mb4)"


def json_sql(value):
    return 'CAST(' + sql_value(canonical(value)) + ' AS JSON)'


def insert(table, values, expressions=None):
    expressions = expressions or {}
    keys = list(values) + list(expressions)
    require(len(keys) == len(set(keys)), 'duplicate insert column')
    return 'INSERT INTO ' + ident(table) + '(' + ','.join(map(ident, keys)) + ') VALUES (' + ','.join([sql_value(v) for v in values.values()] + list(expressions.values())) + ');'


def publication_sql(manifest):
    statements = [insert(r['table'], r['values']) for r in manifest['publicReferenceRows']]
    now = 'CURRENT_TIMESTAMP(3)'
    for p in manifest['publications']:
        m = p['metadata']; c = p['catalog']; gid = m['gameId']; version = m['playVersion']; region = m['regionCode']; source = m['sourceHash']
        snapshots = {k: json_sql(p[k]) for k in ('catalogSnapshot', 'ruleValidator', 'uiSchema', 'componentSnapshot', 'componentChain')}
        hashes = {k: 'SHA2(CAST(' + v + ' AS CHAR CHARACTER SET utf8mb4),256)' for k, v in snapshots.items()}
        bundle = 'JSON_OBJECT(' + ','.join(sql_value(k) + ',' + v for k, v in snapshots.items()) + ')'
        bundle_hash = 'SHA2(CAST(' + bundle + ' AS CHAR CHARACTER SET utf8mb4),256)'
        statements.append(insert('aoo_game_catalog', dict(game_id=gid, game_code=m['gameCode'], display_name=c['displayName'], category_code=c['category'], family_code=c['family'], provider_key=c['providerKey'], catalog_schema_version=1, status='ACTIVE')))
        statements.append(insert('aoo_game_region', dict(game_id=gid, region_code=region, availability='AVAILABLE')))
        statements.append(insert('aoo_play_version', dict(game_id=gid, play_version=version, default_region_code=region, rule_schema_version=1, ui_schema_version=1, component_schema_version=1, content_hash=source, status='ACTIVE', created_by=1), {'activated_at': now}))
        release = dict(release_id=m['releaseId'], game_id=gid, play_version=version, snapshot_schema_version=m['snapshotSchemaVersion'], card_codec_version=m['cardCodecVersion'], event_interpreter_version=m['eventInterpreterVersion'], protocol_version=m['protocolVersion'], source_hash=source, release_version=1, release_scope='REGIONAL', status='ACTIVE', rollout_percent=100, created_by=1, reason='Register existing signed current publication in empty B0 database', ui_hash=source)
        expressions = {'catalog_snapshot': snapshots['catalogSnapshot'], 'rule_snapshot': snapshots['ruleValidator'], 'ui_snapshot': snapshots['uiSchema'], 'component_snapshot': snapshots['componentSnapshot'], 'catalog_hash': hashes['catalogSnapshot'], 'rule_hash': hashes['ruleValidator'], 'component_hash': hashes['componentChain'], 'bundle_hash': bundle_hash, 'validated_at': now, 'activated_at': now}
        statements.append(insert('aoo_game_service_route', dict(route_id=gid,game_id=gid,play_version=version,endpoint=manifest['deploymentRoute']['publicEndpoint'],priority=100,status='ACTIVE')))
        statements.append(insert('aoo_game_release', release, expressions))
        statements.append(insert('aoo_game_release_region', dict(release_id=m['releaseId'], region_code=region, rollout_percent=100, status='ACTIVE')))
        statements.append(insert('aoo_compiled_room_create_index', dict(game_id=gid, region_code=region, play_version=version, index_generation=m['indexGeneration'], release_id=m['releaseId'], lookup_hash=digest(f'{gid}|{region}|{version}|{source}'.encode()), lifecycle_state='ACTIVE'), {'component_chain': snapshots['componentChain'], 'rule_validator': snapshots['ruleValidator'], 'ui_schema': snapshots['uiSchema'], 'bundle_hash': bundle_hash, 'validated_at': now, 'activated_at': now}))
        statements.append(insert('aoo_compiled_index_active', dict(game_id=gid, region_code=region, play_version=version, index_generation=m['indexGeneration'], release_id=m['releaseId'], cache_epoch=1, activated_by=1, activation_reason='Register existing signed current publication in empty B0 database')))
    return '\n'.join(statements)


# MySQL 8.4 semantic normalization for the frozen B0 schema grammar only.
TOKEN = re.compile(r"\s+|--[^\n]*|/\*.*?\*/|`(?:``|[^`])*`|'(?:''|\\.|[^'\\])*'|(?:\d+(?:\.\d+)?)|@?[A-Za-z_$][A-Za-z0-9_$]*|<=>|>=|<=|<>|!=|[(),.=+*/%<>;-]", re.S)


def tokens(sql):
    sql=re.sub(r'/\*!\d+\s+([\s\S]*?)\*/',lambda m:m[1],sql)
    result=[]; end=0
    for match in TOKEN.finditer(sql):
        require(match.start()==end,'unknown schema token')
        end=match.end(); word=match[0]
        if word.isspace() or word.startswith(('--','/*')):continue
        if word.startswith('`'):word=word[1:-1].replace('``','`').lower()
        elif not word.startswith("'"):word=word.lower()
        result.append(word)
    require(end==len(sql),'unparsed schema suffix')
    return result


def split_tokens(items):
    result=[]; item=[]; depth=0
    for token in items:
        if token==',' and depth==0:result.append(item);item=[];continue
        item.append(token)
        if token=='(':depth+=1
        if token==')':depth-=1
        require(depth>=0,'unbalanced schema')
    require(depth==0,'unbalanced schema')
    if item:result.append(item)
    return result


def parenthesized(items, start):
    require(items[start]=='(','expected parenthesis')
    depth=1
    for index in range(start+1,len(items)):
        if items[index]=='(':depth+=1
        if items[index]==')':depth-=1
        if depth==0:return items[start+1:index],index+1
    raise Rejected('unclosed schema expression')


class Expression:
    """Preserve precedence; remove only redundant grouping, not logical structure."""
    def __init__(self,items):self.items=items;self.pos=0
    def peek(self):return self.items[self.pos] if self.pos<len(self.items) else None
    def take(self):
        value=self.peek();require(value is not None,'incomplete expression');self.pos+=1;return value
    def expect(self,value):require(self.take()==value,'unexpected expression token')
    def parse(self,minimum=0):
        word=self.take()
        if word=='(':
            left=self.parse();self.expect(')')
        elif word=='case':
            self.expect('when');condition=self.parse();self.expect('then');yes=self.parse();self.expect('else');no=self.parse();self.expect('end');left=['case',condition,yes,no]
        elif word in ('not','-','+'):
            left=[word,self.parse(60)]
        elif word.startswith('_') and self.peek() and self.peek().startswith("'"):
            require(word=='_utf8mb4','unexpected expression character set');left=['literal',self.take()]
        elif word.startswith("'"):left=['literal',word]
        elif re.fullmatch(r'\d+(\.\d+)?',word):
            from decimal import Decimal
            left=['number',format(Decimal(word).normalize(),'f')]
        elif word in ('true','false'):left=['number','1' if word=='true' else '0']
        elif self.peek()=='(':
            self.take();args=[]
            if self.peek()!=')':
                while True:
                    args.append(self.parse())
                    if self.peek()!=',':break
                    self.take()
            self.expect(')');left=['call',word,args]
        else:left=['name',word]
        while True:
            op=self.peek();precedence={'or':10,'and':20,'=':30,'!=':30,'<>':30,'>':30,'<':30,'>=':30,'<=':30,'is':30,'in':30,'between':30,'regexp':30,'+':40,'-':40,'*':50,'/':50,'%':50}.get(op,-1)
            if precedence<minimum:break
            self.take()
            if op=='is':
                negate=self.peek()=='not'
                if negate:self.take()
                right=self.parse(31);left=['is not' if negate else 'is',left,right]
            elif op=='in':
                self.expect('(');values=[]
                while True:
                    values.append(self.parse())
                    if self.peek()!=',':break
                    self.take()
                self.expect(')');left=['in',left,values]
            elif op=='between':
                low=self.parse(31);self.expect('and');high=self.parse(31);left=['between',left,low,high]
            else:
                right=self.parse(precedence+1)
                if op=='regexp':left=['call','regexp_like',[left,right]]
                elif op in ('and','or'):
                    left=[op]+(left[1:] if left[0]==op else [left])+(right[1:] if right[0]==op else [right])
                else:left=['<>' if op=='!=' else op,left,right]
        return left


def expression(items):
    parser=Expression(items);result=parser.parse();require(parser.pos==len(items),'unsupported expression grammar');return result


def schema_options(items):
    result={'engine':'innodb','row_format':'dynamic','charset':'utf8mb4','collate':'utf8mb4_0900_ai_ci','comment':None,'auto_increment':'1'}
    index=0
    while index<len(items):
        key=items[index];index+=1
        if key==';':continue
        if key=='default':continue
        if key=='character':require(items[index]=='set','table charset');index+=1;key='charset'
        require(key in result,'unexpected table option: '+key)
        if items[index]=='=':index+=1
        result[key]=items[index];index+=1
    return result


def column_definition(items,options):
    name=items[0];kind=items[1];index=2;arguments=[]
    if index<len(items) and items[index]=='(':arguments,index=parenthesized(items,index)
    if kind in ('boolean','bool'):kind='tinyint';arguments=['1']
    if kind=='integer':kind='int'
    if kind in ('bigint','int','smallint','tinyint') and arguments!=['1']:arguments=[]
    col={'name':name,'type':kind,'typeArguments':arguments,'unsigned':False,'nullable':True,'default':None,'generated':None,'generatedStorage':None,'extra':[],'charset':options['charset'] if kind in ('char','varchar','text') else None,'collation':options['collate'] if kind in ('char','varchar','text') else None,'comment':None}
    while index<len(items):
        word=items[index];index+=1
        if word=='unsigned':col['unsigned']=True
        elif word=='null':col['nullable']=True
        elif word=='not':require(items[index]=='null','column NOT');index+=1;col['nullable']=False
        elif word=='default':
            if items[index]=='(':
                value,index=parenthesized(items,index);col['default']=expression(value)
            else:
                value=[items[index]];index+=1
                if index<len(items) and items[index]=='(':
                    args,index=parenthesized(items,index);value+=['(']+args+[')']
                col['default']=None if value==['null'] else expression(value)
        elif word=='on':
            require(items[index]=='update','column ON');index+=1;value=[items[index]];index+=1
            if index<len(items) and items[index]=='(':
                args,index=parenthesized(items,index);value+=['(']+args+[')']
            col['extra'].append(['on update',expression(value)])
        elif word=='auto_increment':col['extra'].append(['auto_increment'])
        elif word=='character':require(items[index]=='set','column charset');col['charset']=items[index+1];index+=2
        elif word=='collate':col['collation']=items[index];index+=1
        elif word=='comment':col['comment']=items[index];index+=1
        elif word=='generated':
            require(items[index:index+2]==['always','as'],'generated column');value,index=parenthesized(items,index+2);col['generated']=expression(value)
        elif word in ('stored','virtual'):col['generatedStorage']=word
        else:raise Rejected('unsupported column attribute: '+word)
    if kind in ('bigint','int','smallint','tinyint','decimal') and col['default'] and col['default'][0]=='literal':
        value=col['default'][1][1:-1]
        require(re.fullmatch(r'-?\d+(\.\d+)?',value),'numeric default literal')
        col['default']=expression(tokens(value))
    if kind=='json' or col['generated'] is not None:col['default']=None
    return col


def table_semantics(ddl):
    items=tokens(ddl);require(items[:2]==['create','table'],'not CREATE TABLE')
    name=items[2];body,end=parenthesized(items,3);options=schema_options(items[end:]);columns=[];indexes={};foreign={};checks={}
    for part in split_tokens(body):
        if part[0] not in ('constraint','primary','unique','key','index'):
            columns.append(column_definition(part,options));continue
        if part[0]=='constraint':
            cname=part[1]
            if part[2]=='check':
                body,last=parenthesized(part,3);require(part[last:] in ([],['enforced'],['not','enforced']),'check tail');checks[cname]={'expression':expression(body),'enforced':part[last:]!=['not','enforced']};continue
            require(part[2:4]==['foreign','key'],'unsupported constraint')
            local,next_index=parenthesized(part,4);require(part[next_index]=='references','foreign reference');target=part[next_index+1];remote,next_index=parenthesized(part,next_index+2)
            relation={'columns':[x[0] for x in split_tokens(local)],'target':target,'targetColumns':[x[0] for x in split_tokens(remote)],'onDelete':'restrict','onUpdate':'restrict'}
            while next_index<len(part):
                require(part[next_index]=='on','foreign action');action=part[next_index+1];next_index+=2;value=part[next_index];next_index+=1
                if value in ('no','set'):value+=' '+part[next_index];next_index+=1
                require(action in ('delete','update') and value in ('restrict','no action','cascade','set null'),'foreign action value')
                relation['on'+action.title()]='restrict' if value=='no action' else value
            foreign[cname]=relation;continue
        unique=part[0] in ('primary','unique');start=2 if part[0] in ('primary','unique') else 1
        iname='PRIMARY' if part[0]=='primary' else part[start];start=start if part[0]=='primary' else start+1
        entries,last=parenthesized(part,start);require(part[last:] in ([],['using','btree']),'index options')
        parsed=[]
        for entry in split_tokens(entries):
            column=entry[0];pos=1;prefix=None;direction='asc'
            if pos<len(entry) and entry[pos]=='(':length,pos=parenthesized(entry,pos);require(len(length)==1 and length[0].isdigit(),'index prefix');prefix=int(length[0])
            if pos<len(entry):direction=entry[pos];pos+=1
            require(pos==len(entry) and direction in ('asc','desc'),'index expression')
            parsed.append({'column':column,'prefix':prefix,'direction':direction})
        indexes[iname]={'unique':unique,'method':'btree','columns':parsed}
    for entry in indexes.get('PRIMARY',{}).get('columns',[]):
        next(c for c in columns if c['name']==entry['column'])['nullable']=False
    for cname,relation in foreign.items():
        wanted=relation['columns']
        if not any([e['column'] for e in i['columns'][:len(wanted)]]==wanted and all(e['prefix'] is None for e in i['columns'][:len(wanted)]) for i in indexes.values()):
            indexes[cname]={'unique':False,'method':'btree','columns':[{'column':c,'prefix':None,'direction':'asc'} for c in wanted]}
    return {'name':name,'columns':columns,'indexes':indexes,'foreignKeys':foreign,'checks':checks,'options':options}


def trigger_semantics(ddl):
    # The body is stored as submitted by MySQL; only quote/case/whitespace tokens normalize.
    start=re.search(r'\bTRIGGER\b',ddl,re.I);require(start is not None,'missing trigger definition')
    body=tokens(ddl[start.start():]);require(body[:1]==['trigger'],'trigger parse')
    if body[-1:]==[';']:body=body[:-1]
    return body


def full_schema_verify(connection,manifest):
    expected=manifest['schemaExpectations'];actual={};readbacks={}
    names=connection.execute('SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY table_name;')
    require(names==sorted(expected['tables']),'schema table/view inventory')
    names=connection.execute('SELECT trigger_name FROM information_schema.triggers WHERE trigger_schema=DATABASE() ORDER BY trigger_name;')
    require(names==sorted(expected['triggers']),'schema trigger inventory')
    for name,wanted in expected['tables'].items():
        rows=connection.execute('SHOW CREATE TABLE '+ident(name)+';');raw='\n'.join(rows).split('\t',1)
        require(len(raw)==2,'SHOW CREATE TABLE shape');readbacks[name]=raw[1];actual[name]=table_semantics(raw[1]);require(actual[name]==wanted,'full table definition mismatch: '+name)
    for name,wanted in expected['triggers'].items():
        row='\n'.join(connection.execute('SHOW CREATE TRIGGER '+ident(name)+';')).split('\t')
        require(len(row)==7,'SHOW CREATE TRIGGER shape')
        require(set(row[1].lower().split(','))==set(manifest['sqlMode'].lower().split(',')),'trigger SQL mode mismatch')
        require(row[3:6]==['utf8mb4','utf8mb4_0900_ai_ci','utf8mb4_0900_ai_ci'],'trigger character context mismatch')
        require(re.search(r'CREATE DEFINER=`root`@`localhost` TRIGGER',row[2],re.I) is not None,'trigger definer mismatch')
        readbacks[name]=row[2];require(trigger_semantics(row[2])==wanted,'full trigger definition mismatch: '+name)
    return {'showCreate':readbacks,'semanticSha256':hash_json(actual)}


def scoped_scans(connection,manifest):
    evidence=[]
    for scan in manifest['verificationQueries']:
        require(scan['sourceSha256']==digest(local_file(scan['sourcePath']).read_bytes()),'scan source drift')
        rows=connection.execute(scan['sql'])
        require(rows==scan['expectedRows'],'scoped SQL result mismatch: '+scan['name'])
        evidence.append({'name':scan['name'],'sqlSha256':digest(scan['sql'].encode()),'rows':len(rows)})
    return evidence



class MysqlFailure(Rejected):
    def __init__(self,diagnostic):
        super().__init__('MySQL first failure; discard this new database')
        self.diagnostic=diagnostic


def safe_mysql_error(raw,phase,ordinal,statement_sha,exit_code,timed_out=False):
    errors=[]
    for line in raw.splitlines():
        found=re.search(r'ERROR (\d+)(?: \(([0-9A-Z]+)\))?(?: at line (\d+))?: (.*)',line)
        if not found:continue
        # Never retain raw quoted values or connection/credential diagnostics.
        safe={1062:'duplicate key [VALUES REDACTED]',1064:'SQL syntax error [SQL TEXT REDACTED]',1146:'missing table [IDENTIFIER REDACTED]',1054:'unknown column [IDENTIFIER REDACTED]',1045:'authentication rejected [IDENTITY REDACTED]',1644:'guard rejected [DETAIL REDACTED]'}.get(int(found[1]),'MySQL error [DETAIL REDACTED]')
        errors.append({'code':int(found[1]),'sqlState':found[2],'line':int(found[3]) if found[3] else None,'message':safe})
    diagnostic={'phase':phase,'submittedBatchOrdinal':ordinal,'submittedBatchSha256':statement_sha,'exitCode':exit_code,'timedOut':timed_out,'stderrBytes':len(raw.encode()),'errors':errors[:1],'unusableNewDatabase':True}
    diagnostic['failureFingerprint']=hash_json({k:diagnostic[k] for k in ('phase','submittedBatchSha256','exitCode','timedOut','errors')})
    return diagnostic


class Mysql:
    """One connection. First errors are bounded, redacted and never overwritten."""
    def __init__(self, container_id, database, credentials):
        env = os.environ.copy();env['MYSQL_PWD'] = credentials['password']
        self.process = subprocess.Popen(['docker', 'exec', '-i', '-e', 'MYSQL_PWD', container_id, 'mysql', '--skip-reconnect', '--batch', '--raw', '--skip-column-names', '--unbuffered', '--default-character-set=utf8mb4', '-u', credentials['user'], database], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, env=env)
        self.lines = queue.Queue();self.stderr=[];self.stderr_size=0;self.stderr_done=threading.Event();self.first_failure=None;self.phase='guard';self.ordinal=0
        def read():
            for line in self.process.stdout:self.lines.put(line.rstrip('\n'))
            self.lines.put(None)
        def errors():
            while True:
                chunk=self.process.stderr.read(4096)
                if not chunk:break
                if self.stderr_size<65536:
                    part=chunk.encode('utf-8')[:65536-self.stderr_size].decode('utf-8',errors='ignore')
                    self.stderr.append(part);self.stderr_size+=len(part.encode('utf-8'))
            self.stderr_done.set()
        threading.Thread(target=read, daemon=True).start();threading.Thread(target=errors,daemon=True).start()

    def fail(self,sql,timed_out=False):
        if self.first_failure is None:
            if timed_out:self.process.kill()
            try:self.process.wait(timeout=3)
            except subprocess.TimeoutExpired:self.process.kill();self.process.wait()
            self.stderr_done.wait(timeout=3)
            self.first_failure=safe_mysql_error(''.join(self.stderr),self.phase,self.ordinal,digest(sql.encode()),self.process.returncode,timed_out)
            self.stderr.clear()
        raise MysqlFailure(self.first_failure)

    def execute(self, sql):
        if self.first_failure is not None:raise MysqlFailure(self.first_failure)
        self.ordinal+=1;marker = 'bootstrap_' + uuid.uuid4().hex
        try:
            self.process.stdin.write(sql + '\nSELECT ' + sql_value(marker) + ';\n');self.process.stdin.flush();result = []
            while True:
                line = self.lines.get(timeout=30)
                if line is None:self.fail(sql)
                if line == marker:return result
                result.append(line)
        except BrokenPipeError:self.fail(sql)
        except queue.Empty:self.fail(sql,True)

    def close(self):
        try:self.process.stdin.close()
        except BrokenPipeError:pass
        try:self.process.wait(timeout=5)
        except subprocess.TimeoutExpired:self.process.kill();self.process.wait()


def resource_guard(manifest, receipt, database, approval):
    r = manifest['resource']
    require(database in r['databases'], 'database not allocated to this task')
    require(receipt.get('owner') == r['owner'] and receipt.get('database') == database, 'resource ownership mismatch')
    require(receipt.get('liveTestResourceReady') is True, 'live resource not Ready')
    require(approval.get('decision') == 'ACCEPT_PREIMPORT', 'preimport not accepted')
    for filename in ('current-schema.sql', 'initialize_empty_database.py', 'manifest.json', 'test_empty_database_bootstrap.py'):
        require(approval.get('sourceSha256', {}).get(filename) == digest((HERE / filename).read_bytes()), 'preimport source binding mismatch')
    issued=datetime.datetime.fromisoformat(receipt.get('issuedAt',''))
    require(issued.tzinfo is not None,'resource receipt time zone required')
    age=(datetime.datetime.now(datetime.timezone.utc)-issued).total_seconds()
    require(0 <= age <= 900,'resource receipt expired or future')
    container_id = receipt.get('containerId', '')
    require(re.fullmatch(r'[0-9a-f]{64}', container_id), 'exact container ID required')
    def inspect(kind,value):
        return json.loads(subprocess.check_output(['docker',kind,'inspect',value],text=True))[0]
    data=inspect('container',container_id)
    require(data['Id'] == container_id and data['Name'] == '/' + r['container'], 'container mismatch')
    require(data['Created']==receipt.get('containerCreated'),'container creation mismatch')
    require(data['Image'] == r['image'], 'image mismatch')
    require(data['State']['Running'] is True, 'container not running')
    require((data['Config'].get('Labels') or {}).get('aoo.owner')==r['ownerLabel'],'container owner label mismatch')
    require(not data['HostConfig'].get('PortBindings'), 'host port binding forbidden')
    require(data['HostConfig'].get('NetworkMode') == r['network'], 'network mismatch')
    networks = data['NetworkSettings']['Networks']
    require(set(networks) == {r['network']} and networks[r['network']]['NetworkID'] == receipt.get('networkId'), 'network ID mismatch')
    aliases=set(networks[r['network']].get('Aliases') or [])
    require('mysql' in aliases and aliases <= {'mysql',r['container'],container_id,container_id[:12]},'unexpected database network alias')
    net=inspect('network',receipt['networkId'])
    require(net['Id']==receipt['networkId'] and net['Name']==r['network'] and net['Created']==receipt.get('networkCreated'),'network creation mismatch')
    require(net.get('Internal') is True,'network must be internal')
    require((net.get('Labels') or {}).get('aoo.owner')==r['ownerLabel'],'network owner label mismatch')
    members=net.get('Containers') or {}
    require(set(members)=={container_id},'unexpected database network member')
    require(members[container_id].get('Name')==r['container'] and members[container_id].get('EndpointID')==networks[r['network']].get('EndpointID') and bool(members[container_id].get('EndpointID')),'network endpoint identity mismatch')
    mounts = data['Mounts']
    require(len(mounts) == 1 and mounts[0]['Type'] == 'volume' and mounts[0]['Name'] == r['volume'] and mounts[0]['Destination'] == '/var/lib/mysql', 'mount mismatch')
    volume=inspect('volume',r['volume'])
    require(volume['Name']==r['volume'] and volume['CreatedAt']==receipt.get('volumeCreatedAt'),'volume creation mismatch')
    require((volume.get('Labels') or {}).get('aoo.owner')==r['ownerLabel'],'volume owner label mismatch')
    require(volume['Mountpoint']==mounts[0]['Source'] and digest(volume['Mountpoint'].encode())==receipt.get('volumeMountpointSha256'),'volume mount identity mismatch')
    require(receipt.get('routeEndpoint')==manifest['deploymentRoute']['publicEndpoint'],'prospective endpoint receipt mismatch')
    return container_id


def empty_guard(connection):
    rows = connection.execute("SELECT (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.events WHERE event_schema=DATABASE());")
    require(rows == ['0'], 'refusing nonempty schema before DDL')


def verify(connection, manifest):
    tables = sorted(o['name'] for o in manifest['objects'] if o['type'] == 'table')
    actual = connection.execute('SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY table_name;')
    require(actual == tables, 'table inventory mismatch')
    triggers = sorted(o['name'] for o in manifest['objects'] if o['type'] == 'trigger')
    require(connection.execute('SELECT trigger_name FROM information_schema.triggers WHERE trigger_schema=DATABASE() ORDER BY trigger_name;') == triggers, 'trigger inventory mismatch')
    require(connection.execute("SELECT (SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema=DATABASE())+(SELECT COUNT(*) FROM information_schema.events WHERE event_schema=DATABASE());") == ['0'], 'unexpected routine/event')
    for obj in manifest['objects']:
        if obj['type'] != 'table':
            continue
        table = obj['name']; where = ' WHERE table_schema=DATABASE() AND table_name=' + sql_value(table)
        columns = [x.split()[0].strip('`') for x in obj['columns']]
        require(connection.execute('SELECT column_name FROM information_schema.columns' + where + ' ORDER BY ordinal_position;') == columns, 'column inventory/order mismatch: ' + table)
        expected_indexes = []
        expected_constraints = []
        for definition in obj['constraints']:
            index = re.match(r'(PRIMARY KEY|(?:UNIQUE )?(?:KEY|INDEX)\s+`?(\w+)`?)\s*\(([^)]+)\)', definition, re.I)
            if index:
                name = 'PRIMARY' if index[1].upper() == 'PRIMARY KEY' else index[2]
                unique = '0' if index[1].upper().startswith(('PRIMARY','UNIQUE')) else '1'
                for ordinal, column in enumerate(index[3].split(','),1):
                    expected_indexes.append('\t'.join((name,str(ordinal),column.strip().strip('`'),unique)))
            constraint = re.match(r'CONSTRAINT\s+`?(\w+)`?', definition,re.I)
            if constraint:
                expected_constraints.append(constraint[1])
        # MySQL adds a supporting FK index only when no existing left prefix fits.
        groups = {}
        for item in expected_indexes:
            name, ordinal, column, unique = item.split('\t'); groups.setdefault(name, []).append(column)
        for definition in obj['constraints']:
            fk = re.search(r'CONSTRAINT\s+(\w+)\s+FOREIGN KEY\s*\(([^)]+)\)', definition,re.I)
            if fk:
                columns = [x.strip().strip('`') for x in fk[2].split(',')]
                if not any(existing[:len(columns)] == columns for existing in groups.values()):
                    groups[fk[1]] = columns
                    expected_indexes.extend('\t'.join((fk[1],str(n),col,'1')) for n,col in enumerate(columns,1))
        indexes = connection.execute('SELECT index_name,seq_in_index,column_name,non_unique FROM information_schema.statistics' + where + ' ORDER BY index_name,seq_in_index;')
        require(sorted(indexes) == sorted(expected_indexes), 'index definition mismatch: ' + table)
        constraints = connection.execute("SELECT constraint_name FROM information_schema.table_constraints" + where + " AND constraint_type IN ('FOREIGN KEY','CHECK') ORDER BY constraint_name;")
        require(constraints == sorted(expected_constraints), 'constraint inventory mismatch: ' + table)
    for p in manifest['publications']:
        m=p['metadata']; values=dict(route_id=m['gameId'],game_id=m['gameId'],play_version=m['playVersion'],endpoint=manifest['deploymentRoute']['publicEndpoint'],priority=100,status='ACTIVE')
        predicate=' AND '.join(ident(k)+' <=> '+sql_value(v) for k,v in values.items())
        require(connection.execute('SELECT COUNT(*) FROM aoo_game_service_route WHERE '+predicate+';')==['1'],'public service route mismatch')
    require(connection.execute('SELECT COUNT(*) FROM aoo_game_service_route;')==['32'],'route cardinality mismatch')
    catalog=manifest['catalogQuery']
    require(digest(local_file(catalog['sourcePath']).read_bytes())==catalog['sourceSha256'],'catalog query source drift')
    catalog_rows=connection.execute(catalog['sql'])
    pairs=[(int(row.split('\t')[catalog['gameIdColumn']]),row.split('\t')[catalog['playVersionColumn']]) for row in catalog_rows]
    require(sorted(pairs)==sorted((p['metadata']['gameId'],p['metadata']['playVersion']) for p in manifest['publications']),'actual Lobby.catalog not exact current32')
    for reference in manifest['publicReferenceRows']:
        values = reference['values']; predicate = ' AND '.join(ident(k) + ' <=> ' + sql_value(v) for k,v in values.items())
        require(connection.execute('SELECT COUNT(*) FROM ' + ident(reference['table']) + ' WHERE ' + predicate + ';') == ['1'], 'public reference field mismatch')
    for table in manifest['businessEmptyTables']:
        require(connection.execute('SELECT COUNT(*) FROM ' + ident(table) + ';') == ['0'], 'business table is not empty: ' + table)
    for p in manifest['publications']:
        m = p['metadata']
        fields = ['r.game_id','c.game_code','r.play_version','a.region_code','r.release_id','a.index_generation','r.snapshot_schema_version','r.card_codec_version','r.event_interpreter_version','r.protocol_version','r.source_hash','r.ui_hash','i.lifecycle_state','r.status']
        expected = [m[k] for k in ('gameId','gameCode','playVersion','regionCode','releaseId','indexGeneration','snapshotSchemaVersion','cardCodecVersion','eventInterpreterVersion','protocolVersion','sourceHash')] + [m['sourceHash'],'ACTIVE','ACTIVE']
        query = 'SELECT ' + ','.join(fields) + ' FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i USING(game_id,region_code,play_version,index_generation,release_id) JOIN aoo_game_release r USING(release_id,game_id,play_version) JOIN aoo_game_catalog c USING(game_id) WHERE a.game_id=' + str(m['gameId']) + ';'
        require(connection.execute(query) == ['\t'.join(map(str,expected))], 'active metadata mismatch')
        query = 'SELECT r.catalog_snapshot,r.rule_snapshot,r.ui_snapshot,r.component_snapshot,i.component_chain,i.rule_validator,i.ui_schema FROM aoo_game_release r JOIN aoo_compiled_room_create_index i USING(release_id,game_id,play_version) WHERE r.game_id=' + str(m['gameId']) + ';'
        result = connection.execute(query)
        require(len(result)==1 and [json.loads(x) for x in result[0].split('\t')] == [p[k] for k in ('catalogSnapshot','ruleValidator','uiSchema','componentSnapshot','componentChain','ruleValidator','uiSchema')], 'publication payload mismatch')
    require(connection.execute("SELECT COUNT(*) FROM aoo_game_release r JOIN aoo_compiled_room_create_index i USING(release_id,game_id,play_version) WHERE r.catalog_hash<>SHA2(CAST(r.catalog_snapshot AS CHAR CHARACTER SET utf8mb4),256) OR r.rule_hash<>SHA2(CAST(i.rule_validator AS CHAR CHARACTER SET utf8mb4),256) OR r.component_hash<>SHA2(CAST(i.component_chain AS CHAR CHARACTER SET utf8mb4),256) OR r.bundle_hash<>i.bundle_hash OR r.bundle_hash<>SHA2(CAST(JSON_OBJECT('catalogSnapshot',r.catalog_snapshot,'ruleValidator',i.rule_validator,'uiSchema',i.ui_schema,'componentSnapshot',r.component_snapshot,'componentChain',i.component_chain) AS CHAR CHARACTER SET utf8mb4),256) OR i.lookup_hash<>SHA2(CONCAT(i.game_id,'|',i.region_code,'|',i.play_version,'|',r.source_hash),256);") == ['0'], 'publication content hash mismatch')
    for table in ('aoo_game_catalog' ,'aoo_play_version','aoo_game_region','aoo_game_release','aoo_game_release_region','aoo_compiled_room_create_index','aoo_compiled_index_active'):
        require(connection.execute('SELECT COUNT(*) FROM ' + ident(table) + ';') == ['32'], 'current count mismatch')
    return {'tables': len(tables), 'triggers': len(triggers), 'current':32, 'businessEmptyTables':len(manifest['businessEmptyTables'])}


def apply(connection, manifest):
    connection.phase='nonempty-guard'
    require(connection.execute("SELECT GET_LOCK(CONCAT('b0-empty:',DATABASE()),0);") == ['1'], 'bootstrap lock unavailable')
    empty_guard(connection)
    require(connection.execute("SELECT default_character_set_name,default_collation_name,@@default_storage_engine FROM information_schema.schemata WHERE schema_name=DATABASE();") == ['utf8mb4\tutf8mb4_0900_ai_ci\tInnoDB'], 'database defaults mismatch')
    connection.phase='ddl'
    connection.execute((HERE / 'current-schema.sql').read_text())
    connection.phase='schema-readback'
    schema=full_schema_verify(connection,manifest)
    connection.phase='scoped-scans'
    connection.execute('START TRANSACTION;')
    try:scans=scoped_scans(connection,manifest)
    finally:connection.execute('ROLLBACK;')
    result=register_public(connection,manifest)
    return {**result,'schema':schema,'scans':scans}


def register_public(connection,manifest):
    connection.phase='public-import'
    connection.execute('START TRANSACTION;')
    try:
        connection.execute(publication_sql(manifest))
        connection.phase='public-readback'
        result = verify(connection, manifest)
        connection.execute('COMMIT;')
    except BaseException:
        try:connection.execute('ROLLBACK;')
        except Rejected:pass  # Exited connection rolls back; DDL requires recreation.
        raise
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('validate-inputs','apply','verify'))
    parser.add_argument('--database')
    parser.add_argument('--resource-receipt', type=Path)
    parser.add_argument('--preimport-approval', type=Path)
    parser.add_argument('--credentials-file', type=Path)
    args = parser.parse_args()
    manifest = json.loads((HERE / 'manifest.json').read_text())
    report = validate_inputs(manifest)
    if args.command != 'validate-inputs':
        require(all((args.database,args.resource_receipt,args.preimport_approval,args.credentials_file)), 'explicit resource, approval and credentials required')
        require(args.credentials_file.stat().st_mode & 0o077 == 0, 'credentials file must be private')
        container_id = resource_guard(manifest,json.loads(args.resource_receipt.read_text()),args.database,json.loads(args.preimport_approval.read_text()))
        credentials = json.loads(args.credentials_file.read_text())
        require(set(credentials)=={'user','password'} and credentials['user']=='root' and isinstance(credentials['password'],str), 'isolated DB credentials shape')
        connection = Mysql(container_id,args.database,credentials)
        try:
            report['database'] = apply(connection,manifest) if args.command=='apply' else {'schema':full_schema_verify(connection,manifest),'public':verify(connection,manifest)}
        finally:
            connection.close()
    print(json.dumps({'status':'DEV_SELF_CHECK','command':args.command,**report},ensure_ascii=False))


if __name__ == '__main__':
    try:
        main()
    except (Rejected, OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        # Never echo driver arguments, credentials or raw database messages.
        print(json.dumps({'status':'BOOTSTRAP_REJECTED','reason':str(error) if isinstance(error,Rejected) else type(error).__name__,'firstFailure':error.diagnostic if isinstance(error,MysqlFailure) else None}),file=sys.stderr)
        sys.exit(1)

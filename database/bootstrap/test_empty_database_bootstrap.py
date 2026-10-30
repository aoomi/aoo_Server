#!/usr/bin/env python3
"""Offline safety tests by default. --live requires four allocated fresh databases.

Live resources are created and destroyed only by the resource owner. This runner
never creates/drops a database/container/volume/network. It deliberately leaves
three failed test databases for ID-bound owner cleanup.
"""
import copy
import importlib.util
import json
from pathlib import Path
import re
import sys
import unittest
from unittest.mock import patch

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('bootstrap', HERE / 'initialize_empty_database.py')
b = importlib.util.module_from_spec(spec); spec.loader.exec_module(b)
MANIFEST = json.loads((HERE / 'manifest.json').read_text())


class RecordingConnection:
    def __init__(self, nonempty=False, fail_import=False):
        self.statements=[]; self.nonempty=nonempty; self.fail_import=fail_import
    def execute(self,sql):
        self.statements.append(sql)
        if 'GET_LOCK' in sql:return ['1']
        if 'information_schema.tables' in sql:return ['1' if self.nonempty else '0']
        if 'default_character_set_name' in sql:return ['utf8mb4\tutf8mb4_0900_ai_ci\tInnoDB']
        if self.fail_import and sql.startswith('INSERT'):raise b.Rejected('injected public DML error')
        return []


class OfflineTests(unittest.TestCase):
    def test_signed_inputs_and_current_set(self):
        self.assertEqual(b.validate_inputs(MANIFEST)['currentPublications'],32)
    def test_nonempty_schema_before_any_ddl(self):
        c=RecordingConnection(nonempty=True)
        with self.assertRaisesRegex(b.Rejected,'nonempty'):b.apply(c,MANIFEST)
        self.assertFalse(any('CREATE TABLE' in s or 'INSERT INTO' in s for s in c.statements))
    def test_import_failure_rolls_back_without_commit(self):
        c=RecordingConnection(fail_import=True)
        with patch.object(b,'full_schema_verify',return_value={}),patch.object(b,'scoped_scans',return_value=[]):
            with self.assertRaises(b.Rejected):b.apply(c,MANIFEST)
        self.assertEqual(c.statements[-1],'ROLLBACK;')
        self.assertNotIn('COMMIT;',c.statements)
    def test_readback_failure_rolls_back(self):
        c=RecordingConnection()
        with patch.object(b,'full_schema_verify',return_value={}),patch.object(b,'scoped_scans',return_value=[]),patch.object(b,'verify',side_effect=b.Rejected('readback mismatch')):
            with self.assertRaises(b.Rejected):b.apply(c,MANIFEST)
        self.assertEqual(c.statements[-1],'ROLLBACK;')
    def test_mutated_publication_and_630_fail(self):
        m=copy.deepcopy(MANIFEST);m['publications'][0]['metadata']['gameId']=630
        with self.assertRaises(b.Rejected):b.validate_inputs(m)
    def test_schema_hash_rejects_before_db(self):
        m=copy.deepcopy(MANIFEST);m['schemaSha256']='0'*64
        with self.assertRaisesRegex(b.Rejected,'schema drift'):b.validate_inputs(m)
    def test_payload_hash_covers_rule_fields(self):
        m=copy.deepcopy(MANIFEST);m['publications'][0]['uiSchema']['fields']=[]
        with self.assertRaisesRegex(b.Rejected,'payload drift'):b.validate_inputs(m)
    def test_sql_literals_do_not_interpolate(self):
        value="'); DROP DATABASE x; -- \\ \n秘密"
        result=b.sql_value(value)
        self.assertNotIn('DROP',result)
        self.assertEqual(bytes.fromhex(result.split("'")[1]).decode(),value)
        with self.assertRaises(b.Rejected):b.ident('x;DROP')
    def test_unallocated_database_rejected_without_docker(self):
        with patch.object(b.subprocess,'check_output') as docker:
            with self.assertRaises(b.Rejected):b.resource_guard(MANIFEST,{},'production',{})
            docker.assert_not_called()
    def test_missing_preimport_rejected_without_docker(self):
        resource=MANIFEST['resource'];db=resource['databases'][0]
        with patch.object(b.subprocess,'check_output') as docker:
            with self.assertRaisesRegex(b.Rejected,'preimport'):b.resource_guard(MANIFEST,{'owner':resource['owner'],'database':db,'liveTestResourceReady':True},db,{})
            docker.assert_not_called()
    def test_exact_scoped_schema_has_fk_column_closure(self):
        tables={o['name']:o for o in MANIFEST['objects'] if o['type']=='table'}
        self.assertEqual(len(tables),39)
        for table,obj in tables.items():
            columns={x.split()[0].strip('`') for x in obj['columns']}
            for constraint in obj['constraints']:
                fk=re.search(r'FOREIGN KEY\s*\(([^)]+)\)\s*REFERENCES\s+(\w+)\s*\(([^)]+)\)',constraint,re.I)
                if fk:
                    self.assertTrue({x.strip() for x in fk[1].split(',')}<=columns,table)
                    target={x.split()[0].strip('`') for x in tables[fk[2]]['columns']}
                    self.assertTrue({x.strip() for x in fk[3].split(',')}<=target,(table,fk[2]))
        self.assertIn('PRIMARY KEY (room_id)',tables['aoo_room_snapshot']['constraints'])
    def test_reference_order_satisfies_parent_keys(self):
        regions=set()
        for row in MANIFEST['publicReferenceRows']:
            if row['table']=='aoo_region':
                parent=row['values'].get('parent_region_code')
                self.assertTrue(parent is None or parent in regions)
                regions.add(row['values']['region_code'])
    def test_business_tables_have_no_seed(self):
        sql=b.publication_sql(MANIFEST)
        for table in MANIFEST['businessEmptyTables']:
            self.assertNotIn('INSERT INTO `'+table+'`',sql)
        self.assertNotIn('ON DUPLICATE KEY',sql)
        self.assertNotIn('INSERT IGNORE',sql)


    def test_whole_schema_expectations_come_from_frozen_source(self):
        for obj in MANIFEST['objects']:
            if obj['type']=='table':
                ddl='CREATE TABLE `'+obj['name']+'` ('+','.join(obj['columns']+obj['constraints'])+') '+obj['tableOptions']+';'
                self.assertEqual(b.table_semantics(ddl),MANIFEST['schemaExpectations']['tables'][obj['name']])
            else:self.assertEqual(b.trigger_semantics(obj['sourceEffects'][0]['sql']),MANIFEST['schemaExpectations']['triggers'][obj['name']])
    def test_schema_mutations_same_names_are_rejected(self):
        ddl="CREATE TABLE t (id BIGINT NOT NULL, flag BOOLEAN DEFAULT TRUE, name VARCHAR(8) NOT NULL DEFAULT 'x', PRIMARY KEY(id), CONSTRAINT ck CHECK (id>0), CONSTRAINT fk FOREIGN KEY(id) REFERENCES p(id) ON DELETE RESTRICT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
        expected=b.table_semantics(ddl)
        for old,new in [('BIGINT','INT'),('name VARCHAR(8) NOT NULL','name VARCHAR(8) NULL'),("DEFAULT 'x'","DEFAULT 'y'"),('DEFAULT CHARSET=utf8mb4','DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci'),('ON DELETE RESTRICT','ON DELETE CASCADE'),('id>0','id>=0'),('REFERENCES p(id)','REFERENCES p(other)'),('PRIMARY KEY(id)','UNIQUE KEY changed(id)')]:
            self.assertNotEqual(b.table_semantics(ddl.replace(old,new)),expected,(old,new))
    def test_mysql84_representation_normalization_preserves_semantics(self):
        source="CREATE TABLE t (id BIGINT PRIMARYPLACE, flag BOOLEAN DEFAULT TRUE, c VARCHAR(8), PRIMARY KEY(id), CONSTRAINT ck CHECK(id>0 AND (flag=1 OR c IS NULL))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;".replace(' PRIMARYPLACE','')
        actual="CREATE TABLE `t` (`id` bigint NOT NULL, `flag` tinyint(1) DEFAULT '1', `c` varchar(8) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL, PRIMARY KEY (`id`), CONSTRAINT `ck` CHECK (((`id` > 0) and ((`flag` = 1) or (`c` is null))))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;"
        self.assertEqual(b.table_semantics(source),b.table_semantics(actual))
        self.assertNotEqual(b.table_semantics(source),b.table_semantics(source.replace('CHECK(id>0 AND (flag=1 OR c IS NULL))','CHECK(id>0 AND (flag=1 OR c IS NULL)) /*!80016 NOT ENFORCED */')))
        self.assertNotEqual(b.expression(b.tokens('a=1 AND (b=2 OR c=3)')),b.expression(b.tokens('(a=1 AND b=2) OR c=3')))
        self.assertEqual(b.expression(b.tokens("c REGEXP '^a'")),b.expression(b.tokens("regexp_like(`c`,_utf8mb4'^a')")))
    def test_trigger_body_mutation_changes_expectation(self):
        source=next(o['sourceEffects'][0]['sql'] for o in MANIFEST['objects'] if o['type']=='trigger')
        self.assertNotEqual(b.trigger_semantics(source),b.trigger_semantics(source.replace('INSERT','DELETE',1)))
    def test_schema_failure_happens_before_public_dml(self):
        c=RecordingConnection()
        with patch.object(b,'full_schema_verify',side_effect=b.Rejected('changed definition')):
            with self.assertRaisesRegex(b.Rejected,'changed definition'):b.apply(c,MANIFEST)
        self.assertFalse(any(s.startswith('INSERT') for s in c.statements))
    def test_scan_failure_rolls_back_and_prevents_public_dml(self):
        c=RecordingConnection()
        with patch.object(b,'full_schema_verify',return_value={}),patch.object(b,'scoped_scans',side_effect=b.Rejected('scan mismatch')):
            with self.assertRaisesRegex(b.Rejected,'scan mismatch'):b.apply(c,MANIFEST)
        self.assertEqual(c.statements[-1],'ROLLBACK;')
        self.assertFalse(any(s.startswith('INSERT') for s in c.statements))
    def test_prospective_routes_are_exact32_current_pairs(self):
        sql=b.publication_sql(MANIFEST)
        self.assertEqual(sql.count('INSERT INTO `aoo_game_service_route`'),32)
        self.assertNotIn('aoo_game_service_route',MANIFEST['businessEmptyTables'])
        self.assertIn('aoo_game_service_route',MANIFEST['publicTables'])
        self.assertIn("sr.status='ACTIVE'",MANIFEST['catalogQuery']['sourceSql'])
        self.assertEqual(MANIFEST['deploymentRoute']['status'],'ACTIVE')
    def test_first_failure_diagnostics_redact_and_are_stable(self):
        raw="ERROR 1062 (23000) at line 7: Duplicate entry 'private-secret-token' for key 'sensitive-key'\n"
        a=b.safe_mysql_error(raw,'public-import',8,'a'*64,1)
        self.assertNotIn('private-secret-token',json.dumps(a));self.assertNotIn('sensitive-key',json.dumps(a))
        self.assertEqual(a['errors'][0]['code'],1062);self.assertEqual(a['exitCode'],1)
        self.assertEqual(a['failureFingerprint'],b.safe_mysql_error(raw,'public-import',99,'a'*64,1)['failureFingerprint'])
        timeout=b.safe_mysql_error('','ddl',1,'b'*64,-9,True)
        self.assertTrue(timeout['timedOut']);self.assertNotEqual(a['failureFingerprint'],timeout['failureFingerprint'])
    def test_resource_labels_internal_and_creation_are_checked(self):
        import datetime
        r=MANIFEST['resource'];cid='a'*64;nid='b'*64;created='2026-10-14T00:00:00Z';mount='/task-volume'
        receipt={'owner':r['owner'],'database':r['databases'][0],'liveTestResourceReady':True,'issuedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'containerId':cid,'containerCreated':created,'networkId':nid,'networkCreated':created,'volumeCreatedAt':created,'volumeMountpointSha256':b.digest(mount.encode()),'routeEndpoint':MANIFEST['deploymentRoute']['publicEndpoint']}
        approval={'decision':'ACCEPT_PREIMPORT','sourceSha256':{name:b.digest((HERE/name).read_bytes()) for name in ['current-schema.sql','initialize_empty_database.py','manifest.json','test_empty_database_bootstrap.py']}}
        container={'Id':cid,'Name':'/'+r['container'],'Created':created,'Image':r['image'],'State':{'Running':True},'Config':{'Labels':{'aoo.owner':r['ownerLabel']}},'HostConfig':{'PortBindings':{},'NetworkMode':r['network']},'NetworkSettings':{'Networks':{r['network']:{'NetworkID':nid,'Aliases':['mysql',r['container']]}}},'Mounts':[{'Type':'volume','Name':r['volume'],'Destination':'/var/lib/mysql','Source':mount}]}
        container['NetworkSettings']['Networks'][r['network']]['EndpointID']='c'*64
        network={'Id':nid,'Name':r['network'],'Created':created,'Internal':True,'Labels':{'aoo.owner':r['ownerLabel']},'Containers':{cid:{'Name':r['container'],'EndpointID':'c'*64}}}
        volume={'Name':r['volume'],'CreatedAt':created,'Labels':{'aoo.owner':r['ownerLabel']},'Mountpoint':mount}
        def run(c,n,v,rec):
            with patch.object(b.subprocess,'check_output',side_effect=[json.dumps([c]),json.dumps([n]),json.dumps([v])]):return b.resource_guard(MANIFEST,rec,r['databases'][0],approval)
        self.assertEqual(run(container,network,volume,receipt),cid)
        for kind in ('container','network','volume','internal','creation','mount','member','endpoint'):
            c,n,v,rec=map(copy.deepcopy,(container,network,volume,receipt))
            if kind=='container':c['Config']['Labels']['aoo.owner']='another-task'
            elif kind=='network':n['Labels']={}
            elif kind=='volume':v['Labels']['aoo.owner']='another-task'
            elif kind=='internal':n['Internal']=False
            elif kind=='creation':rec['containerCreated']='old'
            elif kind=='mount':c['Mounts'][0]['Source']='/wrong'
            elif kind=='member':n['Containers']['d'*64]={'Name':'unrelated','EndpointID':'e'*64}
            elif kind=='endpoint':n['Containers'][cid]['EndpointID']='f'*64
            with self.assertRaises(b.Rejected,msg=kind):run(c,n,v,rec)
        rec=copy.deepcopy(receipt);rec['issuedAt']='2000-01-01T00:00:00+00:00'
        with patch.object(b.subprocess,'check_output') as docker:
            with self.assertRaises(b.Rejected):b.resource_guard(MANIFEST,rec,r['databases'][0],approval)
            docker.assert_not_called()


    def test_mysql_driver_first_error_survives_rollback_and_keeps_exit(self):
        import io
        class Process:
            def __init__(self):
                self.stdin=io.StringIO();self.stdout=io.StringIO('');self.stderr=io.StringIO("ERROR 1064 (42000) at line 4: syntax near 'SECRET'\n"+'秘密'*100000);self.returncode=1
            def wait(self,timeout=None):return self.returncode
            def kill(self):self.returncode=-9
        process=Process()
        with patch.object(b.subprocess,'Popen',return_value=process) as popen:
            c=b.Mysql('a'*64,MANIFEST['resource']['databases'][0],{'user':'root','password':'SECRET'})
            c.phase='ddl'
            with self.assertRaises(b.MysqlFailure) as first:c.execute('INVALID DDL;')
            with self.assertRaises(b.MysqlFailure) as second:c.execute('ROLLBACK;')
            self.assertEqual(first.exception.diagnostic,second.exception.diagnostic)
            self.assertEqual(first.exception.diagnostic['exitCode'],1)
            self.assertEqual(first.exception.diagnostic['errors'][0]['code'],1064)
            self.assertNotIn('SECRET',json.dumps(first.exception.diagnostic))
            self.assertLessEqual(first.exception.diagnostic['stderrBytes'],65536)
            self.assertNotIn('SECRET',repr(popen.call_args.args));self.assertEqual(c.stderr,[])
            c.close()


def live(receipt_dir,approval_path,credentials_path):
    """Execute only after independent approval; receipt JSON per exact database."""
    b.validate_inputs(MANIFEST)
    approval=json.loads(approval_path.read_text())
    b.require(credentials_path.stat().st_mode & 0o077 == 0,'private credentials required')
    credentials=json.loads(credentials_path.read_text())
    for ordinal,database in enumerate(MANIFEST['resource']['databases']):
        receipt=json.loads((receipt_dir/(database+'.json')).read_text())
        container=b.resource_guard(MANIFEST,receipt,database,approval)
        c=b.Mysql(container,database,credentials)
        try:
            if ordinal==0:
                b.apply(c,MANIFEST)
                try:b.apply(c,MANIFEST)
                except b.Rejected as e:b.require('nonempty' in str(e),'wrong repeat failure')
                else:raise b.Rejected('repeat import accepted')
            elif ordinal==1:
                b.empty_guard(c);c.execute('CREATE TABLE bootstrap_nonempty_sentinel(id INT PRIMARY KEY);')
                try:b.apply(c,MANIFEST)
                except b.Rejected as e:b.require('nonempty' in str(e),'wrong nonempty failure')
                else:raise b.Rejected('nonempty accepted')
                b.require(c.execute('SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();')==['1'],'nonempty guard wrote tables')
            else:
                original=c.execute
                if ordinal==2:
                    def fail_ddl(sql):
                        if sql.startswith('-- B0 scoped'):
                            return original('CREATE TABLE bootstrap_partial_ddl(id INT); INVALID BOOTSTRAP DDL;')
                        return original(sql)
                    c.execute=fail_ddl
                    try:b.apply(c,MANIFEST)
                    except b.MysqlFailure as error:
                        b.require(error.diagnostic['phase']=='ddl' and error.diagnostic['errors'][0]['code']==1064,'unexpected DDL fault')
                    else:raise b.Rejected('DDL fault not rejected')
                else:
                    real=b.publication_sql
                    with patch.object(b,'publication_sql',lambda m:real(m)+'\nINSERT INTO nonexistent_bootstrap_fault VALUES(1);'):
                        try:b.apply(c,MANIFEST)
                        except b.MysqlFailure as error:
                            b.require(error.diagnostic['phase']=='public-import' and error.diagnostic['errors'][0]['code']==1146,'unexpected DML fault')
                        else:raise b.Rejected('DML fault not rejected')
        finally:c.close()
        if ordinal>=2:
            check=b.Mysql(container,database,credentials)
            try:
                if ordinal==3:
                    for table in MANIFEST['publicTables']:
                        b.require(check.execute('SELECT COUNT(*) FROM '+b.ident(table)+';')==['0'],'DML failure committed public data')
                    # Same public-registration transaction on this failed empty schema,
                    # not a bootstrap rerun: prove a real successful readback then rollback.
                    real_verify=b.verify
                    def fail_readback(connection,manifest):
                        real_verify(connection,manifest)
                        raise b.Rejected('injected readback mismatch')
                    with patch.object(b,'verify',fail_readback):
                        try:b.register_public(check,MANIFEST)
                        except b.Rejected as error:b.require(str(error)=='injected readback mismatch','unexpected readback failure')
                        else:raise b.Rejected('readback fault not rejected')
                    for table in MANIFEST['publicTables']:
                        b.require(check.execute('SELECT COUNT(*) FROM '+b.ident(table)+';')==['0'],'DML failure committed public data')
                    for table in MANIFEST['businessEmptyTables']:
                        b.require(check.execute('SELECT COUNT(*) FROM '+b.ident(table)+';')==['0'],'DML failure created business data')
                else:
                    b.require(check.execute('SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();')==['1'],'DDL failure boundary mismatch')
                try:b.apply(check,MANIFEST)
                except b.Rejected as error:b.require('nonempty' in str(error),'partial schema rerun wrong failure')
                else:raise b.Rejected('partial schema retry accepted')
            finally:check.close()
    print(json.dumps({'status':'DEV_SELF_CHECK','liveCases':4,'cleanupRequiredBy':'/root/yunwei'}))


if __name__=='__main__':
    if len(sys.argv)>1 and sys.argv[1]=='--live':
        b.require(len(sys.argv)==5,'--live RECEIPT_DIR PREIMPORT_APPROVAL CREDENTIALS_FILE')
        live(*(Path(x) for x in sys.argv[2:]))
    else:unittest.main()

#!/usr/bin/env python3
"""Apply every migration to disposable MySQL and exercise critical DB invariants."""

import argparse
import hashlib
import json
import re
import subprocess
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple


DATABASE_DIR = Path(__file__).resolve().parents[1]
MIGRATIONS_DIR = DATABASE_DIR / "migrations"
AUDIT_PATH = DATABASE_DIR / ".work/audit/mysql-integration-gate.json"
DICTIONARY_PATH = DATABASE_DIR / "target/data-dictionary.json"
PASSWORD = "aoo_test_only"


def run(command: Sequence[str], input_bytes: Optional[bytes] = None, check: bool = True) -> subprocess.CompletedProcess:
    result = subprocess.run(command, input=input_bytes, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if check and result.returncode != 0:
        raise RuntimeError("command failed ({}): {}".format(result.returncode, result.stderr.decode("utf-8", "replace")[-4000:]))
    return result


def migration_key(path: Path) -> Tuple[int, ...]:
    match = re.match(r"V([0-9_]+)__", path.name)
    if not match:
        raise ValueError("invalid migration name: {}".format(path.name))
    return tuple(int(part) for part in match.group(1).split("_"))


class MysqlContainer:
    def __init__(self, name: str, image: str):
        self.name = name
        self.image = image
        self.created = False

    def start(self) -> None:
        existing = run(["docker", "ps", "-a", "--filter", "name=^/{}$".format(self.name), "--format", "{{.Names}}"], check=True)
        if existing.stdout.strip():
            raise RuntimeError("refusing to reuse or remove existing container {}".format(self.name))
        run([
            "docker", "run", "-d", "--rm", "--name", self.name,
            "-e", "MYSQL_ROOT_PASSWORD={}".format(PASSWORD), "-e", "MYSQL_DATABASE=aoo",
            self.image, "--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci",
        ])
        self.created = True
        for _ in range(60):
            ping = run(["docker", "exec", self.name, "mysqladmin", "ping", "-uroot", "-p{}".format(PASSWORD), "--silent"], check=False)
            if ping.returncode == 0:
                probe = self.sql_result("SELECT 1;", check=False)
                if probe.returncode == 0 and probe.stdout.strip() == b"1":
                    return
            time.sleep(1)
        raise RuntimeError("MySQL did not become ready")

    def stop(self) -> None:
        if self.created:
            run(["docker", "stop", "--time", "10", self.name], check=False)
            self.created = False

    def sql_result(self, sql: str, database: bool = True, check: bool = True) -> subprocess.CompletedProcess:
        command = ["docker", "exec", "-i", self.name, "mysql", "-uroot", "-p{}".format(PASSWORD), "--batch", "--raw", "--skip-column-names"]
        if database:
            command.append("aoo")
        return run(command, sql.encode("utf-8"), check=check)

    def sql(self, sql: str, database: bool = True) -> str:
        return self.sql_result(sql, database=database).stdout.decode("utf-8", "replace").strip()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def parse_tsv(value: str, columns: Sequence[str]) -> List[Dict[str, Any]]:
    rows = []
    if not value:
        return rows
    for line in value.splitlines():
        fields = line.split("\t")
        fields.extend([""] * (len(columns) - len(fields)))
        rows.append(dict(zip(columns, fields)))
    return rows


def fixture_sql() -> str:
    component_types = [
        "PROVIDER", "LIFECYCLE", "RULE", "ACTION", "FLOW", "SCORING",
        "SETTLEMENT", "SNAPSHOT", "REPLAY", "UI",
    ]
    component_rows = []
    binding_rows = []
    release_rows = []
    for ordinal, component_type in enumerate(component_types, 1):
        component_id = 900000 + ordinal
        component_rows.append("({id},'dbgate.{kind}','v1',900000,'{kind}','com.aoo.bcg.gamespi.RuleComponent','classpath:dbgate.{kind}','{artifact}','{{}}',1,'{content}','ACTIVE')".format(
            id=component_id, kind=component_type.lower(), artifact=("{:x}".format(ordinal) * 64)[:64], content=("{:x}".format(ordinal + 1) * 64)[:64]))
        binding_rows.append("(900000,'dbgate-v1','{}',1,{},'{{}}',100)".format(component_type, component_id))
        release_rows.append("(900100,'{}',1,{},'dbgate.{}','v1','com.aoo.bcg.gamespi.RuleComponent','classpath:dbgate.{}','{}','{{}}','{}')".format(
            component_type, component_id, component_type.lower(), component_type.lower(),
            ("{:x}".format(ordinal) * 64)[:64], ("{:x}".format(ordinal + 1) * 64)[:64]))
    return """
INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status)
VALUES(900000,'dbgate','DB Gate','POKER','POKER_UNCLASSIFIED','dbgate.provider',1,'ACTIVE');
INSERT INTO aoo_game_region(game_id,region_code,availability,priority) VALUES(900000,'GLOBAL','AVAILABLE',1);
INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by)
VALUES(900000,'dbgate-v1','GLOBAL',1,1,1,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa','ACTIVE',CURRENT_TIMESTAMP(3),1);
INSERT INTO aoo_game_component(component_id,component_key,component_version,game_id,component_type,spi_type,implementation_locator,artifact_digest,parameter_schema,schema_version,content_hash,status)
VALUES {components};
INSERT INTO aoo_play_component_binding(game_id,play_version,component_type,ordinal,component_id,parameters,priority)
VALUES {bindings};
INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at)
VALUES(900100,900000,'dbgate-v1',1,'GLOBAL','{{}}','{{}}','{{}}','{{}}','bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb','cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc','dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd','eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee','ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff','VALIDATED',100,1,'integration gate',CURRENT_TIMESTAMP(3));
INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) VALUES(900100,'GLOBAL',100,'VALIDATED');
INSERT INTO aoo_game_release_component(release_id,component_type,ordinal,component_id,component_key,component_version,spi_type,implementation_locator,artifact_digest,parameter_snapshot,content_hash)
VALUES {release_components};
INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at)
VALUES(900000,'GLOBAL','dbgate-v1',1,900100,'[]','{{}}','{{}}','1111111111111111111111111111111111111111111111111111111111111111','ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff','READY',CURRENT_TIMESTAMP(3));
CALL aoo_activate_compiled_game_index(900200,'db-gate-activate-1',900000,'GLOBAL','dbgate-v1',1,1,'ACTIVATE','integration gate');
INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,component_chain,immutable_rules,bundle_hash,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version)
VALUES(900300,900000,'GLOBAL','dbgate-v1',1,900100,'[]','{{}}','ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff','cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc','eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee','cards-v1','events-v1','protocol-v1');
""".format(components=",".join(component_rows), bindings=",".join(binding_rows), release_components=",".join(release_rows))


def governance_fixture_sql() -> str:
    return """
INSERT INTO aoo_migration_run(migration_run_id,migration_code,source_system,authority_mode,schema_phase,source_snapshot_hash,source_count,target_count,source_business_hash,target_business_hash,source_balance,target_balance,orphan_count,mismatch_count,rollback_manifest,status)
VALUES(910001,'gate-pass','fixture','READ_ONLY_VERIFY','MIGRATE','2121212121212121212121212121212121212121212121212121212121212121',10,10,'2222222222222222222222222222222222222222222222222222222222222222','2222222222222222222222222222222222222222222222222222222222222222',100,100,0,0,'{}','VALIDATING'),
      (910002,'gate-fail','fixture','READ_ONLY_VERIFY','MIGRATE','2323232323232323232323232323232323232323232323232323232323232323',10,9,'2424242424242424242424242424242424242424242424242424242424242424','2525252525252525252525252525252525252525252525252525252525252525',100,99,1,1,'{}','VALIDATING');
INSERT INTO aoo_migration_validation(migration_run_id,validation_code,validation_type,expected_value,actual_value,passed)
VALUES(910001,'count','COUNT','10','10',1),(910002,'count','COUNT','10','9',0);
CALL aoo_finalize_migration_run(910001);
CALL aoo_finalize_migration_run(910002);

INSERT INTO aoo_data_repair_job(repair_job_id,repair_code,target_table,predicate_hash,operation_hash,idempotency_key,dry_run,maximum_rows,rows_per_second,chunk_size,before_image_location,rollback_manifest,requested_by,status)
VALUES(910100,'gate-repair','aoo_game_catalog','3131313131313131313131313131313131313131313131313131313131313131','3232323232323232323232323232323232323232323232323232323232323232','gate-repair-1',1,100,10,10,'fixture://before','{}',10,'AWAITING_APPROVAL');
INSERT INTO aoo_data_repair_approval(repair_job_id,approval_role,approver_id,scope_hash,decision,reason,decided_at)
VALUES(910100,'DATA_OWNER',11,'3333333333333333333333333333333333333333333333333333333333333333','APPROVED','gate',CURRENT_TIMESTAMP(3)),
      (910100,'DBA',12,'3333333333333333333333333333333333333333333333333333333333333333','APPROVED','gate',CURRENT_TIMESTAMP(3));
CALL aoo_approve_data_repair(910100);

INSERT INTO aoo_privacy_request(privacy_request_id,subject_type,subject_id_hash,request_type,identity_verification_ref,legal_basis,requested_at,due_at,status)
VALUES(910200,'PLAYER','3434343434343434343434343434343434343434343434343434343434343434','DELETE','fixture://identity','fixture',CURRENT_TIMESTAMP(3),TIMESTAMPADD(DAY,30,CURRENT_TIMESTAMP(3)),'RUNNING');
INSERT INTO aoo_legal_hold(legal_hold_id,scope_type,scope_key_hash,authority_reference,reason,placed_by,placed_at,status)
VALUES(910201,'PLAYER','3434343434343434343434343434343434343434343434343434343434343434','fixture://hold','gate',1,CURRENT_TIMESTAMP(3),'ACTIVE');
CALL aoo_complete_privacy_request(910200,JSON_OBJECT('phase','held'));
UPDATE aoo_legal_hold SET status='RELEASED',released_by=2,released_at=CURRENT_TIMESTAMP(3) WHERE legal_hold_id=910201;
UPDATE aoo_privacy_request SET status='RUNNING' WHERE privacy_request_id=910200;
CALL aoo_complete_privacy_request(910200,JSON_OBJECT('phase','completed'));

INSERT INTO aoo_schema_change_plan(change_id,migration_version,affected_table,schema_phase,backward_compatible,online_ddl_algorithm,lock_mode,estimated_rows,estimated_bytes,maximum_lock_ms,maximum_replica_lag_ms,preflight_sql,verification_sql,rollback_sql,status)
VALUES(910300,'gate','aoo_game_catalog','EXPAND',1,'INSTANT','NONE',1,1024,100,500,'SELECT 1','SELECT 1','ALTER TABLE noop','PLANNED');
INSERT INTO aoo_online_ddl_observation(observation_id,change_id,environment_code,mysql_version,table_rows_before,table_bytes_before,lock_wait_ms,replica_lag_peak_ms,rows_copied,started_at,completed_at,passed,evidence_uri)
VALUES(910301,910300,'fixture','8.4',1,1024,5,10,0,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),0,'fixture://ddl');
INSERT INTO aoo_query_plan_observation(observation_id,query_code,environment_code,mysql_version,dataset_hash,dataset_rows,explain_json,access_type,selected_index,examined_rows,result_rows,uses_filesort,uses_temporary_table,p50_ms,p95_ms,p99_ms,sample_count,passed,observed_at)
VALUES(910302,'ROOM_CREATE_INDEX_DIRECT','fixture','8.4','3535353535353535353535353535353535353535353535353535353535353535',1,'{}','const','PRIMARY',1,1,0,0,1,2,3,10,0,CURRENT_TIMESTAMP(3));
INSERT INTO aoo_connection_pool_observation(observation_id,service_code,environment_code,test_scenario,concurrency,iteration_count,connections_before,connections_after,abandoned_connections,open_transactions,stream_cancellations,exception_paths,duration_ms,passed,evidence_uri,observed_at)
VALUES(910303,'fixture','fixture','exceptions',10,100,10,10,0,0,10,10,1000,0,'fixture://pool',CURRENT_TIMESTAMP(3));

INSERT INTO aoo_backup_manifest(backup_id,environment_code,backup_type,started_at,completed_at,binlog_file,binlog_position,gtid_set,object_uri,encrypted,encryption_key_version,content_hash,size_bytes,expires_at,status)
VALUES(910400,'fixture','FULL',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),'binlog.1',1,'fixture-gtid','fixture://backup',1,'key-v1','3636363636363636363636363636363636363636363636363636363636363636',1024,TIMESTAMPADD(DAY,30,CURRENT_TIMESTAMP(3)),'VERIFIED');
INSERT INTO aoo_restore_drill(drill_id,backup_id,target_environment,restore_point,started_at,completed_at,rto_seconds,rpo_seconds,table_count_expected,table_count_actual,asset_hash_expected,asset_hash_actual,room_history_hash_expected,room_history_hash_actual,relationship_mismatch_count,passed,evidence_uri)
VALUES(910401,910400,'fixture-restore',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),1,0,99,99,'3737373737373737373737373737373737373737373737373737373737373737','3737373737373737373737373737373737373737373737373737373737373737','3838383838383838383838383838383838383838383838383838383838383838','3838383838383838383838383838383838383838383838383838383838383838',0,0,'fixture://restore');
INSERT INTO aoo_replica_failover_drill(drill_id,environment_code,old_primary_server_uuid,new_primary_server_uuid,expected_gtid_set,promoted_gtid_set,read_only_guard_verified,connection_reroute_verified,identity_generation_verified,writes_fenced_verified,data_loss_rows,unavailable_ms,started_at,completed_at,passed,evidence_uri)
VALUES(910402,'fixture','00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','fixture-gtid','fixture-gtid',1,1,1,1,0,10,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),0,'fixture://failover');

INSERT INTO aoo_archive_run(archive_run_id,source_table,archive_table,boundary_start,boundary_end,source_count,archive_count,source_hash,archive_hash,legal_hold_excluded_count,idempotency_key,status,started_at)
VALUES(910500,'aoo_room_event','fixture_archive','2025-01-01','2025-02-01',10,9,'3939393939393939393939393939393939393939393939393939393939393939','4040404040404040404040404040404040404040404040404040404040404040',0,'gate-archive','VERIFYING',CURRENT_TIMESTAMP(3));
"""


def build_dictionary(mysql: MysqlContainer) -> Dict[str, Any]:
    column_sql = """
SELECT c.table_name,c.ordinal_position,c.column_name,c.column_type,c.is_nullable,
       COALESCE(c.column_default,'<NULL>'),c.extra,c.collation_name,
       COALESCE(g.owner_code,'UNASSIGNED'),COALESCE(g.data_classification,'UNASSIGNED'),
       COALESCE(g.authoritative_source,'UNASSIGNED'),COALESCE(g.hot_retention_days,0),
       COALESCE(g.archive_retention_days,0),COALESCE(g.purge_after_days,0),
       COALESCE(g.legal_hold_supported,0),COALESCE(g.anonymization_strategy,'')
  FROM information_schema.columns c
  LEFT JOIN aoo_table_governance g ON g.table_name=c.table_name
 WHERE c.table_schema=DATABASE()
 ORDER BY c.table_name,c.ordinal_position;
"""
    index_sql = """
SELECT table_name,index_name,non_unique,seq_in_index,column_name,collation,
       COALESCE(cardinality,0),index_type
  FROM information_schema.statistics
 WHERE table_schema=DATABASE()
 ORDER BY table_name,index_name,seq_in_index;
"""
    constraint_sql = """
SELECT tc.table_name,tc.constraint_name,tc.constraint_type,
       COALESCE(GROUP_CONCAT(kcu.column_name ORDER BY kcu.ordinal_position SEPARATOR ','),''),
       COALESCE(MAX(kcu.referenced_table_name),''),
       COALESCE(GROUP_CONCAT(kcu.referenced_column_name ORDER BY kcu.ordinal_position SEPARATOR ','),'')
  FROM information_schema.table_constraints tc
  LEFT JOIN information_schema.key_column_usage kcu
    ON kcu.constraint_schema=tc.constraint_schema
   AND kcu.table_name=tc.table_name
   AND kcu.constraint_name=tc.constraint_name
 WHERE tc.constraint_schema=DATABASE()
 GROUP BY tc.table_name,tc.constraint_name,tc.constraint_type
 ORDER BY tc.table_name,tc.constraint_type,tc.constraint_name;
"""
    columns = parse_tsv(mysql.sql(column_sql), [
        "table", "ordinal", "column", "columnType", "nullable", "default", "extra", "collation",
        "owner", "classification", "authority", "hotDays", "archiveDays", "purgeDays",
        "legalHold", "anonymization",
    ])
    indexes = parse_tsv(mysql.sql(index_sql), ["table", "index", "nonUnique", "ordinal", "column", "collation", "cardinality", "indexType"])
    constraints = parse_tsv(mysql.sql(constraint_sql), ["table", "constraint", "type", "columns", "referencedTable", "referencedColumns"])
    return {
        "schemaVersion": 1,
        "engine": mysql.sql("SELECT VERSION();"),
        "generatedFrom": "live information_schema after all database/migrations",
        "tables": sorted({row["table"] for row in columns}),
        "columns": columns,
        "indexes": indexes,
        "constraints": constraints,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", default="mysql:8.4.5")
    parser.add_argument("--container", default="aoo-database-gate-20260824")
    args = parser.parse_args()
    mysql = MysqlContainer(args.container, args.image)
    result: Dict[str, Any] = {"schemaVersion": 1, "image": args.image, "checks": {}, "migrations": []}
    started = time.monotonic()
    try:
        mysql.start()
        migration_paths = sorted(MIGRATIONS_DIR.glob("V*.sql"), key=migration_key)
        versions = [migration_key(path) for path in migration_paths]
        if len(versions) != len(set(versions)):
            raise RuntimeError("duplicate migration version")
        for migration in migration_paths:
            migration_started = time.monotonic()
            applied = mysql.sql_result(migration.read_bytes().decode("utf-8"))
            result["migrations"].append({
                "file": migration.name,
                "sha256": sha256_file(migration),
                "elapsedMs": round((time.monotonic() - migration_started) * 1000, 3),
                "warnings": applied.stderr.decode("utf-8", "replace").count("Warning"),
            })

        expected_tables = {
            "aoo_game_catalog", "aoo_region", "aoo_game_region", "aoo_play_version",
            "aoo_room_rule_definition", "aoo_create_ui_option", "aoo_game_component",
            "aoo_component_dependency", "aoo_component_conflict", "aoo_play_component_binding",
            "aoo_game_release", "aoo_compiled_room_create_index", "aoo_compiled_index_active",
            "aoo_room_rule_lock", "aoo_migration_run", "aoo_data_repair_job",
            "aoo_privacy_request", "aoo_query_baseline", "aoo_backup_manifest",
        }
        table_names = set(mysql.sql("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';").splitlines())
        result["checks"]["expectedTables"] = {"passed": expected_tables <= table_names, "missing": sorted(expected_tables - table_names), "actualCount": len(table_names)}
        result["checks"]["foreignKeys"] = {"passed": int(mysql.sql("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='FOREIGN KEY';")) >= 35, "count": int(mysql.sql("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='FOREIGN KEY';"))}
        result["checks"]["checkConstraints"] = {"passed": int(mysql.sql("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='CHECK';")) >= 100, "count": int(mysql.sql("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='CHECK';"))}
        result["checks"]["governanceCoverage"] = {"passed": mysql.sql("SELECT COUNT(*) FROM v_aoo_data_governance_gap;") == "0", "gapCount": int(mysql.sql("SELECT COUNT(*) FROM v_aoo_data_governance_gap;"))}
        unsafe_numeric = int(mysql.sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND data_type IN ('double','float','real');"))
        result["checks"]["exactNumericTypes"] = {"passed": unsafe_numeric == 0, "unsafeColumnCount": unsafe_numeric}
        query_hash_mismatches = int(mysql.sql("SELECT COUNT(*) FROM aoo_query_baseline WHERE SHA2(sql_template,256)<>sql_hash;"))
        concurrency_column_gaps = int(mysql.sql("""
SELECT COUNT(*)
FROM aoo_concurrency_contract contract
LEFT JOIN information_schema.columns version_column
  ON version_column.table_schema=DATABASE()
 AND version_column.table_name=contract.table_name
 AND version_column.column_name=contract.version_column
LEFT JOIN information_schema.columns time_column
  ON time_column.table_schema=DATABASE()
 AND time_column.table_name=contract.table_name
 AND time_column.column_name=contract.update_time_column
WHERE (contract.version_column IS NOT NULL AND version_column.column_name IS NULL)
   OR (contract.update_time_column IS NOT NULL AND time_column.column_name IS NULL);
"""))
        result["checks"]["contractMetadata"] = {"passed": query_hash_mismatches == 0 and concurrency_column_gaps == 0, "queryHashMismatches": query_hash_mismatches, "concurrencyColumnGaps": concurrency_column_gaps}

        mysql.sql(fixture_sql())
        active_release = mysql.sql("SELECT release_id FROM aoo_compiled_index_active WHERE game_id=900000 AND region_code='GLOBAL';")
        result["checks"]["atomicActivation"] = {
            "passed": active_release == "900100" and mysql.sql("SELECT COUNT(*) FROM aoo_configuration_cache_epoch WHERE release_id=900100;") == "1" and mysql.sql("SELECT COUNT(*) FROM aoo_outbox WHERE event_id='db-gate-activate-1';") == "1" and mysql.sql("SELECT COUNT(*) FROM aoo_game_release_audit WHERE audit_id=900200;") == "1",
            "activeRelease": active_release,
        }

        failing_release_sql = """
INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at)
VALUES(900101,900000,'dbgate-v1',2,'GLOBAL','{}','{}','{}','{}','1212121212121212121212121212121212121212121212121212121212121212','1313131313131313131313131313131313131313131313131313131313131313','1414141414141414141414141414141414141414141414141414141414141414','1515151515151515151515151515151515151515151515151515151515151515','1616161616161616161616161616161616161616161616161616161616161616','VALIDATED',100,1,'negative gate',CURRENT_TIMESTAMP(3));
INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) VALUES(900101,'GLOBAL',100,'VALIDATED');
INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at)
VALUES(900000,'GLOBAL','dbgate-v1',2,900101,'[]','{}','{}','1717171717171717171717171717171717171717171717171717171717171717','1616161616161616161616161616161616161616161616161616161616161616','READY',CURRENT_TIMESTAMP(3));
"""
        mysql.sql(failing_release_sql)
        rejected = mysql.sql_result("CALL aoo_activate_compiled_game_index(900201,'db-gate-activate-2',900000,'GLOBAL','dbgate-v1',2,1,'ACTIVATE','must fail');", check=False)
        still_active = mysql.sql("SELECT release_id FROM aoo_compiled_index_active WHERE game_id=900000 AND region_code='GLOBAL';")
        result["checks"]["halfPublishIsolation"] = {"passed": rejected.returncode != 0 and still_active == "900100" and mysql.sql("SELECT COUNT(*) FROM aoo_outbox WHERE event_id='db-gate-activate-2';") == "0", "activeReleaseAfterFailure": still_active, "databaseError": rejected.stderr.decode("utf-8", "replace")[-500:]}

        mismatched_room_release = mysql.sql_result("""
INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,component_chain,immutable_rules,bundle_hash,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version)
VALUES(900301,900000,'GLOBAL','dbgate-v1',1,900101,'[]','{}','1616161616161616161616161616161616161616161616161616161616161616','1313131313131313131313131313131313131313131313131313131313131313','1515151515151515151515151515151515151515151515151515151515151515','cards-v1','events-v1','protocol-v1');
""", check=False)
        missing_published_release = mysql.sql_result("INSERT INTO aoo_published_game_configuration(game_id,play_version,release_id,configuration_payload,created_by,reason) VALUES(900000,'dbgate-v1',NULL,'{}',1,'must fail');", check=False)
        mysql.sql("INSERT INTO aoo_published_game_configuration(game_id,play_version,release_id,configuration_payload,created_by,reason) VALUES(900000,'dbgate-v1',900100,'{}',1,'gate publish');")
        result["checks"]["releaseIdentityConsistency"] = {
            "passed": mismatched_room_release.returncode != 0 and missing_published_release.returncode != 0
                and mysql.sql("SELECT release_id FROM aoo_published_game_configuration WHERE game_id=900000 AND play_version='dbgate-v1';") == "900100",
            "mismatchedRoomReleaseRejected": mismatched_room_release.returncode != 0,
            "missingPublishedReleaseRejected": missing_published_release.returncode != 0,
        }
        mysql.sql("UPDATE aoo_compiled_room_create_index SET lifecycle_state='FAILED' WHERE release_id=900101; UPDATE aoo_game_release_region SET status='RETIRED' WHERE release_id=900101; UPDATE aoo_game_release SET status='FAILED' WHERE release_id=900101;")

        immutable_update = mysql.sql_result("UPDATE aoo_room_rule_lock SET protocol_version='changed' WHERE room_id=900300;", check=False)
        release_delete = mysql.sql_result("DELETE FROM aoo_game_release WHERE release_id=900100;", check=False)
        duplicate_code = mysql.sql_result("INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status) VALUES(900999,'DBGATE','duplicate','POKER','POKER_UNCLASSIFIED','duplicate.provider',1,'DRAFT');", check=False)
        missing_reference = mysql.sql_result("INSERT INTO aoo_room_template(club_id,template_code,template_version,game_id,play_version,display_name,rule_payload,status,created_by,created_at,updated_at,row_version) VALUES(1,'bad',1,900000,'missing','bad','{}','ACTIVE',1,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),1);", check=False)
        result["checks"]["immutableAndReferenceGuards"] = {
            "passed": all(item.returncode != 0 for item in (immutable_update, release_delete, duplicate_code, missing_reference)),
            "roomLockUpdateRejected": immutable_update.returncode != 0,
            "releaseDeleteRejected": release_delete.returncode != 0,
            "caseVariantCodeRejected": duplicate_code.returncode != 0,
            "missingPlayVersionRejected": missing_reference.returncode != 0,
        }

        mysql.sql(governance_fixture_sql())
        archive_rejected = mysql.sql_result("UPDATE aoo_archive_run SET status='SUCCEEDED',completed_at=CURRENT_TIMESTAMP(3) WHERE archive_run_id=910500;", check=False)
        mysql.sql("UPDATE aoo_archive_run SET archive_count=source_count,archive_hash=source_hash WHERE archive_run_id=910500; UPDATE aoo_archive_run SET status='SUCCEEDED',completed_at=CURRENT_TIMESTAMP(3) WHERE archive_run_id=910500;")
        governance_values = {
            "migrationPass": mysql.sql("SELECT status FROM aoo_migration_run WHERE migration_run_id=910001;"),
            "migrationFail": mysql.sql("SELECT status FROM aoo_migration_run WHERE migration_run_id=910002;"),
            "repair": mysql.sql("SELECT status FROM aoo_data_repair_job WHERE repair_job_id=910100;"),
            "privacy": mysql.sql("SELECT status FROM aoo_privacy_request WHERE privacy_request_id=910200;"),
            "onlineDdlScored": mysql.sql("SELECT passed FROM aoo_online_ddl_observation WHERE observation_id=910301;"),
            "queryPlanScored": mysql.sql("SELECT passed FROM aoo_query_plan_observation WHERE observation_id=910302;"),
            "poolScored": mysql.sql("SELECT passed FROM aoo_connection_pool_observation WHERE observation_id=910303;"),
            "restoreScored": mysql.sql("SELECT passed FROM aoo_restore_drill WHERE drill_id=910401;"),
            "failoverScored": mysql.sql("SELECT passed FROM aoo_replica_failover_drill WHERE drill_id=910402;"),
            "archive": mysql.sql("SELECT status FROM aoo_archive_run WHERE archive_run_id=910500;"),
        }
        result["checks"]["governanceWorkflows"] = {
            "passed": governance_values == {
                "migrationPass": "PASSED", "migrationFail": "FAILED", "repair": "APPROVED",
                "privacy": "COMPLETED", "onlineDdlScored": "1", "queryPlanScored": "1",
                "poolScored": "1", "restoreScored": "1", "failoverScored": "1", "archive": "SUCCEEDED",
            } and archive_rejected.returncode != 0,
            "values": governance_values,
            "mismatchedArchiveSuccessRejected": archive_rejected.returncode != 0,
        }

        mysql.sql("""
SET SESSION cte_max_recursion_depth=60000;
INSERT INTO aoo_room_event(room_id,round_no,event_sequence,business_event_id,event_type,schema_version,is_compensation,compensates_business_event_id,compensation_reason,event_payload,created_at,visibility,owner_player_id)
WITH RECURSIVE sequence_rows AS (
    SELECT 1 AS value
    UNION ALL SELECT value+1 FROM sequence_rows WHERE value<50000
)
SELECT 700001,1,value,CONCAT('gate-',value),'GateEvent',1,FALSE,NULL,NULL,JSON_OBJECT('sequence',value),TIMESTAMPADD(MICROSECOND,value,'2026-01-01 00:00:00.000'),'PUBLIC',0
FROM sequence_rows;
""")
        direct_plan = mysql.sql("EXPLAIN FORMAT=JSON SELECT compiled.release_id FROM aoo_compiled_index_active active JOIN aoo_compiled_room_create_index compiled ON compiled.game_id=active.game_id AND compiled.region_code=active.region_code AND compiled.play_version=active.play_version AND compiled.index_generation=active.index_generation WHERE active.game_id=900000 AND active.region_code='GLOBAL';")
        keyset_plan = mysql.sql("EXPLAIN FORMAT=JSON SELECT event_sequence,event_payload FROM aoo_room_event WHERE room_id=700001 AND event_sequence>49000 AND visibility='PUBLIC' AND owner_player_id=0 ORDER BY event_sequence LIMIT 100;")
        keyset_analyze = mysql.sql("EXPLAIN ANALYZE SELECT event_sequence,event_payload FROM aoo_room_event WHERE room_id=700001 AND event_sequence>49000 AND visibility='PUBLIC' AND owner_player_id=0 ORDER BY event_sequence LIMIT 100;")
        offset_analyze = mysql.sql("EXPLAIN ANALYZE SELECT event_sequence,event_payload FROM aoo_room_event WHERE room_id=700001 AND visibility='PUBLIC' AND owner_player_id=0 ORDER BY event_sequence LIMIT 49000,100;")
        plan_text = (direct_plan + keyset_plan).lower()
        result["checks"]["queryPlans"] = {
            "passed": "table scan" not in plan_text and ('"access_type": "const"' in plan_text or '"access_type": "ref"' in plan_text or '"access_type": "range"' in plan_text),
            "datasetRows": 50000,
            "roomCreateIndexExplain": json.loads(direct_plan),
            "keysetExplain": json.loads(keyset_plan),
            "keysetAnalyze": keyset_analyze,
            "deepOffsetAnalyzeComparisonOnly": offset_analyze,
        }

        violation_counts = {
            view: int(mysql.sql("SELECT COUNT(*) FROM {};".format(view)))
            for view in (
                "v_aoo_active_game_coverage_violation", "v_aoo_component_completeness_violation",
                "v_aoo_component_graph_violation", "v_aoo_release_preflight_violation",
                "v_aoo_data_governance_gap",
            )
        }
        result["checks"]["integrityViews"] = {"passed": all(count == 0 for count in violation_counts.values()), "counts": violation_counts}

        operational_sql = {}
        for relative_path in ("operations/core-query-plans.sql", "operations/data-quality-detectors.sql"):
            path = DATABASE_DIR / relative_path
            execution = mysql.sql_result(path.read_text(encoding="utf-8"), check=False)
            operational_sql[relative_path] = {
                "passed": execution.returncode == 0,
                "sha256": sha256_file(path),
                "error": execution.stderr.decode("utf-8", "replace")[-1000:] if execution.returncode else "",
            }
        result["checks"]["operationalSql"] = {"passed": all(item["passed"] for item in operational_sql.values()), "files": operational_sql}

        dictionary = build_dictionary(mysql)
        dictionary["contentHash"] = hashlib.sha256(json.dumps(dictionary, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest()
        DICTIONARY_PATH.parent.mkdir(parents=True, exist_ok=True)
        DICTIONARY_PATH.write_text(json.dumps(dictionary, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        result["dataDictionary"] = {"path": str(DICTIONARY_PATH.relative_to(DATABASE_DIR.parent)), "sha256": sha256_file(DICTIONARY_PATH), "tableCount": len(dictionary["tables"]), "columnCount": len(dictionary["columns"]), "indexColumnCount": len(dictionary["indexes"]), "constraintCount": len(dictionary["constraints"])}

        result["passed"] = all(check.get("passed", False) for check in result["checks"].values())
    except Exception as error:
        result["passed"] = False
        result["fatalError"] = repr(error)
    finally:
        result["elapsedMs"] = round((time.monotonic() - started) * 1000, 3)
        result["migrationCount"] = len(result["migrations"])
        AUDIT_PATH.parent.mkdir(parents=True, exist_ok=True)
        AUDIT_PATH.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        mysql.stop()
    print(json.dumps({"passed": result.get("passed", False), "audit": str(AUDIT_PATH), "fatalError": result.get("fatalError")}, ensure_ascii=False))
    return 0 if result.get("passed") else 1


if __name__ == "__main__":
    raise SystemExit(main())

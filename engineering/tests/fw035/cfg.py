import json,sys
from login_catalog import login,api
t=login()
for gid,pv in ((8,'1.0.0'),(90005,'xqp-equivalent-1'),(631,'mn201-v1.0.0')):
    s,b=api('GET',f'/api/v2/hall/configuration?gameId={gid}&playVersion={pv}&clientVersion=3.8.8',t)
    print('=== game',gid,pv,'status',s)
    print(b[:2500])
    print()

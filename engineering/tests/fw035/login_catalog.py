import json,urllib.request,urllib.parse,sys
ACC='http://127.0.0.1:8096'; GW='http://127.0.0.1:8080'
ORIGIN='http://127.0.0.1:7461'
def login(identity='21'):
    h={'X-Aoo-Api-Version':'1','X-Client-Channel':'stable','X-Client-Version':'0.1.0','Content-Type':'application/x-www-form-urlencoded;charset=UTF-8','X-Device-Id':'fw035-diag','Origin':ORIGIN}
    body=urllib.parse.urlencode({'identity':identity,'password':'1','mode':'multi'}).encode()
    r=urllib.request.Request(ACC+'/api/v2/account/login',data=body,headers=h,method='POST')
    with urllib.request.urlopen(r,timeout=10) as resp: return json.loads(resp.read())['accessToken']
def api(method,path,token,payload=None,base=GW):
    h={'Authorization':'Bearer '+token,'Origin':ORIGIN,'X-Aoo-Api-Version':'1','X-Client-Version':'3.8.8','X-Trace-Id':'fw035-diag','Content-Type':'application/json'}
    data=json.dumps(payload).encode() if payload is not None else None
    r=urllib.request.Request(base+path,data=data,headers=h,method=method)
    try:
        with urllib.request.urlopen(r,timeout=20) as resp: return resp.status,resp.read().decode()
    except urllib.error.HTTPError as e: return e.code,e.read().decode()
if __name__=='__main__':
    t=login()
    s,b=api('GET','/api/v2/hall/catalog?clientVersion=3.8.8',t)
    d=json.loads(b)['data']
    print('count',len(d))
    for g in d: print(g['gameId'],g['gameCode'],g['playVersion'],g['classificationRegionCode'],g.get('displayName'))

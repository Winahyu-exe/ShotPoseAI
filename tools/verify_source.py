#!/usr/bin/env python3
"""Offline source/resource and DAO query checks; does NOT replace Android compilation."""
from pathlib import Path
import json, re, sqlite3, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'
checks=[]
def ok(label, condition):
    assert condition, label
    checks.append(label)
xml_files=list(MAIN.rglob('*.xml'))
for file in xml_files: ET.parse(file)
ok('All Android XML documents parse',True)
android='{http://schemas.android.com/apk/res/android}'
layouts={}
for file in (MAIN/'res/layout').glob('*.xml'):
    ids=[x.get(android+'id','').replace('@+id/','').replace('@id/','') for x in ET.parse(file).iter() if x.get(android+'id')]
    ok('Unique IDs: '+file.name,len(ids)==len(set(ids)))
    layouts[''.join(p.title() for p in file.stem.split('_'))+'Binding']={ids[i].split('_')[0]+''.join(x.title() for x in ids[i].split('_')[1:]) for i in range(len(ids))}|{'root'}
for file in (MAIN/'java').rglob('*.kt'):
    text=file.read_text()
    imports=re.findall(r'import com\.shotposeai\.databinding\.(\w+)',text)
    if len(imports)==1 and imports[0] in layouts:
        refs=set(re.findall(r'\b(?:b|binding)\.(\w+)',text))
        ok('ViewBinding references: '+file.name,not(refs-layouts[imports[0]]))
resources={}
for folder in (MAIN/'res').iterdir():
    if folder.is_dir() and not folder.name.startswith('values'):
        resources.setdefault(folder.name.split('-')[0],set()).update(p.stem for p in folder.iterdir() if p.is_file())
for f in (MAIN/'res').glob('values*/*.xml'):
    for e in ET.parse(f).getroot():
        if e.get('name'):resources.setdefault(e.tag,set()).add(e.get('name'))
ids=set()
for f in xml_files:
    ids.update(re.findall(r'@\+id/(\w+)',f.read_text()))
resources['id']=ids
for file in list(MAIN.rglob('*.xml'))+list((MAIN/'java').rglob('*.kt')):
    text=file.read_text()
    refs=re.findall(r'@(drawable|layout|color|string|xml|menu|mipmap)/([\w]+)',text)
    refs+=re.findall(r'(?<!android\.)\bR\.(drawable|layout|color|string|xml|menu|mipmap|id)\.(\w+)',text)
    for typ,name in refs:
        ok('Local resource '+typ+'/'+name+' in '+file.name,name in resources.get(typ,set()))
# Build schema from the actual Java entity declarations.
db=sqlite3.connect(':memory:')
for file in (MAIN/'java/com/shotposeai/data').glob('*Entity.java'):
    text=file.read_text();table=re.search(r'tableName\s*=\s*"(\w+)"',text).group(1)
    fields=re.findall(r'public (long|int|String) (\w+);',text)
    parts=[f'{name} '+('TEXT' if kind=='String' else 'INTEGER')+(' PRIMARY KEY' if name=='id' else '') for kind,name in fields]
    keys=re.search(r'primaryKeys\s*=\s*\{([^}]+)',text)
    if keys: parts.append('PRIMARY KEY ('+','.join(re.findall(r'"(\w+)"',keys.group(1)))+')')
    db.execute(f'CREATE TABLE {table} ('+','.join(parts)+')')
text=(MAIN/'java/com/shotposeai/data/AppDao.java').read_text()
queries={}
for match in re.finditer(r'@Query\(\s*((?:"(?:[^"\\]|\\.)*"\s*(?:\+\s*)?)+)\)\s+[^;]+?\b(\w+)\([^;]*?;',text,re.S):
    sql=''.join(json.loads('"'+x+'"') for x in re.findall(r'"((?:[^"\\]|\\.)*)"',match.group(1)))
    queries[match.group(2)]=sql
    params={x:0 for x in re.findall(r':(\w+)',sql)}
    db.execute('EXPLAIN '+sql,params)
ok('Every DAO SQL query prepares against entity schema',bool(queries))
threshold=int(re.search(r'REQUIRED_SCORE\s*=\s*(\d+)',(MAIN/'java/com/shotposeai/analysis/AchievementRules.kt').read_text()).group(1))
def count():return db.execute(queries['completedStudentCount'],{'movement':'Dribbling','threshold':threshold}).fetchone()[0]
def session(student,movement,score,status='COMPLETED'):
    db.execute('INSERT INTO training_sessions(studentId,movementName,score,status) VALUES(?,?,?,?)',(student,movement,score,status))
ok('Empty progress is zero',count()==0)
db.executemany('INSERT INTO students(id,name) VALUES(?,?)',[(1,'A'),(2,'B'),(3,'C')])
session(1,'Dribbling',84);ok('84 does not earn achievement',count()==0)
session(1,'Dribbling',85);ok('85 earns achievement',count()==1)
session(1,'Dribbling',98);ok('Repeated sessions count a student only once',count()==1)
session(2,'Passing kaki bagian dalam',99);ok('Other movements do not inflate Dribbling progress',count()==1)
session(3,'Dribbling',100,'DRAFT');ok('Incomplete sessions do not count',count()==1)
session(999,'Dribbling',100);ok('Deleted or missing students do not inflate progress',count()==1)
best=dict(db.execute(queries['getMovementBest'],{'studentId':1}));ok('Movement progress uses highest historical score',best=={'Dribbling':98})
ok('Student progress stays isolated',dict(db.execute(queries['getMovementBest'],{'studentId':2}))=={'Passing kaki bagian dalam':99})
for _ in range(2):db.execute('INSERT OR IGNORE INTO student_achievements VALUES(1,1,0)')
ok('An achievement is stored once per student',db.execute('SELECT COUNT(*) FROM student_achievements').fetchone()[0]==1)
movements=set(re.findall(r'MovementDefinition\("([^"]+)"',(MAIN/'java/com/shotposeai/analysis/MovementCatalog.kt').read_text()))
videos=dict(re.findall(r'"([^"]+)" to Video\("([^"]+)"',(MAIN/'java/com/shotposeai/analysis/TechniqueVideos.kt').read_text()))
ok('Every movement has a technique video',set(videos)==movements)
ok('YouTube video IDs have valid syntax',all(re.fullmatch(r'[A-Za-z0-9_-]{11}',v) for v in videos.values()))
ok('Manifest permits YouTube internet access','android.permission.INTERNET' in (MAIN/'AndroidManifest.xml').read_text())
print(f'PASS: {len(checks)} source/resource/query assertions; {len(xml_files)} XML files; {len(queries)} DAO queries.')
print('Android build, device interaction and visual parity have NOT been verified by this script.')

#!/usr/bin/env python3
"""Build knowledge_graph.json v0.1 from dim1-formula.md + examples.json.

Node IDs mirror KnowledgeImporter.generateId(): global counter, block code A/G/P/S,
grade code G5/G6/G7/G8/G9/G10.
"""
import json, re, collections, os, datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIM1 = os.path.join(ROOT, 'dim1-formula.md')
EXAMPLES = os.path.join(ROOT, 'app/src/main/assets/examples.json')
OUT = os.path.join(ROOT, 'app/src/main/assets/knowledge_graph.json')

BLOCKS = ['数与代数', '图形与几何', '统计与概率', '综合与实践']
GRADE_KEYS = [('五年级','五年级'),('六年级','六年级'),('初一','初一'),('初二','初二'),('初三','初三'),
              ('高一','高一'),('高二','高二'),('高三','高三'),('五~六','五~六年级'),('五～六','五~六年级'),
              ('七~九','七~九年级'),('八~九','八~九年级'),('初一~初三','七~九年级')]
GRADE_RANK = {'五年级':1,'六年级':2,'五~六年级':2,'初一':7,'七~九年级':8,'初二':8,'初三':9,'高一':10,'高二':11,'高三':12}
GRADE_RANK.update({'初一~初三':8,'初三~高一':10})

def extract_grade(text):
    for key, val in sorted(GRADE_KEYS, key=lambda x: -len(x[0])):
        if key in text:
            return val
    return ''

def block_to_code(block):
    if '数' in block: return 'A'
    if '图' in block: return 'G'
    if '概' in block: return 'P'
    if '践' in block: return 'S'
    return 'X'

def grade_to_code(grade):
    if '五' in grade: return 'G5'
    if '六' in grade: return 'G6'
    if '初' in grade: return 'G7'
    if '八' in grade: return 'G8'
    if '九' in grade: return 'G9'
    if '高' in grade: return 'G10'
    return 'G00'

def parse_dim1():
    lines = open(DIM1, encoding='utf-8').read().split('\n')
    nodes, counter = [], 0
    block = ''; grade = ''; lv1 = lv2 = lv3 = lv4 = ''
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        m = re.match(r'^# 板块[一二三四1-4]*[：:](.+)$', line)
        if m:
            name = m.group(1).strip()
            block = name if name in BLOCKS else ''
            lv1 = lv2 = lv3 = lv4 = ''
        elif line.startswith('## '):
            lv1 = line[3:].strip(); lv2 = lv3 = lv4 = ''
            g = extract_grade(lv1)
            if g: grade = g
        elif line.startswith('### '):
            lv2 = line[4:].strip(); lv3 = lv4 = ''
            g = extract_grade(lv2)
            if g: grade = g
        elif line.startswith('#### '):
            lv3 = line[5:].strip(); lv4 = ''
            g = extract_grade(lv3)
            if g: grade = g
        elif line.startswith('**') and '知识点：' in line:
            counter += 1
            title = line.removeprefix('**').removeprefix('知识点：').strip().removesuffix('**').strip()
            fm = re.search(r'\$\$(.+?)\$\$', line)
            formula = fm.group(1).strip() if fm else ''
            title_grade = extract_grade(title)
            eff_grade = title_grade or grade
            code = f"KB_{block_to_code(block)}_{grade_to_code(eff_grade)}_K{counter:03d}"
            path = block + ''.join(' > ' + x for x in [lv1, lv2, lv3, lv4] if x)
            nodes.append({
                'id': code, 'title': title, 'block': block, 'grade': eff_grade,
                'categoryLv1': lv1, 'categoryLv2': lv2, 'categoryLv3': lv3,
                'fullPath': path, 'formula': formula,
            })
        i += 1
    return nodes

def norm(s):
    s = re.sub(r'\$', '', s)
    s = re.sub(r'（.*?）|\(.*?\)', '', s)
    s = re.sub(r'[\s\\{}^_]', '', s)
    return s

def aliases_for(node):
    title = node['title']
    base = re.sub(r'（.*?）|\(.*?\)', '', title).strip()
    out = {title, base}
    out.add(re.sub(r'\$', '', title))
    # 常见俗称/教材别名（保守）
    CURATED = {
        '十字相乘法': ['十字相乘'], '完全平方公式': ['完全平方'], '平方差公式': ['平方差'],
        '一元一次方程解法': ['解一元一次方程'], '代入消元法': ['代入法'], '加减消元法': ['加减法'],
        '二次根式乘除': ['根式乘除'], '二次根式加减': ['根式加减'], '提公因式法': ['提公因式'],
        '运用公式法': ['公式法'], '分组分解法': ['分组分解'], '待定系数法求一次函数': ['待定系数法'],
        '平行线判定': ['平行线判定定理'], '平行线性质': ['平行线性质定理'], '绝对值运算性质': ['绝对值化简'],
    }
    out.update(CURATED.get(base, []))
    return sorted(x for x in out if x)

def build_edges(nodes):
    edges = []
    # 同类兄弟：按年级顺序串成前置链（启发式，confidence=low）
    by_cat = collections.defaultdict(list)
    for n in nodes:
        key = (n['block'], n['categoryLv2'] or n['categoryLv1'])
        by_cat[key].append(n)
    for key, arr in by_cat.items():
        arr = sorted(arr, key=lambda n: (GRADE_RANK.get(n['grade'], 99), n['id']))
        for a, b in zip(arr, arr[1:]):
            if a['id'] != b['id']:
                edges.append({'from': a['id'], 'to': b['id'], 'type': 'PREREQUISITE',
                              'confidence': 0.45, 'note': '同章节按年级顺序的启发式前置'})
    # 同名跨年级：related
    by_title = collections.defaultdict(list)
    for n in nodes:
        by_title[norm(n['title'])].append(n)
    for arr in by_title.values():
        for a in arr:
            for b in arr:
                if a['id'] < b['id']:
                    edges.append({'from': a['id'], 'to': b['id'], 'type': 'RELATED',
                                  'confidence': 0.7, 'note': '同名知识点（不同年级/场景）'})
    return edges

PATHS = [
    ('PATH_G7_NUMBER', '初一 · 有理数运算', '初一', ['有理数分类','数轴与实数对应','相反数','绝对值定义','绝对值非负性','绝对值运算性质','有理数加法','有理数减法','有理数乘法','有理数除法','有理数乘方']),
    ('PATH_G7_ALGEBRA', '初一 · 整式与一元一次方程', '初一', ['单项式与多项式','合并同类项','去括号法则','整式加减步骤','等式性质','移项法则','一元一次方程标准形式','一元一次方程解法']),
    ('PATH_G7_SYSTEM', '初一 · 二元一次方程组', '初一', ['二元一次方程组一般形式','代入消元法','加减消元法','方程组解的判定']),
    ('PATH_G7_INEQ', '初一 · 不等式', '初一', ['不等式基本性质','一元一次不等式解集','绝对值不等式']),
    ('PATH_G7_GEOM', '初一 · 几何入门', '初一', ['直线、射线、线段','角','余角与补角','余角性质','平行线判定','平行线性质']),
    ('PATH_G8_POWER', '初二 · 幂与整式乘法', '初二', ['同底数幂乘法','同底数幂除法','幂的乘方','积的乘方','零指数幂与负指数幂','单项式乘以单项式','单项式乘以多项式','多项式乘以多项式','平方差公式','完全平方公式','立方和与立方差公式','完全立方公式']),
    ('PATH_G8_FACTOR', '初二 · 因式分解', '初二', ['提公因式法','运用公式法','十字相乘法','分组分解法','因式分解一般步骤']),
    ('PATH_G8_FRACTION', '初二 · 分式', '初二', ['分式有意义条件','分式基本性质','分式乘法','分式除法','分式加减','分式方程']),
    ('PATH_G8_RADICAL', '初二 · 二次根式', '初二', ['二次根式定义','二次根式性质','积的算术平方根','商的算术平方根','二次根式乘除','二次根式加减']),
    ('PATH_G8_FUNCTION', '初二 · 一次函数与反比例函数', '初二', ['一次函数定义','斜率公式','点斜式','两点式','截距式','一般式','平行条件','垂直条件','待定系数法求一次函数','反比例函数定义','反比例函数变形','函数图像']),
    ('PATH_G8_TRIANGLE', '初二 · 三角形与全等', '初二', ['三角形内角和','三角形外角性质','三角形三边关系','SSS 判定','SAS 判定','ASA 判定','AAS 判定','HL 判定（直角三角形）','等腰三角形性质','等腰三角形判定','等边三角形性质','等边三角形判定','线段垂直平分线性质','轴对称的性质']),
    ('PATH_G8_QUAD', '初二 · 四边形与勾股', '初二', ['勾股定理','勾股定理逆定理','勾股数（勾股弦三元组）','平行四边形面积','平行四边形性质','平行四边形判定','矩形性质','矩形判定','菱形性质','菱形判定','正方形性质','梯形面积','梯形中位线']),
    ('PATH_G8_STATS', '初二 · 统计初步', '初二', ['平均数','加权平均数','中位数','众数','方差','标准差','方差与标准差的意义']),
]

def build_paths(nodes):
    by_title = collections.defaultdict(list)
    for n in nodes:
        by_title[norm(n['title'])].append(n['id'])
    out = []
    for pid, title, grade, titles in PATHS:
        ids = []
        for t in titles:
            ids += by_title.get(norm(t), [])
        ids = list(dict.fromkeys(ids))
        out.append({'id': pid, 'title': title, 'grade': grade, 'nodeIds': ids})
        for nid in ids:
            if 'pathIds' not in next(x for x in nodes if x['id'] == nid):
                pass
    return out

def build_facets(nodes):
    """v0.2：节点能力层 + 题型模型 + 学习主线，规则/人工映射，带置信度。"""
    # 1) 能力层：按标题/分类关键词粗分 L1 基础运算 / L2 概念理解 / L3 逻辑推理
    L1 = ['运算','计算','加法','减法','乘法','除法','乘方','化简','法则','步骤','公式']
    L2 = ['定义','性质','意义','概念','一般形式','图像','图象','关系','条件','表示','分类']
    L3 = ['判定','证明','推理','应用','综合','思想','建模','不等式']
    for n in nodes:
        text = n['title'] + ' ' + n['categoryLv2']
        if any(k in text for k in L3):
            n['abilityLevel'] = 'L3'
        elif any(k in text for k in L1):
            n['abilityLevel'] = 'L1'
        else:
            n['abilityLevel'] = 'L2'
    abilities = []
    for lvl in ['L1','L2','L3']:
        ids = [n['id'] for n in nodes if n['abilityLevel'] == lvl]
        abilities.append({'level': lvl, 'nodeIds': ids, 'confidence': 0.5,
                          'note': '按标题/分类关键词的启发式分层'})

    # 2) 题型模型：人工整理的初中高频模型 -> 关键词
    MODEL_RULES = [
        ('有理数运算', ['有理数']), ('整式化简', ['整式', '合并同类项', '去括号']),
        ('因式分解', ['因式分解', '十字相乘', '提公因式', '公式法']),
        ('解方程', ['一元一次方程', '二元一次方程', '方程']),
        ('比例应用', ['比例', '百分数']), ('运算法则', ['运算', '法则']),
        ('配方法', ['一元二次方程', '配方']), ('换元法', ['整式', '分式', '二次根式']),
        ('全等证明', ['判定', '全等', '三角形']), ('等腰三角形性质', ['等腰三角形']),
        ('平行四边形判定', ['平行四边形']), ('勾股定理应用', ['勾股']),
        ('辅助线', ['三角形', '梯形', '平行四边形']), ('面积法', ['面积']),
        ('函数图像性质', ['函数']), ('数形结合', ['函数', '绝对值', '不等式', '数轴']),
        ('分类讨论', ['绝对值', '不等式']), ('坐标法', ['坐标', '函数']),
        ('统计计算', ['统计', '平均数', '方差', '中位数', '众数']),
    ]
    d4 = open(os.path.join(ROOT, 'dim4-formula.md'), encoding='utf-8').read()
    bullets = [m.group(1) for m in re.finditer(r'^[-*] (.+)$', d4, re.M)]
    models = []
    for model_name, keywords in MODEL_RULES:
        ids = []
        for n in nodes:
            text = n['title'] + ' ' + n['categoryLv2']
            if any(k in text for k in keywords):
                ids.append(n['id'])
        if not ids:
            continue
        models.append({'name': model_name, 'nodeIds': sorted(set(ids)),
                       'confidence': 0.55, 'note': '按关键词人工映射'})

    # 3) 学习主线阶段：人工映射到主题关键词
    ROUTE_MAP = {
        '数感阶段': ['有理数', '小数', '分数', '百分数', '运算'],
        '式感阶段': ['整式', '代数式', '因式分解', '分式'],
        '方程阶段': ['方程', '方程组', '不等式'],
        '函数阶段': ['函数', '坐标', '图像'],
        '几何直观': ['图形', '角', '平行线', '三角形', '线段'],
        '空间观念': ['平行四边形', '矩形', '菱形', '正方形', '勾股', '轴对称', '梯形'],
        '向量思维': ['向量'],
        '解析几何': ['直线', '圆', '圆锥曲线', '坐标'],
        '数据分析': ['统计', '平均数', '中位数', '方差', '频数'],
        '随机思维': ['概率', '频率'],
        '统计推断': ['统计', '概率', '方差'],
        '推理能力': ['判定', '性质', '证明', '全等'],
        '模型思想': ['函数', '方程', '不等式', '应用'],
        '数学建模': ['应用', '综合实践', '函数', '统计'],
    }
    d3 = open(os.path.join(ROOT, 'dim3-formula.md'), encoding='utf-8').read()
    stages = re.findall(r'^### (.+)$', d3, re.M)
    routes = []
    for stage in stages:
        keywords = ROUTE_MAP.get(stage)
        if not keywords:
            routes.append({'stage': stage, 'nodeIds': [], 'confidence': 0, 'note': '未映射'})
            continue
        ids = []
        for n in nodes:
            text = n['title'] + ' ' + n['categoryLv2']
            if any(k in text for k in keywords):
                ids.append(n['id'])
        routes.append({'stage': stage, 'nodeIds': sorted(set(ids)),
                       'confidence': 0.5, 'note': '按主题关键词人工映射'})
    return abilities, models, routes


def build_examples(nodes):
    ex = json.load(open(EXAMPLES, encoding='utf-8'))
    by_title = collections.defaultdict(list)
    for n in nodes:
        by_title[re.sub(r'（[^）]*）$', '', n['title']).strip()].append(n['id'])
    mapping = []
    for e in ex:
        ids = by_title.get(e['knowledgeTitle'], [])
        mapping.append({'exampleId': e['id'], 'title': e['knowledgeTitle'],
                        'knowledgeId': ids[0] if ids else None, 'candidates': ids})
    return mapping

def main():
    nodes = parse_dim1()
    assert len(nodes) == 368, f'expected 368 nodes, got {len(nodes)}'
    for n in nodes:
        n['aliases'] = aliases_for(n)
    edges = build_edges(nodes)
    paths = build_paths(nodes)
    for p in paths:
        for nid in p['nodeIds']:
            for n in nodes:
                if n['id'] == nid:
                    n.setdefault('pathIds', []).append(p['id'])
                    break
        for a, b in zip(p['nodeIds'], p['nodeIds'][1:]):
            if a != b:
                edges.append({'from': a, 'to': b, 'type': 'PREREQUISITE',
                              'confidence': 0.8, 'note': f'专题路径顺序：{p["title"]}'})
    abilities, models, routes = build_facets(nodes)
    examples = build_examples(nodes)
    graph = {
        'version': '0.1.0',
        'generatedAt': datetime.datetime.now().isoformat(timespec='seconds'),
        'source': 'dim1-formula.md (nodes) + dim2/3/4 (facets) + examples.json (links)',
        'stats': {
            'nodes': len(nodes), 'edges': len(edges), 'paths': len(paths),
            'abilities': len(abilities), 'questionModels': len(models),
            'examples': len(examples),
            'examplesLinked': sum(1 for x in examples if x['knowledgeId']),
        },
        'nodes': nodes, 'edges': edges, 'paths': paths,
        'abilities': abilities, 'questionModels': models, 'routes': routes,
        'examples': examples,
    }
    json.dump(graph, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    # keep examples.json in sync with knowledgeId
    exdata = json.load(open(EXAMPLES, encoding='utf-8'))
    by_id = {x['exampleId']: x['knowledgeId'] for x in examples}
    for e in exdata:
        kid = by_id.get(e['id'])
        if kid:
            e['knowledgeId'] = kid
    json.dump(exdata, open(EXAMPLES, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print('graph written:', OUT)
    print(graph['stats'])
    print('path node coverage:', [(p['id'], len(p['nodeIds'])) for p in paths])
    print('examples without knowledgeId:', [x['exampleId'] for x in examples if not x['knowledgeId']])
    print('sample node:', nodes[0]['id'], nodes[0]['title'])
    print('sample edge:', edges[0] if edges else None)

if __name__ == '__main__':
    main()

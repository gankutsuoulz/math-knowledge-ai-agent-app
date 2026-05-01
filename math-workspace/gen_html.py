#!/usr/bin/env python3
import re

def make_html(md, title):
    md = re.sub(r'\\\[(.+?)\\\]', r'$$\1', md, flags=re.DOTALL)
    md = re.sub(r'\\\((.+?)\\\)', r'$\1', md, flags=re.DOTALL)
    md = re.sub(r'^```markdown\s*', '', md, flags=re.MULTILINE)
    md = re.sub(r'^```\s*$', '', md, flags=re.MULTILINE)

    lines = md.split('\n')
    out = []
    i = 0
    while i < len(lines):
        l = lines[i].strip()
        if not l:
            i += 1
            continue
        if l.startswith('# '):
            out.append('<h1>' + l[2:] + '</h1>')
        elif l.startswith('## '):
            out.append('<h2>' + l[3:] + '</h2>')
        elif l.startswith('### '):
            out.append('<h3>' + l[4:] + '</h3>')
        elif l.startswith('- '):
            items = []
            while i < len(lines) and lines[i].strip().startswith('- '):
                items.append('<li>' + lines[i].strip()[2:] + '</li>')
                i += 1
            if items:
                out.append('<ul>' + ''.join(items) + '</ul>')
            continue
        elif l.startswith('|'):
            # Collect table rows until a non-row line
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                stripped = lines[i].strip()
                # Skip separator rows like |---|---|---|
                if all(c in '| +-:' for c in stripped):
                    i += 1
                    continue
                cells = [c.strip() for c in stripped.split('|') if c.strip()]
                if cells:
                    rows.append('<tr>' + ''.join('<td>'+c+'</td>' for c in cells) + '</tr>')
                i += 1
            if rows:
                out.append('<table><thead>' + rows[0] + '</thead><tbody>' + ''.join(rows[1:]) + '</tbody></table>')
            continue
        else:
            out.append('<p>' + l + '</p>')
        i += 1

    body = '\n'.join(out)
    css = 'body{font-family:"Microsoft YaHei","PingFang SC",sans-serif;max-width:900px;margin:0 auto;padding:20px;line-height:1.8;color:#333}h1{color:#1a1a2e;border-bottom:3px solid #4361ee;padding-bottom:10px;margin-top:40px}h2{color:#3730a3;border-left:5px solid #6366f1;padding-left:12px;margin-top:30px}h3{color:#4f46e5;margin-top:20px}table{border-collapse:collapse;width:100%;margin:15px 0;box-shadow:0 2px 8px rgba(0,0,0,0.1)}th{background:#4361ee;color:#fff;padding:10px 15px}td{padding:8px 15px;border:1px solid #e0e0e0}tr:nth-child(even){background:#f8f9ff}tr:hover{background:#eef0ff}ul,ol{padding-left:24px}li{margin:6px 0}code{background:#f1f3ff;padding:2px 6px;border-radius:4px;color:#4338ca}blockquote{border-left:4px solid #6366f1;margin:15px 0;padding:8px 16px;background:#f8f9ff;color:#555}'
    head = '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"><title>' + title + '</title><link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css"><script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"></script><script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js"></script><script>window.addEventListener("load",function(){renderMathInElement(document.body,{delimiters:[{left:"$$",right:"$$",display:true},{left:"$",right:"$",display:false}],throwOnFalse:false})});</script><style>' + css + '</style></head><body>'
    return head + body + '\n</body>\n</html>'

for fname, title in [
    ("dim2-formula.md", "维度二：思维层级与知识点体系"),
    ("dim3-formula.md", "维度三：核心素养主线"),
    ("dim4-formula.md", "维度四：解题策略与考试热点"),
]:
    with open('/projects/math-workspace/' + fname) as f:
        md = f.read()
    html = make_html(md, title)
    out = fname.replace('.md', '.html')
    with open('/projects/math-workspace/' + out, 'w') as f:
        f.write(html)
    print(out + ': ' + str(len(html)) + ' bytes, $: ' + str(html.count('$')))

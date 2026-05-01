#!/usr/bin/env python3
import re, subprocess, os

HTML = '/projects/math-workspace/dim1-formula.html'
OUT_DIR = '/mnt/smb/education'
os.makedirs(OUT_DIR, exist_ok=True)

with open(HTML) as f:
    content = f.read()

# Find all h1 section boundaries
# We want to add page-break-before for 板块二/三/四
section_names = {
    '板块二：图形与几何': 'dim2-图形与几何',
    '板块三：统计与概率': 'dim3-统计与概率',
    '板块四：综合与实践': 'dim4-综合与实践'
}

# Split at each section start
parts = re.split(r'(<h1>板块[三四])', content)
# parts will be: [before板块一, 板块二, rest...]

# Build 4 full HTML strings
result = []
current = parts[0]  # before 板块二
for i in range(1, len(parts), 2):
    sec = parts[i]  # e.g. "<h1>板块二：图形与几何"
    body = parts[i+1] if i+1 < len(parts) else ''
    result.append(current + sec + body)
    # next starts from where we left
    if i+2 < len(parts):
        current = parts[i+2]

# Wait, our split pattern doesn't handle板块一 - let me restructure
# Split by <h1>板块
sections = re.split(r'(<h1>板块[一二三四]：)', content)

# sections[0] = before any板块 headings (header stuff)
# sections[1] = "<h1>板块一："  sections[2] = body of 板块一
# sections[3] = "<h1>板块二：" sections[4] = body of 板块二
# etc.

plate_names = ['数与代数', '图形与几何', '统计与概率', '综合与实践']
files = []

i = 0
sec_idx = 0
while i < len(sections):
    if sections[i] == '<h1>板块一：':
        body1 = sections[i+1] if i+1 < len(sections) else ''
        i += 2
    elif sections[i] == '<h1>板块二：':
        body2 = sections[i+1] if i+1 < len(sections) else ''
        i += 2
    elif sections[i] == '<h1>板块三：':
        body3 = sections[i+1] if i+1 < len(sections) else ''
        i += 2
    elif sections[i] == '<h1>板块四：':
        body4 = sections[i+1] if i+1 < len(sections) else ''
        i += 2
    else:
        i += 1

# Find the header (everything before 板块一 heading)
header_end = content.find('<h1>板块一：')
header = content[:header_end]
# Remove the old body closing and reconstruct

def make_html(header, body):
    # Ensure body ends properly
    if not body.strip():
        return ''
    return header + body + '\n</body>\n</html>'

html1 = header + sections[1] if len(sections) > 1 else ''
# Actually let me just find each section by searching directly

# Find start positions of each section
pos1 = content.find('<h1>板块一：')
pos2 = content.find('<h1>板块二：')
pos3 = content.find('<h1>板块三：')
pos4 = content.find('<h1>板块四：')

# Find where body content ends (next <h1> or end)
# Each section: from its h1 to just before next h1 (or end)

# Get the common header (everything before 板块一)
header_html = content[:pos1]
body1 = content[pos1:pos2]
body2 = content[pos2:pos3]
body3 = content[pos3:pos4]
body4 = content[pos4:]

def build_html(body):
    # Extract opening tags from header
    head_end = header_html.find('</head>')
    head = header_html[:head_end+6]
    rest = header_html[head_end+6:]
    # rest should be <body> plus first few elements
    body_start = rest.find('<body>')
    body_prefix = rest[:body_start+6]  # <body> tag included
    return head + body_prefix + body + '\n</body>\n</html>'

html_parts = [
    (build_html(body1), 'dim1-数与代数.html'),
    (build_html(body2), 'dim1-图形与几何.html'),
    (build_html(body3), 'dim1-统计与概率.html'),
    (build_html(body4), 'dim1-综合与实践.html'),
]

# Write temp HTMLs
for html, fname in html_parts:
    path = f'/tmp/{fname}'
    with open(path, 'w') as f:
        f.write(html)
    files.append(path)
    print(f'Wrote {path}: {len(html)} bytes')

print('Temp files ready')
print('Files:', files)
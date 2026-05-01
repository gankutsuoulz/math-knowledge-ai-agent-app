#!/usr/bin/env python3
"""Generate dim1-formula.html from dim1-formula.md with proper KaTeX rendering."""

import re

MD_FILE = '/projects/math-workspace/dim1-formula.md'
OUT_FILE = '/projects/math-workspace/dim1-formula.html'

# Read the markdown file
with open(MD_FILE, 'r', encoding='utf-8') as f:
    content = f.read()

# HTML template based on dim2 (which works correctly)
HTML_TEMPLATE = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>维度一：学科知识板块</title>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css">
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js"></script>
<script>
document.addEventListener("DOMContentLoaded", function() {{
  renderMathInElement(document.body, {{
    delimiters: [
      {{left: "$$", right: "$$", display: true}},
      {{left: "$", right: "$", display: false}}
    ],
    throwOnFalse: false
  }});
}});
</script>
<style>
body {{ font-family: "Microsoft YaHei", "PingFang SC", sans-serif; max-width: 1000px; margin: 0 auto; padding: 20px; line-height: 1.8; color: #333; }}
h1 {{ color: #1a1a2e; border-bottom: 3px solid #4361ee; padding-bottom: 10px; margin-top: 40px; }}
h2 {{ color: #3730a3; border-left: 5px solid #6366f1; padding-left: 12px; margin-top: 30px; }}
h3 {{ color: #4f46e5; margin-top: 20px; }}
h4 {{ color: #5b4fcf; margin-top: 16px; }}
table {{ border-collapse: collapse; width: 100%; margin: 15px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }}
th {{ background: #4361ee; color: white; padding: 12px 15px; }}
td {{ padding: 10px 15px; border: 1px solid #e0e0e0; }}
tr:nth-child(even) {{ background: #f8f9ff; }}
tr:hover {{ background: #eef0ff; }}
ul, ol {{ padding-left: 24px; }}
li {{ margin: 6px 0; }}
code {{ background: #f1f3ff; padding: 2px 6px; border-radius: 4px; color: #4338ca; }}
blockquote {{ border-left: 4px solid #6366f1; margin: 15px 0; padding: 8px 16px; background: #f8f9ff; color: #555; }}
hr {{ border: none; border-top: 2px solid #e0e0e0; margin: 24px 0; }}
</style>
</head>
<body>
{content}
</body>
</html>
'''

def process_line(line):
    """Process a single line of markdown and convert to HTML."""
    # Skip empty lines at boundaries
    if not line.strip():
        return ''
    
    # Handle headings
    if line.startswith('#### '):
        text = line[5:]
        text = process_inline(text)
        return f'<h4>{text}</h4>\n'
    elif line.startswith('### '):
        text = line[4:]
        text = process_inline(text)
        return f'<h3>{text}</h3>\n'
    elif line.startswith('## '):
        text = line[3:]
        text = process_inline(text)
        return f'<h2>{text}</h2>\n'
    elif line.startswith('# '):
        text = line[2:]
        text = process_inline(text)
        return f'<h1>{text}</h1>\n'
    
    # Handle horizontal rules
    if line.strip() == '---':
        return '<hr>\n'
    
    # Handle blockquotes
    if line.startswith('>'):
        text = line[1:].strip()
        text = process_inline(text)
        return f'<blockquote>{text}</blockquote>\n'
    
    # Handle unordered list items
    if line.startswith('- '):
        text = process_inline(line[2:])
        return f'<li>{text}</li>\n'
    
    # Handle tables - find lines that look like table rows
    if '|' in line and line.strip().startswith('|'):
        return process_table_row(line)
    
    # Check if it's a formula line (contains $$)
    if '$$' in line:
        # It's a formula - return as-is for KaTeX
        return line + '\n'
    
    # Regular paragraph
    text = process_inline(line)
    if text.strip():
        return f'<p>{text}</p>\n'
    return ''

def process_inline(text):
    """Process inline markdown formatting: **bold**, $math$"""
    # Process bold: **text** -> <strong>text</strong>
    result = []
    i = 0
    while i < len(text):
        if text[i:i+2] == '**':
            end = text.find('**', i+2)
            if end != -1:
                result.append('<strong>')
                result.append(text[i+2:end])
                result.append('</strong>')
                i = end + 2
            else:
                result.append(text[i])
                i += 1
        else:
            result.append(text[i])
            i += 1
    return ''.join(result)

def process_table_row(line):
    """Process a markdown table row."""
    cells = [c.strip() for c in line.split('|')[1:-1]]  # Split by | and remove empty first/last
    
    # Check if it's a header row (contains ---)
    if all(re.match(r'^-+$', c) for c in cells):
        return ''  # Skip separator rows
    
    # Determine if this is a header row
    is_header = False
    for cell in cells:
        if '<th>' in cell or '</th>' in cell:
            is_header = True
            break
        # Check if cell contains only formatting
        clean_cell = re.sub(r'[<>$*]', '', cell)
        if not clean_cell.strip() or clean_cell.strip().startswith('---'):
            continue
        if '$' in cell:  # Contains math
            continue
        # If it looks like a header (all cells are short)
        if len(cells) > 0 and all(len(c) < 20 for c in cells):
            is_header = True
    
    tag = 'th' if is_header else 'td'
    
    # Process inline content in cells
    processed_cells = []
    for cell in cells:
        # Skip separator dashes
        if re.match(r'^-+$', cell):
            continue
        processed = process_inline(cell)
        if processed:
            processed_cells.append(f'<{tag}>{processed}</{tag}>')
    
    if processed_cells:
        return '<tr>' + ''.join(processed_cells) + '</tr>\n'
    return ''

def process_markdown(content):
    """Process entire markdown content."""
    lines = content.split('\n')
    result = []
    table_rows = []
    in_table = False
    
    for line in lines:
        # Handle tables
        if '|' in line and line.strip().startswith('|'):
            if not in_table:
                in_table = True
                table_rows = []
            table_rows.append(line)
        else:
            # Finish any pending table
            if in_table:
                result.append(process_table(table_rows))
                table_rows = []
                in_table = False
            result.append(process_line(line))
    
    # Handle final table if exists
    if in_table:
        result.append(process_table(table_rows))
    
    return ''.join(result)

def process_table(rows):
    """Process a table from rows."""
    if not rows:
        return ''
    
    html = '<table>\n'
    for row in rows:
        processed_row = process_table_row(row)
        if processed_row and '<tr>' in processed_row:
            # Check if it's a header row
            if '<th>' in processed_row:
                html = '<table>\n'  # Reset to include header
            html += processed_row
    html += '</table>\n'
    return html

# Process the markdown
processed = process_markdown(content)

# Generate final HTML
final_html = HTML_TEMPLATE.format(content=processed)

# Write output
with open(OUT_FILE, 'w', encoding='utf-8') as f:
    f.write(final_html)

print(f"Generated {OUT_FILE}: {len(final_html)} bytes")
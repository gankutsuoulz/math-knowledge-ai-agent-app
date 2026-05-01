#!/usr/bin/env python3
import re

# The original complete dim4 HTML (before LLM truncation)
ORIGINAL = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>数学知识框架（五年级～高三）</title>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css">
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js"></script>
<script>
window.addEventListener("load", function() {
  renderMathInElement(document.body, {
    delimiters: [
      {left: "$$", right: "$$", display: true},
      {left: "$", right: "$", display: false}
    ],
    throwOnFalse: false
  });
});
</script>
<style>
  body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; line-height: 1.8; color: #333; }
  h1 { color: #1a1a2e; border-bottom: 3px solid #4361ee; padding-bottom: 10px; margin-top: 40px; }
  h2 { color: #3730a3; border-left: 5px solid #6366f1; padding-left: 12px; margin-top: 30px; }
  h3 { color: #4f46e5; margin-top: 20px; }
  table { border-collapse: collapse; width: 100%; margin: 15px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
  th { background: #4361ee; color: white; padding: 12px 15px; text-align: left; }
  td { padding: 10px 15px; border: 1px solid #e0e0e0; }
  tr:nth-child(even) { background: #f8f9ff; }
  tr:hover { background: #eef0ff; }
  ul { padding-left: 20px; }
  li { margin: 6px 0; }
  code { background: #f1f3ff; padding: 2px 6px; border-radius: 4px; font-family: "Consolas", monospace; color: #4338ca; }
  pre { background: #1e1e2e; color: #cdd6f4; padding: 15px; border-radius: 8px; overflow-x: auto; font-size: 14px; }
  hr { border: none; border-top: 2px solid #e0e0e0; margin: 30px 0; }
  .section-header { background: linear-gradient(135deg, #4361ee, #7c3aed); color: white; padding: 15px 20px; border-radius: 8px; margin: 30px 0 10px 0; font-size: 1.3em; font-weight: bold; }
</style>
</head>
<body>

<h2 class="section-header">高频计算模型</h2>

<h3>常考题型</h3>
<ul>
  <li>有理数运算</li>
  <li>整式化简</li>
  <li>因式分解</li>
  <li>解方程</li>
  <li>比例应用</li>
</ul>

<h3>核心方法</h3>
<ul>
  <li>运算法则</li>
  <li>配方法</li>
  <li>换元法</li>
</ul>

<h3>知识点表格</h3>

<table>
  <thead>
    <tr>
      <th>知识点</th>
      <th>年级</th>
      <th>LaTeX公式</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>小数乘除法</td><td>五年级</td><td>$a \\times b$，$a \\div b$</td></tr>
    <tr><td>分数乘除法</td><td>六年级</td><td>$\\frac{a}{b} \\cdot \\frac{c}{d} = \\frac{ac}{bd}$，$\\frac{a}{b} \\div \\frac{c}{d} = \\frac{a}{b} \\cdot \\frac{d}{c}$</td></tr>
    <tr><td>百分数</td><td>六年级</td><td>$p\\% = \\frac{p}{100}$</td></tr>
    <tr><td>比和比例</td><td>六年级</td><td>$a:b = c:d \\Rightarrow ad = bc$</td></tr>
    <tr><td>正负数</td><td>初一</td><td>$+a$，$-a$</td></tr>
    <tr><td>绝对值</td><td>初一</td><td>$\\|a\\|$</td></tr>
    <tr><td>数轴</td><td>初一</td><td>数轴上的点与实数一一对应</td></tr>
    <tr><td>相反数</td><td>初一</td><td>$-a$ 是 $a$ 的相反数</td></tr>
    <tr><td>有理数运算</td><td>初一</td><td>$a+b$，$a-b$，$a \\times b$，$a \\div b$</td></tr>
    <tr><td>幂运算</td><td>初一</td><td>$a^m \\cdot a^n = a^{m+n}$，$(a^m)^n = a^{mn}$</td></tr>
    <tr><td>整式加减</td><td>初一</td><td>合并同类项：$ax+bx = (a+b)x$</td></tr>
    <tr><td>乘法公式</td><td>初二</td><td>$(a+b)^2 = a^2+2ab+b^2$，$(a-b)(a+b)=a^2-b^2$</td></tr>
    <tr><td>因式分解</td><td>初二</td><td>$a^2-b^2=(a+b)(a-b)$，$a^2+2ab+b^2=(a+b)^2$</td></tr>
    <tr><td>一元一次方程</td><td>初一</td><td>$ax+b=0 \\quad (a \\neq 0)$</td></tr>
    <tr><td>二元一次方程组</td><td>初一</td><td>$\\begin{cases} a_1x+b_1y=c_1 \\\\ a_2x+b_2y=c_2 \\end{cases}$</td></tr>
    <tr><td>一元二次方程</td><td>初三</td><td>$ax^2+bx+c=0 \\quad (a \\neq 0)$，求根公式 $x=\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}$</td></tr>
  </tbody>
</table>

<h2 class="section-header">几何证明模型</h2>

<h3>常考题型</h3>
<ul>
  <li>全等证明</li>
  <li>等腰三角形性质</li>
  <li>平行四边形判定</li>
  <li>圆综合</li>
  <li>勾股定理应用</li>
  <li>解直角三角形</li>
</ul>

<h3>核心方法</h3>
<ul>
  <li>辅助线</li>
  <li>截长补短</li>
  <li>旋转平移</li>
  <li>面积法</li>
</ul>

<h3>知识点表格</h3>

<table>
  <thead>
    <tr>
      <th>知识点</th>
      <th>年级</th>
      <th>LaTeX公式</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>全等三角形</td><td>初二</td><td>$\\triangle ABC \\cong \\triangle DEF$（$SSS$，$SAS$，$ASA$，$AAS$，$HL$）</td></tr>
    <tr><td>轴对称</td><td>初二</td><td>点 $P(a,b)$ 关于 $x$ 轴对称：$(a,-b)$；关于 $y$ 轴对称：$(-a,b)$</td></tr>
    <tr><td>等腰三角形</td><td>初二</td><td>$AB=AC$，等边对等角，三线合一</td></tr>
    <tr><td>平行四边形</td><td>初二</td><td>$AB\\parallel CD$，$AD\\parallel BC$；对角线互相平分</td></tr>
    <tr><td>圆</td><td>初三</td><td>$\\odot O$，圆周长 $C=2\\pi r$，圆面积 $S=\\pi r^2$</td></tr>
    <tr><td>勾股定理</td><td>初二</td><td>$a^2+b^2=c^2$（直角三角形）</td></tr>
    <tr><td>锐角三角函数</td><td>初三</td><td>$\\sin A = \\frac{\\mathrm{对边}}{\\mathrm{斜边}}$，$\\cos A = \\frac{\\mathrm{邻边}}{\\mathrm{斜边}}$，$\\tan A = \\frac{\\mathrm{对边}}{\\mathrm{邻边}}$</td></tr>
  </tbody>
</table>

<h2 class="section-header">函数与导数综合</h2>

<h3>常考题型</h3>
<ul>
  <li>函数图像性质</li>
  <li>导数几何意义</li>
  <li>单调性极值</li>
  <li>数列求和</li>
</ul>

<h3>核心方法</h3>
<ul>
  <li>数形结合</li>
  <li>分类讨论</li>
  <li>构造新函数</li>
  <li>错位相减</li>
</ul>

<h3>知识点表格</h3>

<table>
  <thead>
    <tr>
      <th>知识点</th>
      <th>年级</th>
      <th>LaTeX公式</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>一次函数</td><td>初二</td><td>$y=kx+b \\quad (k \\neq 0)$</td></tr>
    <tr><td>二次函数</td><td>初三</td><td>$y=ax^2+bx+c \\quad (a \\neq 0)$，顶点 $(-\\frac{b}{2a}, \\frac{4ac-b^2}{4a})$</td></tr>
    <tr><td>反比例函数</td><td>初二</td><td>$y=\\frac{k}{x} \\quad (k \\neq 0)$</td></tr>
    <tr><td>导数</td><td>高中</td><td>$f'(x) = \\lim_{\\Delta x \\to 0} \\frac{f(x+\\Delta x)-f(x)}{\\Delta x}$</td></tr>
    <tr><td>数列</td><td>高中</td><td>$\\{a_n\\}$，通项公式 $a_n$</td></tr>
    <tr><td>等差数列</td><td>高中</td><td>$a_n = a_1+(n-1)d$，前 $n$ 项和 $S_n = \\frac{n(a_1+a_n)}{2}$</td></tr>
    <tr><td>等比数列</td><td>高中</td><td>$a_n = a_1 q^{n-1}$，前 $n$ 项和 $S_n = \\frac{a_1(1-q^n)}{1-q} \\ (q \\neq 1)$</td></tr>
  </tbody>
</table>

<h2 class="section-header">解析几何综合</h2>

<h3>常考题型</h3>
<ul>
  <li>轨迹方程</li>
  <li>圆锥曲线位置关系</li>
  <li>定点定值</li>
  <li>最值范围</li>
</ul>

<h3>核心方法</h3>
<ul>
  <li>坐标法</li>
  <li>韦达定理</li>
  <li>设而不求</li>
  <li>向量法</li>
</ul>

<h3>知识点表格</h3>

<table>
  <thead>
    <tr>
      <th>知识点</th>
      <th>年级</th>
      <th>LaTeX公式</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>空间向量</td><td>高中</td><td>$\\vec{a}=(x,y,z)$，数量积 $\\vec{a} \\cdot \\vec{b} = x_1x_2+y_1y_2+z_1z_2$</td></tr>
    <tr><td>圆锥曲线</td><td>高中</td><td>椭圆：$\\frac{x^2}{a^2}+\\frac{y^2}{b^2}=1$；双曲线：$\\frac{x^2}{a^2}-\\frac{y^2}{b^2}=1$；抛物线：$y^2=2px$</td></tr>
    <tr><td>一元二次方程（用于联立）</td><td>初三</td><td>$ax^2+bx+c=0$，判别式 $\\Delta = b^2-4ac$，韦达定理 $x_1+x_2=-\\frac{b}{a}$，$x_1x_2=\\frac{c}{a}$</td></tr>
  </tbody>
</table>

<h2 class="section-header">概率统计综合</h2>
<h3>常考题型</h3>
<ul>
  <li>古典概型</li>
  <li>条件概率</li>
  <li>随机变量分布列</li>
  <li>二项分布</li>
  <li>正态分布应用</li>
</ul>

<h3>核心方法</h3>
<ul>
  <li>列举法</li>
  <li>树状图</li>
  <li>独立重复试验</li>
  <li>标准化</li>
</ul>

<h3>知识点表格</h3>
<table>
  <thead>
    <tr>
      <th>知识点</th>
      <th>年级</th>
      <th>LaTeX公式</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>概率初步</td><td>初三</td><td>$P(A) = \\frac{n(A)}{n(\\Omega)}$（古典概型）</td></tr>
    <tr><td>排列组合</td><td>高中</td><td>$A_n^m = n(n-1)\\cdots(n-m+1)$，$C_n^m = \\frac{n!}{m!(n-m)!}$</td></tr>
    <tr><td>二项分布</td><td>高中</td><td>$P(X=k) = C_n^k p^k (1-p)^{n-k} \\quad (k=0,1,\\dots,n)$</td></tr>
    <tr><td>正态分布</td><td>高中</td><td>$f(x) = \\frac{1}{\\sqrt{2\\pi}\\sigma} e^{-\\frac{(x-\\mu)^2}{2\\sigma^2}}$，$X \\sim N(\\mu,\\sigma^2)$</td></tr>
    <tr><td>数学建模</td><td>高中</td><td>建立实际问题的数学模型，优化与决策</td></tr>
  </tbody>
</table>

</body>
</html>'''

with open("/projects/math-workspace/dim4-formula.html", "w") as f:
    f.write(ORIGINAL)

print(f"Restored complete dim4 HTML: {len(ORIGINAL)} bytes")
print("Key fixes applied:")
print("  - \\text{...} → \\mathrm{...} in trig functions")
print("  - \\\\ for cases environment newlines")
print("  - All 5 sections + 5 tables complete")

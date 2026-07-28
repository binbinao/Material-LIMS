#!/bin/bash

# setup-hooks.sh — Material LIMS Git钩子安装脚本
# 功能：安装pre-commit钩子，集成java-reviewer进行本地代码审查

set -e

echo "🔧 开始安装Git钩子..."

# 检查是否在Git仓库中
if [ ! -d .git ]; then
    echo "❌ 错误：当前目录不是Git仓库根目录"
    exit 1
fi

# 创建hooks目录
HOOKS_DIR=".git/hooks"
if [ ! -d "$HOOKS_DIR" ]; then
    mkdir -p "$HOOKS_DIR"
fi

# 创建pre-commit钩子
cat > "$HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash

# Material LIMS pre-commit钩子
# 功能：在提交前运行java-reviewer进行代码审查

echo "🔍 pre-commit: 运行Java代码审查..."

# 检查是否有Java文件变更
JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.java$')

if [ -z "$JAVA_FILES" ]; then
    echo "✅ 没有Java文件变更，跳过审查"
    exit 0
fi

echo "📄 检测到Java文件变更:"
echo "$JAVA_FILES"
echo ""

# 运行java-reviewer进行审查
echo "🚀 启动java-reviewer审查..."
if ! node scripts/java-reviewer.mjs --strict; then
    echo ""
    echo "❌ 代码审查失败！发现ERROR级别问题"
    echo ""
    echo "💡 建议操作:"
    echo "   1. 运行 'node scripts/java-reviewer.mjs' 查看详细问题"
    echo "   2. 运行 'node scripts/java-reviewer.mjs --fix-only' 自动修复问题"
    echo "   3. 运行 'git add .' 添加修复后的文件"
    echo "   4. 重新提交"
    echo ""
    echo "⚠️  如需强制提交，请使用 'git commit --no-verify'"
    exit 1
fi

echo "✅ Java代码审查通过，允许提交"
exit 0
EOF

# 创建pre-push钩子（可选）
cat > "$HOOKS_DIR/pre-push" << 'EOF'
#!/bin/bash

# Material LIMS pre-push钩子
# 功能：在推送前进行最终审查

echo "🔍 pre-push: 最终代码审查..."

# 检查是否有Java文件变更
JAVA_FILES=$(git diff origin/$(git rev-parse --abbrev-ref HEAD)..HEAD --name-only --diff-filter=ACM | grep -E '\.java$')

if [ -z "$JAVA_FILES" ]; then
    echo "✅ 没有Java文件变更，跳过最终审查"
    exit 0
fi

echo "📄 检测到待推送的Java文件:"
echo "$JAVA_FILES"
echo ""

# 运行java-reviewer进行最终审查
echo "🚀 启动最终审查..."
if ! node scripts/java-reviewer.mjs --strict; then
    echo ""
    echo "❌ 最终审查失败！发现ERROR级别问题"
    echo ""
    echo "💡 建议操作:"
    echo "   1. 运行 'node scripts/java-reviewer.mjs' 查看详细问题"
    echo "   2. 运行 'node scripts/java-reviewer.mjs --fix-only' 自动修复问题"
    echo "   3. 运行 'git add .' 添加修复后的文件"
    echo "   4. 重新提交并推送"
    echo ""
    echo "⚠️  如需强制推送，请使用 'git push --no-verify'"
    exit 1
fi

echo "✅ 最终审查通过，允许推送"
exit 0
EOF

# 设置钩子可执行权限
chmod +x "$HOOKS_DIR/pre-commit"
chmod +x "$HOOKS_DIR/pre-push"

echo "✅ Git钩子安装完成！"
echo ""
echo "📋 已安装的钩子:"
echo "   - pre-commit: 提交前自动审查Java代码"
echo "   - pre-push:  推送前最终审查（可选）"
echo ""
echo "💡 使用说明:"
echo "   1. 每次git commit都会自动运行java-reviewer"
echo "   2. 发现ERROR级别问题会阻断提交"
echo "   3. 使用 'git commit --no-verify' 可跳过审查"
echo "   4. 使用 'git push --no-verify' 可跳过推送审查"
echo ""
echo "🔧 测试钩子:"
echo "   git add . && git commit -m 'test: 测试钩子功能'"

# 创建卸载脚本
cat > "scripts/uninstall-hooks.sh" << 'EOF'
#!/bin/bash

# uninstall-hooks.sh — 卸载Git钩子

echo "🗑️  开始卸载Git钩子..."

rm -f .git/hooks/pre-commit
rm -f .git/hooks/pre-push

echo "✅ Git钩子已卸载"
echo "💡 现在可以自由提交和推送，无需代码审查"
EOF

chmod +x "scripts/uninstall-hooks.sh"

echo "📁 创建了卸载脚本: scripts/uninstall-hooks.sh"
echo ""
echo "🎉 安装完成！现在开始享受自动代码审查吧！"
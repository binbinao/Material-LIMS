#!/bin/bash

# uninstall-hooks.sh — 卸载Git钩子

echo "🗑️  开始卸载Git钩子..."

rm -f .git/hooks/pre-commit
rm -f .git/hooks/pre-push

echo "✅ Git钩子已卸载"
echo "💡 现在可以自由提交和推送，无需代码审查"

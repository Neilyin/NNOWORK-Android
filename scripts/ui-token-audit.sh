#!/bin/sh
# UI token 收斂檢查：超過門檻就以非 0 結束。用法：scripts/ui-token-audit.sh
cd "$(dirname "$0")/../app/src/main/java/com/neilyin/nnowork" || exit 2
pc=$(grep -rE 'PixelColors\.' feature navigation | wc -l | tr -d ' ')
c0x=$(grep -rE 'Color\(0x' . | grep -v '^\./ui/theme\|^ui/theme' | wc -l | tr -d ' ')
sp=$(grep -rE '\.sp\b' . | grep -v '^\./ui/theme\|^ui/theme' | wc -l | tr -d ' ')
echo "PixelColors. in feature+navigation: $pc (<=20)"
echo "Color(0x outside ui/theme:          $c0x (<=5)"
echo ".sp outside ui/theme:               $sp (<=30)"
[ "$pc" -le 20 ] && [ "$c0x" -le 5 ] && [ "$sp" -le 30 ]

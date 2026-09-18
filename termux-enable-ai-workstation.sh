#!/data/data/com.termux/files/usr/bin/bash
set -e
mkdir -p "$HOME/.termux"
if grep -q '^allow-external-apps=' "$HOME/.termux/termux.properties" 2>/dev/null; then
  sed -i 's/^allow-external-apps=.*/allow-external-apps=true/' "$HOME/.termux/termux.properties"
else
  printf '\nallow-external-apps=true\n' >> "$HOME/.termux/termux.properties"
fi
echo "Termux external-app access enabled."
echo "Restart Termux once, then AI Workstation can use RUN_COMMAND IPC."

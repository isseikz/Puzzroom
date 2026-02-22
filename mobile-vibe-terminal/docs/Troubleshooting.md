# Troubleshooting Guide

This document records common issues and their solutions for the Mobile Vibe Terminal.

## UI & Navigation

### Mouse Scrolling not working in Byobu/Tmux

**Issue**: 
When connected to a server running `byobu` (or raw `tmux`), swipe gestures intended for scrolling do not work, and the terminal does not enter copy-mode.

**Cause**: 
By default, `tmux` (the backend for byobu) often has mouse support disabled globally. Even if the terminal emulator (this app) supports SGR mouse reporting, the server will ignore the sequences if it hasn't explicitly requested them.

**Diagnosis**:
Run the following command inside your byobu/tmux session:
```bash
tmux show -g mouse
```
If the output is `mouse off`, mouse reporting is disabled.

**Solution**:
Enable mouse support globally in your tmux configuration or session:
1.  **Immediate Fix (Current Session)**:
    Run this command in the terminal:
    ```bash
    tmux set -g mouse on
    ```
2.  **Permanent Fix**:
    Add the following line to your `~/.tmux.conf` file:
    ```
    set -g mouse on
    ```
3.  **Byobu Menu**:
    Press `F9`, navigate to `Change byobu settings`, and select `Enable mouse support`.

**Verification**:
After enabling, run `cat` in the terminal and scroll. You should see escape sequences like `^[[<64;...M` appearing if the app is correctly reporting gestures. In byobu, scrolling should now enter "Copy Mode" and allow you to scroll through history.

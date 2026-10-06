"""Exercise navigation on the R8 optimized APK installed by CI."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.kegel_coach.myapp"

def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)

def screen():
    adb("shell", "uiautomator", "dump", "/sdcard/window.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/window.xml"))

def tap(label):
    nodes = list(screen().iter("node"))
    node = next((n for n in nodes if n.get("text") == label or n.get("content-desc") == label), None)
    assert node is not None, f"Missing navigation: {label}: {ET.tostring(screen(), encoding='unicode')}"
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(2)
    assert adb("shell", "pidof", PACKAGE).strip(), f"App exited after {label}"

adb("logcat", "-c")
adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(5)
for label in ["Continuar", "Continuar", "Continuar", "COMEÇAR"]:
    tap(label)
for label in ["Treinos", "Progresso", "Ajustes", "Início"]:
    tap(label)
logs = adb("logcat", "-d", "-b", "crash")
assert "FATAL EXCEPTION" not in logs, logs
print("Optimized release navigation PASS")

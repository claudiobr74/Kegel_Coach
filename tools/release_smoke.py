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
    node = None
    for attempt in range(8):
        root = screen()
        nodes = list(root.iter("node"))
        node = next((n for n in nodes if n.get("text") == label or n.get("content-desc") == label), None)
        if node is not None:
            break
        adb("shell", "input", "swipe", "160", "480", "160", "200", "350")
        time.sleep(1)
    assert node is not None, f"Missing navigation: {label}: {ET.tostring(screen(), encoding='unicode')}"
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(2)
    assert adb("shell", "pidof", PACKAGE).strip(), f"App exited after {label}"

adb("shell", "pm", "grant", PACKAGE, "android.permission.POST_NOTIFICATIONS")
adb("logcat", "-c")
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/com.pausa.MainActivity")
time.sleep(5)
for label in ["Continuar", "Continuar", "Continuar", "COMEÇAR"]:
    tap(label)
for label in ["Treinos", "Progresso", "Ajustes", "Início"]:
    tap(label)
# Save chosen days and confirm them after reopening the optimized app.
for label in ["Ajustes", "Lembretes", "+ Adicionar horário", "Sábado e domingo", "Salvar"]:
    tap(label)
assert any(n.get("text") == "SÁB · DOM" for n in screen().iter("node"))
assert any(n.get("text") == "08:00" for n in screen().iter("node"))
for label in ["Editar horário e dias", "Dia SEG", "Cancelar"]:
    tap(label)
assert any(n.get("text") == "SÁB · DOM" for n in screen().iter("node"))
assert any(n.get("text") == "08:00" for n in screen().iter("node"))
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/com.pausa.MainActivity")
time.sleep(5)
for label in ["Ajustes", "Lembretes"]:
    tap(label)
assert any(n.get("text") == "SÁB · DOM" for n in screen().iter("node"))
assert any(n.get("text") == "08:00" for n in screen().iter("node"))
tap("Remover")
assert not any(n.get("text") == "SÁB · DOM" for n in screen().iter("node"))
logs = adb("logcat", "-d", "-b", "crash")
assert "FATAL EXCEPTION" not in logs, logs
print("Optimized release navigation PASS")

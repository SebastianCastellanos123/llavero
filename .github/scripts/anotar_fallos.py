"""Convierte cada prueba fallida de los reportes JUnit en una anotación de GitHub Actions.

Así el motivo del fallo aparece en el resumen del CI sin tener que descargar
los reportes. Uso: python anotar_fallos.py <carpeta con XML de JUnit>
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def escape(text: str) -> str:
    # Formato de comandos de GitHub Actions: %, saltos de línea y retornos se codifican.
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def main() -> int:
    root = Path(sys.argv[1])
    failures = 0
    for report in sorted(root.rglob("*.xml")):
        for case in ET.parse(report).getroot().iter("testcase"):
            for problem in list(case.findall("failure")) + list(case.findall("error")):
                failures += 1
                name = f"{case.get('classname', '')}.{case.get('name', '')}"
                detail = (problem.get("message") or problem.text or "sin mensaje").strip()
                print(f"::error title={escape(name)}::{escape(detail[:3000])}")
    print(f"{failures} prueba(s) fallida(s) en {root}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

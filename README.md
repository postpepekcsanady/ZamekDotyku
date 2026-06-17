# Zámek dotyku

Česká jednoduchá Android aplikace pro zamčení dotyku přes ostatní aplikace.

## Co umí

- Vyžádá oprávnění „Zobrazovat přes jiné aplikace“.
- Zobrazí malé plovoucí tlačítko `ZAMKNI`.
- Po klepnutí zakryje obrazovku průhlednou vrstvou, která zachytává dotyky.
- Odemčení je schválně pomalejší: podržet tlačítko 3 sekundy.

## Omezení Androidu

Android z bezpečnostních důvodů vyžaduje oprávnění `SYSTEM_ALERT_WINDOW`. Systémová tlačítka, gesto Domů, vypínač nebo některé systémové obrazovky můžou fungovat dál podle verze telefonu.

## Sestavení APK

Na tomto počítači je připravené JDK 17 a Android SDK. APK sestaví:

```powershell
.\build.ps1
```

Výstup:

```text
build\outputs\ZamekDotyku-debug.apk
```

Instalaci do telefonu popisuje soubor `INSTALACE.md`.

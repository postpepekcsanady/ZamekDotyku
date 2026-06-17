# Zámek dotyku

Česká jednoduchá Android aplikace pro zamčení dotyku přes ostatní aplikace.

## Co umí

- Vyžádá oprávnění „Zobrazovat přes jiné aplikace“.
- Zobrazí malé plovoucí tlačítko `ZÁMEK`.
- Po klepnutí zakryje obrazovku jemnou průhlednou vrstvou, aby pohádka zůstala vidět.
- Zachytává dotyky přes video, YouTube nebo jinou aplikaci.
- Snaží se schovat systémovou navigaci pomocí režimu přes celou obrazovku.
- Odemčení je záměrně delší: podržet tlačítko 3, 5 nebo 8 sekund.

## Proč je to takhle

Aplikace je dělaná hlavně pro situaci, kdy dítě kouká na pohádku a nechcete, aby omylem pauzovalo, přepínalo nebo zavíralo video.

## Omezení Androidu

Android z bezpečnostních důvodů nedovolí běžné aplikaci stoprocentně zamknout celý telefon. Vypínač, gesto Domů, některá systémová tlačítka nebo horní lišta můžou podle telefonu pořád fungovat. Dotyky přímo do pohádky by ale zámek zachytit měl.

## Sestavení APK

Na tomto počítači je připravené JDK 17 a Android SDK. APK sestaví:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

Výstup:

```text
build\outputs\ZamekDotyku-debug.apk
```

Instalaci do telefonu popisuje soubor `INSTALACE.md`.

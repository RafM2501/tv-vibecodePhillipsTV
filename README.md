# Philips Clean Home

Eigen lichte Android TV-launcher voor de Philips **55PUS8897/12**
(Android TV 11, MediaTek MT5895, platform PH1M_EA_9970A).

Deze code staat op de branch **philips-clean-home**. De originele
main-branch en de standaard Philips Android TV-launcher blijven onaangeroerd.

## Jouw acht apps

1. TV Vlaanderen — nl.streamgroup.vlaanderen
2. Prime Video — com.amazon.amazonvideo.livingroom
3. GoPlay — be.goplay.app.tv (fallback: be.goplay.app)
4. VTM GO — be.vmma.vtm.zenderapp
5. VRT MAX — be.vrt.vrtnu
6. DAZN — com.dazn
7. IBO Pro Player — com.ibopro.player
8. Stremio — com.stremio.one

Twee rijen van vier tegels, D-padbediening, klok, Instellingen en app-iconen
uit de geïnstalleerde apps. Geen reclame, aanbevelingen, HDMI-switching,
telemetrie, tracking, boot receivers of Accessibility-service.

## Permanente APK-ondertekening (eenmalig)

Eerdere debug builds kregen op iedere GitHub runner een andere handtekening.
Nu publiceert de workflow alleen een APK wanneer de permanente,
PERSOONLIJKE ondertekeningssleutel veilig via GitHub Actions Secrets aanwezig is.

1. Installeer JDK 17 op je Windows-pc zodat het keytool-commando werkt.
2. Download philips/setup-signing.ps1 en voer dat script uit in PowerShell.
3. Kies je eigen keystorewachtwoord. De sleutel wordt lokaal opgeslagen in
   %USERPROFILE%\PhilipsCleanHomeSigning\philips-clean-home.p12 .
4. De Base64-keystore staat op je klembord. Maak via
   https://github.com/RafM2501/tv-vibecodePhillipsTV/settings/secrets/actions
   de Actions repository secret PHILIPS_KEYSTORE_BASE64 aan en plak de inhoud.
5. Maak een tweede repository secret PHILIPS_SIGNING_PASSWORD en vul het
   wachtwoord in dat je lokaal bij keytool koos.
6. Bewaar bestand en wachtwoord op een veilige plek buiten GitHub en wis het klembord.
   Zonder deze sleutel zijn updates van dezelfde app niet mogelijk.
7. Actions > Philips Clean Home APK > Run workflow > philips-clean-home.
   Na succes bevat de artifact philips-clean-home-STABLE-signed-apk
   het bestand app-release.apk.

Zolang de secrets ontbreken, controleert de CI alleen of het project bouwt.
Er verschijnt dan GEEN downloadbare APK. Er wordt nooit opnieuw een tijdelijke
handtekening gebruikt voor een gepubliceerde release.

## Eenmalige overgang van de bestaande debug-versie

LET OP: de originele Google TV-launcher is momenteel op je televisie
uitgeschakeld. Schakel die EERST via ADB weer in om een veilige Home-fallback
te houden, VOOR het verwijderen van de bestaande Clean Home APK.

Volg in deze volgorde op je Windows-pc (in de platform-tools map):

    .\adb.exe shell pm enable --user 0 com.google.android.tvlauncher
    .\adb.exe shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
    .\adb.exe uninstall be.philips.cleanhome
    .\adb.exe install .\app-release.apk
    .\adb.exe shell am start -n be.philips.cleanhome/.MainActivity

Controleer dat de nieuwe launcher werkt. Pas daarna:

    .\adb.exe shell cmd package set-home-activity --user 0 be.philips.cleanhome/.MainActivity
    .\adb.exe shell pm disable-user --user 0 com.google.android.tvlauncher

Voortaan worden updates met dezelfde handtekening simpel:

    .\adb.exe install -r .\app-release.apk

Verhoog bij een nieuwe versie steeds versionCode in launcher/app/build.gradle.

## Herkomst

Eigen herimplementatie in de MIT-licensed fork van
[ToomRamcharan/tv-vibehack](https://github.com/ToomRamcharan/tv-vibehack).
De upstream MIT-LICENSE is behouden. Op de originele main-branch staat de
ongewijzigde oorspronkelijke code.

## De launcher v0.5
- Warm, rustig en huiselijk ontworpen Android TV-startscherm.
- Acht favoriete apps op het Home-scherm.
- **App Store** linksboven; **Alle apps** en **Instellingen** rechtsboven.
- **Alle apps** toont een enkele lijst van alle rechtstreeks startbare,
  herkenbare TV-apps: Play Store, APK en optionele voorgeïnstalleerde apps.
  Geen categorieën of uitlegteksten, geen technische systeemservices.
- Iedere apptegel in **Alle apps** heeft zichtbaar de actie **Verwijderen**
  voor gewone apps of **App-info** voor systeemapps.
- Gewone apps worden via de Android-systeemdialoog verwijderd (bevestiging
  vereist); als de dialoog niet bestaat, wordt Android App-info geopend.
- Systeemapps kunnen niet stilzwijgend worden verwijderd door een normale
  Android-app. Voor optionele bloatware is de aparte, omkeerbare ADB-tool
  `philips/optional-system-apps.ps1` beschikbaar.
- Houd OK lang ingedrukt om het app-menu te tonen; de zichtbare actie onder
  een tegel werkt ook zonder lang indrukken.

Deze versie verwijdert of schakelt nooit rechtstreeks Philips- of Android-
infrastructuur uit. De originele launcher blijft beschikbaar.

Versie 0.5.0 — Android-app werkt alleen voor zover op de TV getest.

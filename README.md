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

## APK bouwen via GitHub Actions

1. Open **Actions** in deze GitHub-repository. Schakel Actions voor de fork in als
   GitHub daarom vraagt.
2. Kies **Philips Clean Home APK** en open de nieuwste run.
3. Is de run niet automatisch gestart? Klik op **Run workflow** en selecteer
   de branch **philips-clean-home**.
4. Na een geslaagde build: download onder **Artifacts** het ZIP-bestand
   **philips-clean-home-debug-apk**.
5. Pak het ZIP-bestand uit. Je vindt daarin **app-debug.apk**.

Alternatief: op een pc met Java 17 en Android SDK Platform 34:
\`\`\`powershell
cd launcher
.\gradlew.bat assembleDebug
\`\`\`

## Veilig testen op je Philips

Zorg dat ADB via je eigen netwerk is verbonden. Gebruik op Windows PowerShell:

\`\`\`powershell
.\adb.exe devices
.\adb.exe install -r "C:\pad\naar\app-debug.apk"
.\adb.exe shell am start -n be.philips.cleanhome/.MainActivity
\`\`\`

Deze stappen installeren en openen alleen een gewone app. Ze veranderen
**niet** de standaard Home-app en schakelen geen systeemcomponenten uit.

Test alle acht tegels, de afstandsbediening, Instellingen, Stremio en Ambilight.
Als iets niet werkt, herstel je uitsluitend onze testapp:

\`\`\`powershell
.\adb.exe uninstall be.philips.cleanhome
\`\`\`

**Nog niet uitvoeren:** \`pm disable-user\` op de originele launcher,
\`cmd package set-home-activity\`, scripts uit de upstream-repo of
wijzigingen aan Accessibility-services. Eerst de APK op de TV testen.

## Herkomst

Eigen herimplementatie in de MIT-licensed fork van
[ToomRamcharan/tv-vibehack](https://github.com/ToomRamcharan/tv-vibehack).
De upstream MIT-LICENSE is behouden. Op de originele main-branch staat de
ongewijzigde oorspronkelijke code.

## Alle apps beheren (v0.3)
- Open **Alle apps** en schakel tussen **Apps**, **Systeemapps** en **Alles**.
- Ook geïnstalleerde pakketten zonder Home/Leanback-startactiviteit verschijnen.
- **OK** opent een app; bij apps zonder startactiviteit opent **App-info**.
- **OK lang indrukken** opent het beheermenu.
- Voor normaal geïnstalleerde apps start **Verwijderen** de systeemdialoog;
  Android vraagt altijd expliciete bevestiging.
- Systeemapps worden nooit via deze launcher verwijderd; gebruik
  **App-info** alleen als je begrijpt welke TV-functies nodig zijn.

LET OP: Android TV kan geen verwijderverzoek uitvoeren voor bepaalde
pakkettypen. Philips-firmware kan systeemfuncties beschermen. Onze launcher
schakelt geen systeempakketten uit en verwijdert niets zonder bevestiging.

Status: versie 0.3.0; installatie en verwijderen moeten nog op de echte TV
worden getest.

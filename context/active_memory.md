# 🧠 Projekt-Gedächtnis & Status (Active Memory)

**Projekt:** Sabo-Bewerbungshelfer  
**Stand:** 13. November 2026, 01:45 Uhr  
**Nutzer / Maintainer:** Markus Jansen (`janzock@googlemail.com`)  
**GitHub Repository:** [https://github.com/janzock-cell/job-Bewerbung](https://github.com/janzock-cell/job-Bewerbung)  
**Netlify Projekt:** `sabo-bewerbung` (ID: `171cece2-6dbc-496a-959c-a228e685cc08`)  
**Live Production URL:** [https://sabo-bewerbung.netlify.app](https://sabo-bewerbung.netlify.app)  

---

## 1. Architektur & Dual-Projekt-Struktur
Das Repository enthält zwei Komponenten:
1. **Android App (Google AI Studio Export):**
   - Befindet sich im Verzeichnis `app/` (Jetpack Compose, Kotlin, Material 3, ViewModel-Architektur).
   - Screens: `HomeScreen.kt`, `VaultScreen.kt`, `ScanScreen.kt`, `EditorScreen.kt`.
2. **Web-App & PWA (Produktiv-Deployment auf Netlify):**
   - `index.html`: Vollständige Single-Page Application im 100% Dark-Mode (Tailwind CSS, Lucide Icons, Tesseract.js, html2pdf.js).
   - `netlify/functions/gemini.js`: Serverless Proxy zur sicheren Ausführung von Gemini-API-Calls (optional über Netlify-Umgebungsvariable `GEMINI_API_KEY` oder direkten Client-Schlüssel).
   - `manifest.webmanifest` & `sw.js`: Progressive Web App (PWA) für Offline-Caching und Homescreen-Installation.

---

## 2. Aktueller Umsetzungsstand & Features
- **Design-Parität:** Die Web-App bildet die Android-Screens aus Google AI Studio pixelgenau im Dark-Mode nach:
  - **Home Dashboard:** Hero-Banner mit Schnellstart-Kacheln (Tresor, Notizen-Scan, Anschreiben, Verlauf).
  - **1. Daten-Tresor:** Schritt-1-Upload-Box (Kamera, Foto, PDF/Datei), strukturierte Datenextraktion, ausklappbare Abschnitte für persönliche Daten, Berufserfahrung und Fähigkeiten.
  - **Notizen-Scan:** Kamera- & Galerie-Auswahl mit Tesseract OCR und Hinweisen zur Handschrift-Erkennung.
  - **2. Anschreiben-Generator:** 4-Schritte-Wizard (Jobdaten -> Notizen -> KI-Generierung -> DIN-5008-Vorschau mit Falt-/Lochmarken und direktem PDF-Export).
- **Behobene Fehler & Diagnostics:**
  - Gemini Model-Endpoint auf `gemini-2.0-flash` korrigiert (in `index.html` und `ApplicationViewModel.kt`).
  - DIN 5008 Grußformel ohne nachfolgendes Komma standardkonform umgesetzt.
  - Print-CSS repariert (weißes Blatt wird beim Drucken und PDF-Export sauber gerendert).
  - Alle Webhint- und Accessibility-Warnungen behoben (`apple-touch-icon`, `input[capture]`-Bereinigung, ARIA-Labels).
  - Temporäre Gradle-Reports (`build/`) bereinigt.

---

## 3. Git & Netlify Workflow
- **Git Branch:** `main` (sauber, synchron mit `origin/main`).
- **Netlify Besonderheit:** Produktions-Deployments waren im Netlify-Konto gelockt.
  - Neuer Release-Workflow:
    1. Code committen & pushen: `git push origin main`
    2. Build triggern: `netlify deploy --trigger`
    3. Produktions-Release freigeben: `netlify api restoreSiteDeploy --data '{"site_id": "<id>", "deploy_id": "<new_deploy_id>"}'`
- Letzter verifizierter Deploy: `6af65af4a20ee15ceb702083` (HTTP 200 OK).

---

## 4. Offene Punkte für die nächste Session
- Markus testet die Workflows und KI-Generierungen live durch.
- Evtl. Feinschliff an den Extraktions-Prompts, Validierung von Tesseract OCR auf mobilen Browsern oder Ergänzung von weiteren Dokumententypen im Tresor.

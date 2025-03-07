# Kollaborativer Markdown-Editor

Dieses Repository enthält den Quellcode meines kollaborativen Markdown-Editors.  
Das Projekt ermöglicht es mehreren Nutzern, in Echtzeit gemeinsam an Markdown-Dokumenten zu arbeiten.  

## Überblick  
Der kollaborative Markdown-Editor basiert auf einer Client-Server-Architektur und nutzt WebSockets für die Echtzeitkommunikation.  
Das Ziel des Projekts war es, eine benutzerfreundliche und skalierbare Lösung für gemeinsames Schreiben und Bearbeiten von Markdown-Dokumenten zu schaffen.  

## Funktionen  
- **Echtzeit-Kollaboration**: Änderungen werden sofort für alle Nutzer synchronisiert.  
- **Benutzerkonten-Verwaltung**: Registrierung, Anmeldung und Freundeslisten.  
- **Sitzungssystem**: Nutzer können Sitzungen erstellen, beitreten und Dokumente gemeinsam bearbeiten.  
- **Markdown-Vorschau**: Direktes Anzeigen von Markdown als formatierten Text.  
- **Sicherheitsfunktionen**: Passwortgeschützte Sitzungen und verschlüsselte Speicherung von Zugangsdaten.  

## Verwendete Technologien  
- **Frontend:** Java Swing  
- **Backend:** Java mit Spring Boot  
- **Datenbank:** MongoDB Atlas  
- **Echtzeitkommunikation:** WebSockets  
- **Deployment:** Docker und Render  

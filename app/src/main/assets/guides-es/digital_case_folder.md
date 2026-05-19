# Crear una carpeta digital del caso en su PC

Ya sea en Windows, macOS o Linux, la misma estructura de carpetas hace manejable un caso pro se. La pestaña Notas de Forseti es para pensar; **la carpeta del caso es el expediente oficial**.

## La estructura (cópiela tal cual)

```
~/Cases/
  YYYY-MM-DD_<nombre-corto-del-caso>/
    00_INDEX.md                <- resumen de una línea de cada archivo
    01_pleadings/              <- demanda, contestación, escritos enmendados
    02_motions/
       <fecha>_<quien>_<titulo>.pdf
    03_discovery/
       outgoing/               <- solicitudes que usted envió
       incoming/               <- respuestas que recibió
       privilege_log.xlsx
    04_evidence/
       documents/
       photos/
       audio/
       video/
       hashes.txt              <- SHA-256 de cada archivo (cadena de custodia)
    05_correspondence/
       email/                  <- exportado como .eml o .pdf
       letters/
       text_messages/
    06_court_orders/
    07_briefs_and_exhibits/
       motion_<id>/
          brief.pdf
          ex_A_<nombre-corto>.pdf
          ex_B_<nombre-corto>.pdf
    08_research/                <- casos, estatutos, artículos guardados
    09_billing_and_costs/
    10_archive/                 <- borradores sustituidos; nunca borre
```

## Convención de nombres

`YYYY-MM-DD_<remitente>_<titulo-corto>.<ext>`

> Ejemplo: `2026-05-08_pl_motion-to-compel-rule-37.pdf`

Ordenable, apto para scripts, sin espacios (use guiones).

## Copias de seguridad — regla 3-2-1

- **3** copias de cada archivo.
- En al menos **2** medios distintos (SSD del portátil + disco externo).
- **1** copia fuera del sitio (nube cifrada o disco en casa de un familiar).

Para archivos sensibles del caso, prefiera nube **cifrada de extremo a extremo** (Cryptomator + Dropbox, Tresorit o Proton Drive) antes que Drive/iCloud sin cifrar.

## Seguimiento de quién tiene qué

- Cada correo que envíe: cópia oculta a usted mismo, guarde la exportación en `05_correspondence/email/`.
- Cada correo certificado: escanee la tarjeta verde a `05_correspondence/letters/`.
- Cada confirmación de fax / e-file: PDF en la carpeta correspondiente.

## Versiones de sus borradores

- Use sufijos `_v1`, `_v2`, `_FINAL`, `_FILED`.
- La versión que **se presentó** va en `01_pleadings/`, `02_motions/`, etc. — nunca en `10_archive/`.
- Los borradores anteriores van a `10_archive/`.

## Integridad de archivos

Calcule un hash único de su evidencia para demostrar que no fue alterada:

```sh
# macOS / Linux
find 04_evidence -type f -exec shasum -a 256 {} \; > 04_evidence/hashes.txt
```

```powershell
# Windows PowerShell
Get-ChildItem -Recurse 04_evidence | Get-FileHash -Algorithm SHA256 |
  Export-Csv 04_evidence\hashes.csv
```

## Qué poner en `00_INDEX.md`

Lista con viñetas, un ítem por archivo, con fecha, remitente, título del documento y enlace. Actualícelo cada vez que guarde un archivo nuevo. Su yo del futuro lo agradecerá cuando un juez pregunte "¿dónde produjo exactamente eso?"

> Solo información. No constituye asesoramiento legal.

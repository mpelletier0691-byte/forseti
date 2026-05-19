# Incorporar un caso existente a Forseti

Ya tiene meses de papeles, capturas de pantalla y PDF dispersos en el teléfono, la nube y varias carpetas de correo. Forseti está diseñado para absorber ese desorden y ordenarlo en una estructura clara y repetible.

Esta guía recorre los **botones de incorporación del Perfil de caso** (carpeta e imágenes), explica el **diseño de carpetas Brokkr Forge** y enseña hábitos de nombres de archivo que hacen preciso el enrutamiento automático.

---

## 1. El diseño Brokkr Forge (cómo se ve cada caso)

Cada caso que Forseti crea usa el mismo esqueleto de once carpetas. No tiene que inventarlo — Forseti lo genera al guardar un caso nuevo. La numeración mantiene el orden procesal sin importar el administrador de archivos.

```
Case_001_<su título>/
├── 00_Case_Overview/        Notas, lista de partes, índice del caso
├── 01_Pleadings/            Demanda, Contestación, Mociones, Órdenes
├── 02_Service_of_Process/   Proof_of_Service, Summons, Correspondence
├── 03_Discovery/            Interrogatories, Requests_for_Production,
│                            Admissions, Depositions, Discovery_Responses
├── 04_Evidence/             Photos, PDFs, Screenshots, Audio, Video
├── 05_Motions/              Drafts, Filed, Court_Responses
├── 06_Correspondence/       Opposing_Party, Court, Misc
├── 07_Deadlines/            Entradas de calendario + Completed/
├── 08_Exhibits/             Labels and Final_Exhibits
├── 09_Hearings/             Notices, Prep, Outcomes
├── 10_Trial/                Trial_Brief, Witness_List, Jury_Instructions, Final_Binder
├── 98_Scans/                Bandeja del escáner (sin palabra clave)
└── 99_Inbox/                El enrutador no pudo clasificar
```

Todo lo que caiga en **99_Inbox/** puede moverlo desde **Perfil de caso → Abrir caso**.

---

## 2. Dos botones de incorporación

Abra Perfil de caso, edite (o cree) el caso que desea poblar, desplácese bajo **Demanda presentada**:

| Botón | Úselo cuando… |
| --- | --- |
| **Incorporar carpeta de archivos del caso** | Ya tiene una carpeta en el teléfono, en USB-OTG o en Google Drive con decenas de archivos mezclados de este caso. |
| **Incorporar imágenes / capturas** | Quiere tomar fotos, capturas o PDF cortos concretos sin arrastrar todo lo demás. |

Ambos usan el **mismo enrutador automático**; la única diferencia es *qué elige*: un árbol completo o una lista seleccionada.

### 2.1 Incorporación de carpeta

Al tocarlo se abre el selector de directorios SAF de Android. Elija la carpeta del caso, pulse *Usar esta carpeta*, y Forseti:

1. Recorrerá cada archivo del árbol (subcarpetas incluidas).
2. Examinará el nombre y la extensión.
3. Colocará cada archivo en la carpeta Brokkr Forge correcta.
4. Registrará cada movimiento en `00_INDEX.txt` como constancia.
5. Mostrará un mensaje: *"Se importaron 42 archivos · 6 a 99_Inbox"*.

Lo que Forseti no clasifique irá a `99_Inbox/`. Abra el caso (Perfil de caso → toque la tarjeta) y muévalos manualmente — por lo general un toque por archivo.

### 2.2 Incorporación de imágenes

Abre el selector del sistema filtrado a imágenes, PDF, audio y video. Seleccione varios, pulse *Abrir*, y corre el mismo enrutador.

Útil cuando tiene mil fotos en la galería pero solo necesita las siete del escalón roto de mayo.

---

## 3. Haga inteligente el enrutador renombrando

El enrutador decide dónde va un archivo mirando primero el **nombre del archivo**, luego la **extensión**.

La forma más rápida de mejorar la precisión es renombrar **antes** de incorporar. Dos ejemplos:

| Nombre malo | Nombre bueno | Destino |
| --- | --- | --- |
| `IMG_20240312_103144.jpg` | `2024-03-12_broken_stair_evidence.jpg` | `04_Evidence/Photos/` |
| `Document (3).pdf` | `2024-04-01_motion_to_dismiss.pdf` | `05_Motions/Drafts/` |
| `Screenshot_20240419_181203.png` | `2024-04-19_screenshot_text_threats.png` | `04_Evidence/Screenshots/` |
| `scan001.pdf` | `2024-05-08_proof_of_service_summons.pdf` | `02_Service_of_Process/Proof_of_Service/` |

No tiene que renombrar todo — Forseti detecta palabras como `complaint`, `answer`, `motion`, `discovery`, `deposition`, `interrog`, `subpoena`, `service`, `summons`, `exhibit`, `hearing`, `witness`, `trial brief`, `order`, `judgment`. Use la tabla como guía; el resto irá a `99_Inbox/` para que usted clasifique.

---

## 4. Mejor flujo para principiantes

1. Cree el caso en **Perfil de caso** con al menos título, tribunal y número de caso.
2. En la galería, **renombre capturas y fotos** importantes — cinco minutos bien invertidos.
3. En el diálogo **Editar caso**, toque **Incorporar carpeta** y apunte Forseti a la carpeta desordenada.
4. Abra el caso y revise **99_Inbox/**. Mueva rezagados a la carpeta Brokkr Forge correcta.
5. Fije plazos en la pestaña **Plazos** — Forseti creará subcarpetas en `07_Deadlines/`.
6. Use **Compartir** desde correo o lector PDF y elija **Forseti → Espacio de trabajo del caso** para seguir añadiendo documentos.

---

## 5. Abrir, renombrar y reubicar desde el caso

Una vez dentro del espacio de trabajo no tiene que salir de Forseti.

- **Toque cualquier archivo** en **Perfil de caso → su caso** para abrirlo en el visor integrado:
  - Los PDF se abren con el mismo lector con zoom que usa para la FRCP, con botón **Leer en voz alta**.
  - Las imágenes en vista previa con zoom.
  - Texto/markdown con texto seleccionable — mantenga pulsado para copiar.
  - Otros formatos: atajo **Abrir en otra app**.
- **Renombre** con el icono del lápiz. Tras cada cambio Forseti pregunta *¿Mover a otra carpeta?* — elija el nuevo destino en un toque o **Mantener aquí**.
- **Compartir** envía el archivo por la hoja de compartir de Android (FileProvider; otras apps reciben URL temporal de lectura).
- **Eliminar** quita el archivo del espacio de trabajo de forma permanente.

> **Consejo.** Un recibo escaneado como `scan001.pdf` caerá en `98_Scans/`. Ábralo, **Renombre** → `2024-08-14_receipt_repair_invoice.pdf`, luego elija `04_Evidence/PDFs/`. Listo en 10 segundos.

---

## 6. Privacidad

Cada archivo incorporado se copia al almacenamiento privado de Forseti en `Android/data/com.forseti/files/case_workspace/`. No se sube nada; no se comparte hasta que toque **Compartir** en un archivo concreto. La copia de seguridad automática solo restaura sus datos en la misma cuenta de Google al reinstalar.

---

> **Filosofía Forseti:** La IA puede ser poderosa, pero nada supera el espíritu y la determinación humanos — que esto le guíe en su camino.

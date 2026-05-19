# Atajos de Forseti y hoja de referencia de flujo seguro

Esta guía es la ruta más rápida por la aplicación. Solo describe lo que Forseti
hace hoy — sin promesas ni hoja de ruta. Si algún paso deja de funcionar,
reporte el problema.

> **Filosofía Forseti.** La IA puede ser poderosa, pero nada supera el espíritu
> y la determinación humanos — que esto le guíe en su camino. No imponemos
> respuestas; organizamos el procedimiento para que llegue preparado.

---

## 1. Llevar un PDF *a* la aplicación

Tiene cuatro caminos igual de rápidos. Elija el que encaje en el momento:

1. **Pestaña Escáner de documentos** — ideal cuando tiene una página en papel.
   Toque **Capturar página** por cada hoja, luego **Guardar PDF en el caso**. Forseti
   archiva el resultado en la carpeta de fase correspondiente si pone una palabra clave
   como `motion`, `discovery`, `answer` u `order` en el campo de etiqueta.
2. **Compartir a Forseti** — abra cualquier PDF en el teléfono (Archivos, Drive,
   adjunto de correo, app de escáner) y use **Compartir** del sistema →
   elija **Forseti**. La app pregunta si archivarlo como Regla cargada o en un espacio de caso.
3. **Reglas cargadas → Importar PDF** — para PDF de reglas que Forseti no pudo
   descargar (sitio estatal caído, HTML de pago, su copia anotada). Nunca se archivan solos en un caso.
4. **Perfil de caso → abrir caso → Importar archivo** dentro de cualquier subcarpeta
   del espacio de trabajo. Útil cuando ya sabe en qué carpeta va el documento.

> **Por qué compartir a la app es útil:** el PDF queda privado de Forseti.
> No crea una segunda copia en el carrete o en Descargas.

---

## 2. Archivo automático (cómo Forseti decide dónde va cada cosa)

Al guardar un escaneo o un PDF compartido en un caso, Forseti busca palabras clave
en la etiqueta o el nombre de archivo y envía el archivo a la carpeta de fase:

| Palabra clave en etiqueta/nombre | Destino |
| --- | --- |
| `complaint` | `01_Pleadings/Drafts/` |
| `answer` | `02_Answer/Drafts/` |
| `motion`, `sanction` | `03_Motions/Drafts/` |
| `discovery`, `interrog`, `deposition`, `subpoena`, `rfp`, `rfa` | `04_Discovery/Drafts/` |
| `pretrial`, `trial` | `05_Trial/Drafts/` |
| `judgment`, `appeal` | `06_PostTrial/Drafts/` |
| `order` | `07_General/Drafts/` |
| `exhibit` | `<fase>/Exhibits/` si existe |
| cualquier otra | `98_Scans/` (puede moverlo después) |

Siempre puede cambiar la ubicación en **Perfil de caso → su caso** con renombrar / compartir / eliminar.

> **Los PDF de reglas cargadas nunca se archivan solos en un caso.** Permanecen en Reglas cargadas hasta que los comparta fuera.

---

## 3. Hábitos de captura que evitan dolores de cabeza

- **Mantenga la página plana.** El escáner usa la cámara trasera; páginas curvas salen mal en el PDF.
- **Use el campo de etiqueta siempre.** Tres segundos de tecleo ahora significan encontrar el archivo después.
- **Capture la portada al final** para que quede primera al apilar el PDF.
- **Documentos largos: lotes de 10 páginas o menos.** Si sale a mitad de captura, solo lo ya guardado queda confirmado.
- **Prefiera la cámara de Forseti al escáner del teléfono** si quiere que el archivo caiga directo en el caso — Forseti solo archiva automáticamente lo que captura o lo que comparte a la app.

---

## 4. Mientras lee las reglas

La pestaña **Salto rápido** es donde pasa más tiempo leyendo. Atajos ocultos:

- El **icono de libro** abre el panel de búsqueda — escriba un número de regla ("12(b)(6)", "26", "56(a)") para ir directo.
- El **icono de nota adhesiva** abre un bloc rápido sobre la página. Escriba, **Guardar** para conservarlo (aparece en Notas anclado a la página), o **Listo** para descartar.
- El **icono de marcador** destaca la página en Notas → Marcadores. Toque de nuevo para quitar.
- El icono **Leer** ejecuta OCR en el dispositivo sobre la página actual y la lee en voz alta con la voz de Android configurada en *Ajustes → Accesibilidad → Salida de texto a voz*. **Detener** para cortar. Si no hay voz, la app abre los ajustes (o instalar el motor TTS de Google).
- Pellizco para zoom en el PDF; un dedo para desplazar.
- El contador inferior derecho muestra la página actual.

### Pulsación larga para copiar/pegar

En **Guías**, **Notas** y el visor de archivos de texto/markdown del caso,
mantenga pulsado para arrastrar los tiradores de selección de Android. **Copiar**
lleva la selección al portapapeles para pegar en su borrador, correo o chat. Los PDF son imágenes por página; use **Leer en voz alta** y una nota rápida a mano en ese caso.

---

## 5. Borradores y firmas

La pestaña **Borradores de mociones** genera PDF editables desde plantillas incluidas —
esqueletos al estilo FRCP para mociones y contestaciones. Forseti nunca firma por usted;
imprima el borrador, fírmelo a mano y vuelva a capturarlo con el Escáner de documentos para que la copia firmada quede junto al borrador sin firmar.

---

## 6. Copias de seguridad

La pestaña **Copia de seguridad** genera un ZIP de todo el espacio de trabajo del caso.
Guárdelo en Drive, envíeselo por correo o cópielo a una memoria USB.
Restaurar en una instalación nueva de Forseti devuelve todo a su sitio.

---

## 7. Conocer los límites

Forseti es **solo información — no asesoramiento legal**. La aplicación:

- incluye las Federal Rules of Civil Procedure de dominio público (ed. 1 dic. 2024)
- enlaza al sitio oficial de cada estado para reglas estatales
- guarda en caché PDF que usted descargue para uso sin conexión
- nunca envía datos del caso a ningún sitio; todo queda en este dispositivo

Si un enlace estatal devuelve 404, el sitio del estado cambió — pruebe la pestaña **Referencias**
para la URL principal más estable, o importe su PDF en **Reglas cargadas**.

---

## 8. El flujo que usan los que perseveran

1. Abra **Perfil de caso** → cree el caso.
2. Abra **Plazos** → anote cada fecha que le dio el tribunal.
3. Abra **Escáner de documentos** → capture la orden, etiquétela `order`.
4. Abra **Borradores** → genere el esqueleto de moción de respuesta.
5. Abra **Salto rápido** → relea la regla que citó la orden.
6. Toque el **icono de nota adhesiva** → anote lo que argumentará.
7. Edite el borrador en cualquier app PDF; imprima, firme, escanee de nuevo.
8. Abra **Copia de seguridad** → ZIP de la carpeta del caso antes de ir al tribunal.

Ese es el ciclo. Repítalo en cada moción, cada audiencia, cada apelación.
El sistema fue diseñado para desgastar con el procedimiento — Forseti
mantiene el procedimiento visible para que pueda competir en igualdad de condiciones.

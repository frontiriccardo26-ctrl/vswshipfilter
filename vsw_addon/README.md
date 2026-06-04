# VSW Ship ID Filter

Addon per **Valkyrien Space War** (Forge 1.20.1).

## Problema risolto

Quando copii una nave con tutti i suoi blocchi (Control Seat, RS Channel Sender, RS Channel Receiver), i **canali RS interferiscono** tra le navi copiate perché usano solo due parametri (nome canale + filtro numerico) che sono uguali su entrambe le copie.

## Soluzione

Questo addon aggiunge un **terzo filtro opzionale** all'**RS Channel Receiver**: il **Ship ID di Valkyrien Skies**.

Quando attivato, il Receiver risponde **solo** ai segnali provenienti da un Sender che si trova sulla **stessa nave** (stesso Ship ID VS2). Le navi copiate avranno Ship ID diversi → nessuna interferenza.

## Come usarlo

1. Piazza e configura i tuoi blocchi **RS Channel Receiver**, **RS Channel Sender** e **Control Seat** come al solito (stesso nome canale + stesso numero filtro).
2. Apri la GUI dell'**RS Channel Receiver**.
3. In basso vedrai il nuovo pulsante:
   - **`[Ship ID Filter: OFF]`** → filtro disattivato (comportamento originale)
   - **`[Ship ID Filter: ON ]`** → filtro attivato (solo segnali dalla stessa nave)
4. Clicca il pulsante per attivarlo su **tutti i Receiver** della nave.
5. Ora puoi copiare la nave: ogni copia avrà il suo Ship ID e i canali non si mescoleranno.

> **Nota:** il filtro funziona solo quando il Receiver è montato su una nave VS2.
> Se il blocco è nel mondo normale (non su una nave), il filtro viene ignorato.

## Dipendenze richieste

- Minecraft **1.20.1**
- Forge **47+**
- Valkyrien Skies 2 (`valkyrienskies-1.20.1-forge`)
- Valkyrien Space War **0.6.x**

## Compilazione (per sviluppatori)

```bash
# 1. Copia la jar originale nella cartella libs/
mkdir -p libs
cp valkyrien_space_war-0_6_18.jar libs/

# 2. Compila
./gradlew build

# Il file .jar si trova in: build/libs/vsw_shipfilter-1.0.0.jar
```

## Installazione

Copia `vsw_shipfilter-1.0.0.jar` nella cartella `mods/` insieme a:
- `valkyrien_space_war-0_6_18.jar`
- la jar di Valkyrien Skies 2

## Come funziona (tecnico)

La mod usa **Mixin** (SpongePowered) per:

1. **`MixinRsChannelReceiverBe`** — aggiunge il campo `vsw_shipIdFilter` (boolean) all'NBT del BlockEntity tramite hook su `saveAdditional`/`load`.

2. **`MixinRsChannelManager`** — intercetta `sendRsChannel` con un `@Inject HEAD` (per calcolare gli Ship ID di sender e receiver dalla mappa VS2) e un `@Redirect` sulla chiamata a `receiveRsChannel` per bloccare i segnali cross-ship quando il filtro è attivo.

3. **`MixinRsChannelReceiverScreen`** *(client only)* — aggiunge il pulsante toggle alla GUI.

4. **`MixinRsChannelReceiverBeSync`** — quando il server invia il packet di sincronizzazione della GUI al client, invia anche lo stato del filtro.

La comunicazione client↔server usa due packet Forge Network:
- `SetShipFilterPacket` (client → server): toggle del filtro
- `SyncShipFilterPacket` (server → client): stato iniziale all'apertura della GUI

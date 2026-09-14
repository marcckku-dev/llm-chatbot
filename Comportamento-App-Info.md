# Comportamento e informazioni dell'applicazione

## Descrizione

L'applicazione è un chatbot basato su Spring Boot che espone API REST e un canale
WebSocket per comunicare con un modello linguistico locale gestito da Ollama.

L'applicazione:

1. crea sessioni di chat tramite identificativi UUID;
2. riceve messaggi dagli utenti;
3. inoltra il testo a Ollama;
4. salva messaggio utente e risposta del modello nel database H2;
5. restituisce la risposta al client tramite REST oppure WebSocket;
6. consente di recuperare la cronologia associata a una sessione;
7. espone un health check per verificare la disponibilità di Ollama.

## Tecnologia e architettura

- **Framework:** Spring Boot 3.2
- **Java:** 17
- **Tipo di applicazione:** API REST con supporto WebSocket/STOMP
- **Architettura:** Controller - Service - Repository
- **Database:** H2 in-memory con Spring Data JPA
- **LLM:** Ollama
- **Versione Ollama verificata:** `0.34.0`
- **Modello configurato:** `mistral`
- **Porta applicativa:** `8581`
- **Context path:** `/api`
- **URL base:** `http://localhost:8581/api`
- **URL Ollama:** `http://localhost:11434`

## Ollama

### Installazione e versione

Ollama è il runtime locale utilizzato dall'applicazione per eseguire il modello
linguistico senza inviare prompt a un provider cloud. La versione installata e
verificata durante la configurazione è:

```text
ollama version is 0.34.0
```

L'installazione è stata effettuata su Windows con lo script ufficiale:

```powershell
irm https://ollama.com/install.ps1 | iex
```

### Modello installato

Il modello configurato e disponibile localmente è:

```text
mistral:latest
```

È stato scaricato con:

```powershell
ollama pull mistral
```

Il download occupa circa `4,4 GB`; lo spazio effettivo può variare in base
alla versione del modello e ai file già presenti nella cache locale.

### Avvio e API

Il server Ollama viene avviato con:

```powershell
ollama serve
```

L'API locale ascolta su:

```text
http://localhost:11434
```

L'applicazione utilizza questi endpoint Ollama:

- `GET /api/tags`: verifica la disponibilità del servizio e dei modelli;
- `POST /api/generate`: invia il prompt al modello configurato e riceve la
  risposta generata.

La configurazione applicativa imposta:

```yaml
ollama:
  url: http://localhost:11434
  model: mistral
  timeout: 30000
```

Il client HTTP dell'applicazione utilizza inoltre un timeout di connessione di
30 secondi e un timeout di lettura di 120 secondi, perché la generazione può
richiedere tempo.

### Cosa può fare

Con il modello `mistral`, Ollama può:

- generare risposte testuali;
- rispondere a domande e richieste conversazionali;
- riassumere, riformulare e tradurre testi;
- produrre spiegazioni, esempi e frammenti di codice;
- funzionare interamente in locale dopo il download del modello;
- essere utilizzato senza una connessione cloud durante la generazione.

Le capacità effettive dipendono dal modello installato, dalla memoria
disponibile e dalle risorse CPU/GPU del computer.

### Cosa non può fare automaticamente

Ollama e il modello non:

- garantiscono che le risposte siano corrette o aggiornate;
- sostituiscono una fonte ufficiale o una validazione umana;
- conoscono automaticamente dati presenti nel database dell'applicazione;
- mantengono la memoria delle conversazioni tra richieste: l'applicazione
  invia al modello solo il messaggio corrente;
- eseguono azioni sul sistema operativo tramite questa applicazione;
- accedono autonomamente a Internet, file locali o servizi esterni;
- offrono autenticazione, autorizzazione o controllo degli accessi per le API
  dell'applicazione;
- rendono l'applicazione multiutente o persistente oltre il ciclo di vita del
  database H2 in-memory.

Il modello può comunque produrre contenuti inesatti, incompleti o plausibili ma
non verificati. Le risposte devono quindi essere considerate generate
automaticamente e sottoposte a controllo quando riguardano decisioni
importanti.

### Requisiti operativi e limiti

Per usare il chatbot è necessario che:

1. Ollama sia installato;
2. il processo `ollama serve` sia attivo;
3. il modello `mistral` sia stato scaricato;
4. la porta `11434` sia raggiungibile localmente;
5. il computer disponga di spazio disco e memoria sufficienti.

La generazione può essere lenta su macchine prive di GPU o con poca memoria.
Il primo avvio del modello può richiedere più tempo perché il modello viene
caricato in memoria. Se Ollama è spento, il chatbot risponde con HTTP `503`
all'health check e con HTTP `500` durante un tentativo di generazione.

Comandi utili:

```powershell
ollama --version
ollama list
ollama pull mistral
ollama serve
```

Per verificare direttamente l'API:

```powershell
Invoke-WebRequest http://localhost:11434/api/tags
```

## Logging

Il logging applicativo utilizza Lombok `@Slf4j`, che genera i logger SLF4J
senza boilerplate. Sono registrati:

- avvio delle operazioni di chat e persistenza dei messaggi;
- recupero della cronologia;
- chiamate a Ollama e relativi codici HTTP;
- errori REST, WebSocket e Ollama;
- risultato dei controlli di disponibilità del servizio LLM.

Per evitare di esporre dati conversazionali, il testo completo dei prompt e
delle risposte non viene scritto nei log.

I log applicativi vengono salvati in:

```text
logs/llm-chatbot.log
```

Il livello predefinito per il package applicativo è `INFO`; i dettagli
diagnostici non sensibili sono disponibili a livello `DEBUG`. I file di log
locali sono esclusi dal repository tramite `.gitignore`.

## Comportamento principale

### Creazione della sessione

Una richiesta a `POST /api/chat/session/new` genera un nuovo UUID. L'identificativo
deve essere utilizzato dal client nelle successive richieste di messaggistica e
per il recupero della cronologia.

### Invio di un messaggio REST

Il client invia `sessionId` e `message` a `POST /api/chat/message`.

Il servizio:

1. chiama Ollama sull'endpoint `/api/generate`;
2. attende la risposta del modello;
3. crea un'entità `ChatMessage`;
4. salva il messaggio nel database;
5. restituisce id, messaggio utente, risposta AI e timestamp.

Se Ollama restituisce un errore o non è raggiungibile, la richiesta restituisce
HTTP `500` e il messaggio non viene salvato.

### Recupero della cronologia

`GET /api/chat/history/{sessionId}` restituisce i messaggi della sessione
ordinati per timestamp crescente.

### Verifica disponibilità LLM

`GET /api/chat/health` verifica la disponibilità di Ollama chiamando il suo
endpoint `/api/tags`.

- Ollama disponibile: HTTP `200`
- Ollama non disponibile: HTTP `503`

## Endpoint REST esposti

### Creazione sessione

```http
POST /api/chat/session/new
```

Risposta di esempio:

```text
"550e8400-e29b-41d4-a716-446655440000"
```

### Invio messaggio

```http
POST /api/chat/message
Content-Type: application/json
```

Body:

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Qual è la capitale della Francia?"
}
```

Risposta di esempio:

```json
{
  "id": 1,
  "userMessage": "Qual è la capitale della Francia?",
  "aiResponse": "La capitale della Francia è Parigi.",
  "timestamp": "2026-09-14T21:30:00"
}
```

In caso di errore durante la generazione:

```text
HTTP 500
Error processing message: <dettaglio errore>
```

### Recupero cronologia

```http
GET /api/chat/history/{sessionId}
```

Risposta di esempio:

```json
[
  {
    "id": 1,
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "userMessage": "Ciao",
    "aiResponse": "Ciao! Come posso aiutarti?",
    "timestamp": "2026-09-14T21:30:00"
  }
]
```

### Health check

```http
GET /api/chat/health
```

Risposte:

```text
HTTP 200
LLM service is available
```

oppure:

```text
HTTP 503
LLM service is unavailable
```

## Endpoint WebSocket/STOMP

Endpoint di connessione:

```text
ws://localhost:8581/api/chat-socket
```

L'endpoint supporta SockJS.

### Invio del messaggio

Il client pubblica il messaggio su:

```text
/app/chat
```

Payload:

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Raccontami una barzelletta"
}
```

### Ricezione della risposta

Il client deve sottoscriversi a:

```text
/topic/chat/{sessionId}
```

In caso di successo riceve una risposta con id, messaggio utente, risposta AI e
timestamp. In caso di errore riceve una risposta con `id` e `timestamp` null e
un testo che inizia con `Error:`.

## Persistenza

I messaggi sono memorizzati nell'entità `ChatMessage`, con i seguenti campi:

- `id`: identificativo numerico generato dal database;
- `sessionId`: identificativo della sessione;
- `userMessage`: testo inviato dall'utente;
- `aiResponse`: risposta generata da Ollama;
- `timestamp`: data e ora di creazione.

Il database H2 è configurato in-memory:

```text
jdbc:h2:mem:chatbotdb
```

Lo schema viene creato all'avvio e rimosso allo spegnimento dell'applicazione.
La console H2 è disponibile su:

```text
http://localhost:8581/api/h2-console
```

## Configurazione Ollama

La configurazione corrente è:

```yaml
ollama:
  url: http://localhost:11434
  model: mistral
  timeout: 30000
```

Ollama deve essere in esecuzione e il modello deve essere installato:

```powershell
ollama serve
ollama pull mistral
```

## Verifica e test

La suite automatica comprende test unitari, test del repository e test di
integrazione REST.

Ultimo risultato verificato:

```text
Tests run: 13
Failures: 0
Errors: 0
BUILD SUCCESS
```

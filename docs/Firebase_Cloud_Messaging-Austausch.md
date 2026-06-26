# Firebase Cloud Messaging - Austausch von Hintergrunddaten zwischen zwei Geräten

Um Daten zwischen zwei Geräten über Firebase Cloud Messaging (FCM) auszutauschen, müssen Sie einen serverseitigen oder gerätebasierten Ansatz wählen, da FCM primär für Downstream-Nachrichten (Server zu Gerät) konzipiert ist.

**1. Token-Management auf dem Server**
Jedes Gerät generiert einen eindeutigen **FCM-Token**. Dieser Token muss an Ihren Backend-Server gesendet und dort in einer Datenbank (z. B. Firebase Firestore oder Realtime Database) gespeichert werden, um später Nachrichten an das Zielgerät senden zu können.

**2. Nachrichtenaustausch-Methoden**
*   **Server-vermittelter Austausch (Empfohlen):** Gerät A sendet die Daten an Ihren Server. Der Server speichert die Daten und löst über das FCM SDK eine Nachricht an Gerät B aus, die die Daten im Payload enthält. Dies ist sicherer, da der Server-Key nicht auf dem Client liegt.
*   **Direkter HTTP-Post von Gerät zu Gerät:** Ein Gerät kann eine HTTP-Anfrage an die FCM-Endpunkt-URL (`https://fcm.googleapis.com/fcm/send`) senden. Dies erfordert die Eingabe des Legacy Server Keys im Client-Code, was aus Sicherheitsgründen **nicht empfohlen** wird, da der Key von Angreifern extrahiert werden kann.

**3. Technische Implementierung**
Für den serverseitigen Versand nutzen Sie das jeweilige Admin SDK (z. B. Node.js, Java, Python). Das Payload-Objekt enthält den Ziel-Token und die zu übertragenden Daten. Für Daten-only-Nachrichten müssen Sie auf Android die Priorität auf „high“ setzen und auf iOS das Flag `content-available` aktivieren, um Hintergrundaktualisierungen zu ermöglichen.

```javascript
// Beispiel: Node.js Admin SDK zum Senden an ein spezifisches Gerät
const message = {
  token: 'ZIELGERAET_TOKEN',
  data: {
    key1: 'value1',
    key2: 'value2'
  }
};

admin.messaging().send(message)
  .then((response) => {
    console.log('Erfolgreich gesendet:', response);
  })
  .catch((error) => {
    console.log('Fehler beim Senden:', error);
  });
```

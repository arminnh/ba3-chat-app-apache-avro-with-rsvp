==================================================
||   Project Telecom/Gedistribueerde Systemen   ||
==================================================

Deze README beschrijft enkel wat geimplementeerd is.
Meer info over hoe de programma's worden geinstalleerd en gebruikt staat in de READMEs in de subdirectories.


Auteurs
==================================================
Josse Coen      -   20134090
Armin Halilovic -   20122210



Telecommunicatie Systemen:           RSVP in Click
==================================================

Behaalde functionaliteit
--------------------------------------------------
Alles.



Gedistribueerde Systemen: Chat app met Apache Avro
==================================================

Behaalde functionaliteit
--------------------------------------------------

Alles.
Meer specifieke info over functionaliteit en implementatie is te vinden in gedistribueerde-systemen/.



Integratie
==================================================

Behaalde functionaliteit
--------------------------------------------------

Alles: een videosessie in de chat-app zal via het gedeelte in Click, via RSVP een padreservatie opzetten.
De reservatie kan ook manueel, buiten de applicatie, opgezet worden. 
Video's zullen dan via QoS gestreamed worden. Dit kan in in een richting of in twee richtingen tegelijk.
Om de QoS reservatie te doen werken moeten de juiste click scripts worden aan gezet voor dat Chat App Clients worden aangezet (zie stappenplan).
Als de scripts niet aan staan zal Chat App gewoon werken zonder QoS.

Stappenplan voor integratie (met 2 PCs)
--------------------------------------------------
1. Run setup_ds_server.sh op een PC.
   Run host2.click op poort 10000.
2. Start AppServer op op dezelfde PC als in stap 1.
3. Start AppClient op op de zelfde PC.
4. Run setup_click.sh op een andere PC.
5. Start AppClient op op de PC in stap 4.
   Run ipnetwork.click op poort 10000.
6. Start een private chat sessie tussen de twee AppClient instances.
7. Om een reservatie aan te vragen (= path message te versturen), schrijf "?videoRequest" of "?vr"
8. Om een reservatie te aanvaarden (= resv message versturen en reservatie pad opstellen), schrijf "?acceptVideo" of "?av"
9. Kies een video om te streamen.
10. Met read_statistics.sh zie je dat de video wordt gestreamed met QoS.




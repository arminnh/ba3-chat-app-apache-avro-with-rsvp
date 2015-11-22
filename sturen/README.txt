==================================================
||   Project Telecom/Gedistribueerde Systemen   ||
==================================================
Deze README bevat enkel info over wat er is geimplementeerd tot nu toe.
Meer info over hoe de programma's worden gebruikt staat in de READMEs in de subdirectories.


Auteurs
==================================================
Josse Coen      -   20134090
Armin Halilovic -   20122210


Telecommunicatie Systemen:           RSVP in Click
==================================================

Behaalde functionaliteit
--------------------------------------------------
*) het sturen van alle benodigde RSVP berichten met behulp van handlers
    Onderdelen van berichten moeten eerst worden gezet met handlers. Na het instellen van gewenste onderdelen kunnen berichten worden versuurd.
    Onderdelen die kunnen worden ingesteld:
        session, hop, timevalues, scope, reservation confirmation, error spec
    Berichten die kunen worden verstuurd:
        path, resv, path error, resv error, path tear, resv tear, resv conf
        
*) op pakketten kan de TOS byte gezet worden met een handler
    Ondersteund door het MyIPEncap element met de "tos" handler
    
*) een Click script toont hoe priority scheduling is geimplementeerd met enkel bestaande Click elementenn
    Zie priority-scheduling.click

*) voorgestelde waarden voor de TSpec en RSpec parameters, om het gevraagde scenario te ondersteunen
    J
    O
    S
    S
    E

    P
    L
    Z

    H
    I
    E
    R
      


Gedistribueerde Systemen: Chat app met Apache Avro
==================================================

Behaalde functionaliteit
--------------------------------------------------

Server:
    *) Een gebruiker kan registreren bij de server, de server kent een unieke id/gebruikersnaam toe aan die gebruiker.
    *) De server houdt een lijst bij van alle verbonden gebruikers en hoe deze bereikt kunnen worden.
    *) De server staat in voor het afhandelen van het joinen van de publieke chat room en het verzenden van berichten binnen dit groepsgesprek.
    *) Wanneer een client de applicatie verlaat, moeten zijn gegevens uit de lijst van verbonden gebruikers worden verwijderd.
   
Client:
    *) Automatische registratie bij de server. 
    *) Nadat een gebruiker geregistreerd is, kan hij commando’s in geven.
    *) Een gebruiker kan een lijst opvragen van andere online gebruikers.
    *) Een gebruiker kan besluiten de publieke chat room te joinen en boodschappen uit te wisselen met andere personen in deze chat room.
    *) Een gebruiker kan om de publieke chat room verlaten, of de chat applicatie te verlaten.

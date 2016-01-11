Deze README legt uit hoe je de labo PC's gebruikt en hoe je click kan starten.


A. Labo PC's
------------

1. Hoe log je in?

Na opstarten verschijnt een login venster voor de "student" gebruiker.
Het paswoord dat je dient te gebruiken is "mvkbj1n" (met veel kabels bouw je 1 netwerk)


2. Hoe gebruik je de PC's voor het project?

De labo PC's zijn reeds twee aan twee rechtstreeks met een netwerkkabel met elkaar verbonden.
PC 1 is verbonden met PC 2, PC 3 met PC4, enzovoort.
De netwerkkabel steekt in beide PC's in de "eth0" interface.
Eens je bent ingelogd, kan je in de home directory van de student gebruiker de map "click-2.0.1" terugvinden.
In die map staat een uitgepakte (standaard) versie van click, reeds gecompileerd.
Om je eigen elementen toe te voegen, plaats je die in elements/local en hercompileer.
In de click-2.0.1 map vind je een map "scripts" terug. Deze bevat een aantal bestanden, die in deel B. van deze README worden verduidelijkt.



B. Bestanden
------------

1. setup_click.sh

Dit scriptje moet je éénmalig draaien op een PC voordat je click start.
Dit zorgt ervoor dat de juiste netwerkinterfaces worden aangemaakt en geconfigureerd.


2. setup_ds_server.sh

Dit scriptje moet je éénmalig draaien op de andere PC.
Dit zorgt ervoor dat de juiste netwerkinterfaces worden aangemaakt en geconfigureerd.


3. read_statistics.sh

Dit script verbindt met een lopende click router, reset alle aanwezige counters, en leest die na een korte pauze terug uit.
De aanwezige counters zijn:
	- TX counter voor verkeer verzonden vanop de Linux PC (die waarop Click draait)
	- RX counter voor verkeer ontvangen door de Linux PC (die waarop Click draait)
	- TX counter voor verkeer verzonden door Router 2 naar de LAN (dus naar de andere PC)
	- RX counter voor verkeer ontvangen door Router 2 op de LAN (dus ontvangen van de andere PC)
Wat je te zien krijgt is hoeveel kbps er momenteel wordt verstuurd en ontvangen.
Best-Effort en Quality-of-Service verkeer worden apart geteld.
LET OP! De counters bevinden zich vóór alle bandwidth shapers en routers e.d.


4. ipnetwork.click

Het click script, gebruikt voor de evaluatie van het geïntegreerde project.
Het script verzorgt de communicatie tussen beide labo PC's.
Zie deel C. voor meer uitleg.


5. host2.click

Het click script dat op de tweede PC moet runnen (die waar de server van het gedistribueerde systeem draait).


6. ipnetwork-local.click

Het click script, gebruikt voor de evaluatie van het standalone Telecom project.
Dit kan je in eerste instantie ook gebruiken voor het geïntegreerde project.
Om dit scriptje te gebruiken, heb je maar één PC nodig (alles draait lokaal).
Dit scriptje bevat standaard ook traffic generators die continu verkeer genereren.
Zie deel C. voor meer uitleg.



C. Click scripts
----------------

Hieronder vind je een schematische voorstelling terug van de click scripts.
Het schema toont de belangrijkste componenten en hoe deze met elkaar zijn verbonden.

1. ipnetwork.click en host2.click

 /--------\       /------\        /------\       /----------\       /----------\       /--------\
 | ToHost |=======| eth0 | =======| eth0 |===C---| router 2 |-------| router 1 |---C===| ToHost |
 \--------/       \------/        \------/       \----------/       \----------/       \--------/

\---------------------------/  \----------------------------------------------------------------/
     PC 2 (host2.click)                              PC 1 (ipnetwork.click)

Zoals je hierboven kan zien, werkt dit click script als volgt:
Er draait op beide PCs een click script.
Op PC1 gebeurt het meeste. Daar zijn er (virtueel) twee routers gedefinieerd, router1 en router2 (zie ook in het script zelf).
Deze routers zijn compound elements, gedefinieerd in routers/router.click.
Op PC2 gebeurt niet zoveel: Click leest en schrijft pakketten op de netwerkinterface en speelt deze door naar Linux (m.b.v. ToHost).
De ToHost kan je vinden in routers/host.click.
Deze twee files zijn meteen ook de enige die jullie mogen/moeten aanpassen.
Alle overige click files zijn goed zoals ze zijn en worden niet aangepast.
In deze file zullen jullie wijzigingen moeten doen om RSVP te implementeren.
Router 2 luister langs een kant op interface eth0 van de labo PC.
Deze is verbonden (via eth0) met de tweede labo PC (die voor de server van het gedistribueerde systeem).
Intern wordt er dan, via router 2 en router 1, (virtueel) gerouteerd, totdat je het pakket aflevert aan het ToHost element, dat uiteindelijk de pakketten aan de labo PC aflevert.
De "C" in het schema is de locatie van de counters (TX en RX).
De dubbele lijnen (===) stellen delen van het netwerk voor zonder bandbreedtebeperking.
De enkele lijnen (---) stellen delen voor die begrensd zijn op 1 Mbps.

Voor het uiteindelijke project zal je dus een videostream end-to-end hebben lopen.
Ook voorzie je achtergrondverkeer (bijv. met het iperf commando, zie https://iperf.fr).
M.b.v. de counters (en door naar de video te kijken), kunnen we dan zien of de video inderdaad prioriteit krijgt.


2. ipnetwork-local.click

Een variant van het click script, bedoeld voor puur lokaal gebruik.
Dit is tevens het click script voor de studenten die enkel Telecom volgen.

/--------\       /----------\       /----------\       /--------\
| Host 2 |===C---| router 2 |-------| router 1 |---C===| Host 1 |
\--------/       \----------/       \----------/       \--------/

Het ipnetwork-local.click script ziet er zo uit.
Alles gebeurt lokaal, er is geen interactie met een fysiek netwerk.
De counters staan nog steeds op dezelfde plaats.
De "eth0" en "ToHost" zijn vervangen door twee "Host" compound elements.
Standaard lopen er steeds twee UDP stromen van host 1 naar host 2:
	- 1 UDP stroom van 300 kbps
	- 1 UDP stroom van 1000 kbps
Voor meer info over de stromen (poorten e.d.): kijk in het ipnetwork-local.click script
Als je met dit script werkt, is het de bedoeling dat de eerste UDP stroom (die van 300 kbps) prioriteit krijgt.
De routers worden weer verzorgd door het script in routers/router.click
De hosts worden geïmplementeerd in routers/host-local.click
Net zoals bij het geïntegreerde project zijn dit de enige twee click files die je mag/moet aanpassen!

Dit click script kan je gebruiken:
	- als je enkel het stuk Telecom van het project moet doen. Merk op dat het dan ook niet nodig is om het setup_click.sh script te runnen.
	- als je verder thuis of op je laptop puur aan het click gedeelte wilt werken.
	
LET GOED OP!!!
Voor wie het geïntegreerde project moet doen: jullie implementatie telt niet als ze enkel op dit lokaal script werkt.
Daar wordt niet naar gekeken, enkel naar de werking in ipnetwork.click
DIT CLICK SCRIPT GEBRUIKT EEN ANDERE INCLUDE VOOR "HOST" (host-local.click)!!!
Let daarop als je dingen aanpast in host.click!!!

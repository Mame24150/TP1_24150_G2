# TP1 — SOAP et WSDL (Réponses aux questions)

---

## Partie A — Code-first

### Étape 2 : Lire le WSDL généré

**1. Quelle est l'URL du service ? Quelles sont les deux opérations et leurs messages d'entrée et de sortie ?**
- **URL du service** : `http://localhost:8080/bib/EmpruntService`
- **Opération 1 (`emprunterLivre`)** :
  - Message d'entrée (`input`) : `tns:emprunterLivre`
  - Message de sortie (`output`) : `tns:emprunterLivreResponse`
  - Message d'erreur (`fault`) : `tns:LivreIndisponibleException`
- **Opération 2 (`rendreLivre`)** :
  - Message d'entrée (`input`) : `tns:rendreLivre`
  - Message de sortie (`output`) : `tns:rendreLivreResponse`

**2. Quel style (document ou rpc) et quel use (literal ou encoded) le binding déclare-t-il ? Est-ce du SOAP 1.1 ou 1.2 ?**
- **Style** : `document` (`<soap:binding style="document" .../>`)
- **Use** : `literal` (`<soap:body use="literal"/>`)
- **Version SOAP** : **SOAP 1.1**, car l'espace de noms utilisé pour le binding est `http://schemas.xmlsoap.org/wsdl/soap/` (SOAP 1.2 utiliserait `http://schemas.xmlsoap.org/wsdl/soap12/`).

**3. Quel type XSD a été généré pour `isbn` et pour `dateRetour` ? La règle de format de l'ISBN apparaît-elle ?**
- Dans le schéma XSD (`?xsd=1`), `isbn` et `dateRetour` ont tous les deux été générés avec le type générique **`xs:string`**.
- **Non**, la règle de format (l'expression régulière `Pattern` de l'ISBN) n'apparaît pas dans le XSD, car en approche *code-first*, la validation par `Pattern` est cachée à l'intérieur du code Java et n'est pas traduite dans le contrat XSD.

**4. Dans la requête SoapUI, les paramètres `idAdherent` et `isbn` portent-ils un préfixe d'espace de noms ?**
- **Non**, ils ne portent pas de préfixe (ils sont non qualifiés : `<idAdherent>` et `<isbn>`), car dans le schéma XSD généré par défaut par JAX-WS, l'attribut `elementFormDefault="qualified"` n'est pas défini (par défaut il vaut `unqualified`, donc seuls les éléments globaux comme `<bib:emprunterLivre>` portent un préfixe).

**5. Dans l'onglet Raw, où voyez-vous HTTP, XML, SOAP ? Quel rôle joue chacun de ces standards, et quel rôle joue le WSDL ?**
- **HTTP** : Ce sont les en-têtes de transport tout en haut (`POST /bib/EmpruntService HTTP/1.1`, `Content-Type: text/xml`, `SOAPAction`, etc.). **Rôle** : transporter les messages sur le réseau entre le client et le serveur.
- **XML** : C'est la syntaxe de structuration des données sous forme de balises (`<?xml ...?>`, `<idAdherent>...</idAdherent>`). **Rôle** : représenter les données de manière structurée et indépendante du langage de programmation.
- **SOAP** : Ce sont les balises de l'enveloppe (`<soapenv:Envelope>`, `<soapenv:Header>`, `<soapenv:Body>`). **Rôle** : encadrer le message applicatif et gérer les métadonnées ou les erreurs (`Fault`).
- **WSDL** : C'est le contrat de service lu au départ par le client. **Rôle** : décrire l'interface du service (URL, opérations, types de données, protocole) afin que le client sache comment construire ses requêtes.

---

### Étape 3 : Envoyer des messages SOAP

**1. Pourquoi la requête utilise-t-elle POST, même pour lire une information ?**
- Parce qu'en SOAP sur HTTP, le message SOAP est transporté dans le corps (`Body`) de la requête HTTP sous forme d'un document XML. Or, la méthode `GET` ne permet pas d'envoyer un corps XML complexe ; `POST` est donc obligatoire pour transmettre l'enveloppe SOAP au serveur.

**2. Le bloc `jeton` du Header a-t-il empêché le traitement ? Pourquoi ?**
- **Non**, il n'a pas empêché le traitement. Par défaut (lorsque l'attribut `mustUnderstand` est absent ou vaut `"0"`), un nœud SOAP qui ne comprend pas un en-tête (`Header`) l'ignore simplement et poursuit le traitement du `Body`.

**3. Que répond le service au message SOAP 1.2 ? Expliquez avec le binding lu à l'étape 2.**
- Le serveur répond avec une erreur HTTP **`415 Unsupported Media Type`** (ou une `VersionMismatch` Fault).
- **Explication** : Comme observé dans le `<wsdl:binding>` à l'étape 2, le service est configuré uniquement pour **SOAP 1.1** (`http://schemas.xmlsoap.org/wsdl/soap/`), qui attend le type MIME `text/xml` et l'espace de noms `http://schemas.xmlsoap.org/soap/envelope/`. Il rejette donc les requêtes SOAP 1.2 (`application/soap+xml`).

**4. Listez les cinq erreurs du dernier message et leur correction. Certaines ne déclenchent aucune erreur côté serveur : lesquelles, et pourquoi ?**
- **Les 5 erreurs et corrections** :
  1. `<soap:envelope>` (minuscule) -> **`<soap:Envelope>`** (XML est sensible à la casse).
  2. `xmlns:bib="http://supnum.mr/bibliotheque"` -> **`xmlns:bib="http://supnum.mr/bib"`** (mauvais `targetNamespace`).
  3. `<bib:numero>` -> **`<numeroEmprunt>`** (mauvais nom de paramètre ET préfixe `bib:` en trop car `unqualified`).
  4. `</rendreLivre>` -> **`</bib:rendreLivre>`** (la balise fermante doit avoir le même préfixe `bib:` que la balise ouvrante).
  5. `<soap:Header>` placé après `<soap:Body>` -> **Placer `<soap:Header>` avant `<soap:Body>`** (l'ordre imposé par la spécification SOAP est `Header` puis `Body`).
- **Celles qui ne déclenchent pas d'erreur serveur** : L'erreur n°3 sur le nom/préfixe du paramètre (`<bib:numero>` au lieu de `<numeroEmprunt>`) et l'ordre du `Header` ne bloquent pas l'exécution par défaut avec JAX-WS sans validation de schéma stricte (`@SchemaValidation`) : JAXB ignore les balises inconnues et passe `null` au paramètre `numeroEmprunt` en Java !

---

### Étape 4 : Provoquer et construire des Faults

**1. Pour les cas 1 et 2, quels `faultcode` et `faultstring` recevez-vous, et quel code HTTP ?**
- **Code HTTP** : **`500 Internal Server Error`** pour les deux cas (en SOAP 1.1, toute `Fault` est renvoyée avec le statut HTTP 500).
- **Cas 1 (ISBN invalide `978-2-10`)** :
  - `faultcode` : `S:Client` (généré explicitement par notre méthode `faultClient`).
  - `faultstring` : `ISBN mal formé : 978-2-10`
- **Cas 2 (Livre déjà emprunté `978-2-10-074444-4`)** :
  - `faultcode` : `S:Server` (code choisi par défaut par JAX-WS pour une exception applicative).
  - `faultstring` : `Livre indisponible`

**2. Dans le cas 2, que contient `<detail>` ? Où sa structure est-elle décrite dans le WSDL ?**
- **Contenu de `<detail>`** : Il contient l'élément `<ns2:LivreIndisponible xmlns:ns2="http://supnum.mr/bib">` avec deux sous-éléments : `<isbn>978-2-10-074444-4</isbn>` et `<retourPrevu>2026-12-02</retourPrevu>`.
- **Description dans le WSDL** : Sa structure est décrite dans la section `<wsdl:types>` (plus précisément dans le schéma XSD importé `?xsd=1`) sous le type complexe `livreIndisponibleInfo` lié à l'élément global `LivreIndisponible`, lui-même référencé par `<wsdl:message name="LivreIndisponibleException">` et `<wsdl:fault>` dans le `portType`.

**3. Quel code JAX-WS a-t-il choisi pour le livre indisponible ? Est-ce une erreur du client ou du serveur ? Qu'auriez-vous choisi ?**
- JAX-WS a choisi **`S:Server`** par défaut lorsqu'une exception Java (`@WebFault`) est levée.
- En réalité, il s'agit d'une **erreur métier liée à la demande du client** (le client demande un livre qui n'est pas disponible), et non d'une panne technique du serveur.
- **Choix personnel** : J'aurais préféré un code **`Client`** (ou `Sender` en SOAP 1.2), car le serveur a fonctionné normalement et c'est la requête du client qui ne peut pas être satisfaite en l'état.

**4. Que se passe-t-il avec `mustUnderstand="1"` ? Et avec `"0"` ?**
- **Avec `mustUnderstand="1"`** : Le serveur rejette immédiatement le message sans exécuter `emprunterLivre` et renvoie une Fault SOAP avec le code **`S:MustUnderstand`** (et le message indiquant que l'en-tête `{http://supnum.mr/bib}jeton` n'est pas compris).
- **Avec `mustUnderstand="0"`** : Le serveur ignore l'en-tête `jeton` qu'il ne connaît pas, exécute normalement l'opération `emprunterLivre` et renvoie la réponse `200 OK`.

**5. Quelle différence voyez-vous entre votre Fault 1.2 et les Faults 1.1 reçues ?**
- **Espace de noms** : `http://www.w3.org/2003/05/soap-envelope` en 1.2 contre `http://schemas.xmlsoap.org/soap/envelope/` en 1.1.
- **Noms des balises et structure** :
  - `<faultcode>` (1.1) devient `<soap:Code><soap:Value>...</soap:Value></soap:Code>` (1.2).
  - Les valeurs standard `Client` / `Server` (1.1) deviennent `soap:Sender` / `soap:Receiver` (1.2).
  - `<faultstring>` (1.1) devient `<soap:Reason><soap:Text xml:lang="fr">...</soap:Text></soap:Reason>` (1.2, avec attribut `xml:lang` obligatoire).
  - `<detail>` (1.1) devient `<soap:Detail>` avec une majuscule (1.2).

---

## Partie B — Contract-first

### Étape 5 — Générer le code depuis le WSDL

**1. Dans ce WSDL, retrouvez l'opération, son message de fault et l'action SOAP à envoyer.**
- **Opération** : `emprunterLivre` définie dans `<wsdl:portType name="EmpruntPortType">`.
- **Message de fault** : `LivreIndisponible` associé au message `bib:LivreIndisponibleFault`.
- **Action SOAP** :


`http://supnum.mr/bib/emprunterLivre` définie dans `<soap:operation soapAction="..."/>`.

**2. Quelle est la signature de `emprunterLivre` dans `EmpruntPortType` ?**
```java
public EmpruntType emprunterLivre(String idAdherent, String isbn) throws LivreIndisponibleFault;

3. Quel type Java a été généré pour dateRetour ? Comparez avec la partie A.

En Partie B (Contract-first), le type XSD xs:date est traduit par wsimport / JAXB en javax.xml.datatype.XMLGregorianCalendar.

En Partie A (Code-first), nous avions utilisé un type Java standard (String ou LocalDate), qui produisait simplement xs:string dans le WSDL généré.

4. Quel est le nom de la classe d'exception générée, et d'où vient ce nom dans le WSDL ?

Nom de la classe : LivreIndisponibleFault.java (dans le paquet mr.supnum.bib.contrat).

Origine : Ce nom provient directement de l'attribut name de la balise <wsdl:message name="LivreIndisponibleFault"> dans le fichier WSDL.


Étape 6 — Implémenter et comparer
1. Le WSDL publié sur ?wsdl est-il identique au WSDL fourni (contient-il la restriction IsbnType et xs:date) ?

Oui, grâce à l'attribut wsdlLocation = "/wsdl/EmpruntService.wsdl" dans l'annotation @WebService, JAX-WS publie exactement le WSDL fourni dans src/main/resources/wsdl/. Il contient donc bien la restriction IsbnType (xs:pattern) ainsi que le type xs:date.

2. Avez-vous dû modifier le fichier 01-emprunter.xml pour appeler le service Contract-first ? Pourquoi ?

Non, aucune modification n'a été nécessaire car le contrat WSDL de la partie B utilise exactement le même targetNamespace ([http://supnum.mr/bib](http://supnum.mr/bib)) et les mêmes noms d'opération et d'éléments (emprunterLivre, idAdherent, isbn) que ceux utilisés en partie A.

3. Que se passe-t-il avec l'ISBN 978-2-10 sans @SchemaValidation, puis avec @SchemaValidation ?

Sans @SchemaValidation : JAX-WS ne vérifie pas les expressions régulières XSD ; la requête atteint la méthode Java emprunterLivre, qui ne trouve pas le livre et lève l'exception métier LivreIndisponibleFault.

Avec @SchemaValidation : La requête est rejetée avant même d'atteindre le code Java. Le serveur retourne un SOAP Fault de code S:Client (ou Sender) indiquant une erreur de validation XSD (cvc-pattern-valid).

4. Que se passe-t-il si on supprime l'attribut wsdlLocation dans l'annotation @WebService ?

JAX-WS ignore le fichier EmpruntService.wsdl et régénère dynamiquement un WSDL à partir des classes Java (comme en approche Code-first). On perd alors les contraintes fines du contrat initial (notamment le motif xs:pattern de IsbnType, qui redevient un simple xs:string).

Conclusion — Choisir et rendre
1. Citez deux différences observées entre le WSDL généré de la partie A et le WSDL écrit de la partie B.

Structure du schéma XSD : En partie A, le schéma XSD est externe et importé via <xsd:import schemaLocation="..."/>, alors qu'en partie B il est intégré directement à l'intérieur de <wsdl:types>.

Précision des types et contraintes : En partie B, le contrat WSDL inclut des types fortement contraints (IsbnType avec xs:pattern, xs:date), tandis qu'en partie A les champs sont générés en xs:string sans validation de format au niveau du contrat.

2. Si un développeur renomme le champ dateRetour dans le code Java, quel projet est impacté et quel est le risque pour les clients ?

Projet impacté : Le projet de la Partie A (Code-first) voit son contrat WSDL modifié automatiquement au redémarrage (en partie B, le compilateur empêche ce renommage car la classe implémente l'interface générée depuis le WSDL figé).

Risque pour les clients : Rupture de compatibilité (breaking change) : les applications clientes existantes qui attendent la balise <dateRetour> ne pourront plus désérialiser la réponse SOAP.

3. Quelle approche choisiriez-vous pour chacun des deux contextes suivants ? Justifiez.

Service Paiement de l'université (appelé par Scolarité en .NET, Bibliothèque en PHP, Portail étudiant en Node.js) : Contract-first, car l'interopérabilité entre plusieurs équipes et technologies hétérogènes exige un contrat WSDL stable, précis, validé et partagé à l'avance avant le développement.

Prototype interne développé seul en deux jours pour tester une idée : Code-first, car il permet d'écrire directement le code métier en Java et de publier le service très rapidement sans perdre de temps à rédiger manuellement le WSDL et les schémas XSD.
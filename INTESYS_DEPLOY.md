# Deploy su Nexus Intesys e gestione features

Per poter effettuare il deploy sul Nexus di Intesys, è possibile seguire questo workflow.

Tutte le features devono essere create a partire dal main (che è possibile tenere aggiornato con l'origin tramite **Sync fork** nella relativa pagina GitHub). I
feature branch devono seguire la namin "feature/...".

Se si desidera fare il deploy sul Nexus di Intesys occorre seguire questi step:

1. Effettuare il merge del feature branch nel branch **intesys**;
2. Lanciare in locale il seguente comando per effettuare il deploy su Nexus:

```shell
mvn deploy -DskipTests
```

Si noti che per utilizzare le dipendenze in altri progetti, nel pom deve essere cambiato il groupId.
Per fare un esempio, anziché usare

```xml

<dependency>
    <groupId>com.github.paulcwarren</groupId>
    <artifactId>spring-content-commons</artifactId>
    <version>3.0.17-SNAPSHOT</version>
</dependency>
```

deve essere usato

```xml

<dependency>
    <groupId>it.intesys.sc</groupId>
    <artifactId>spring-content-commons</artifactId>
    <version>3.0.17-SNAPSHOT</version>
</dependency>
```

Ricapitolando, ipotizzando di dover implementare una nuova feature in spring-content, gli step da seguire sono:

1. Aggiornamento branch main (Sync fork);
2. Creazione feature branch con nome "feature/..." a partire dal main;
3. Al termine delle modifiche, merge del branch feature nel branch "intesys";
4. Deploy manuale tramite l'apposito comando maven;
5. Utilizo in progetti esterni della dipendenza impostando "it.intesys.sc" come groupId.